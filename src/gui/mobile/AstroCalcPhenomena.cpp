/*
 * Asterium - a touch-first planetarium, forked from Stellarium.
 * Copyright (C) 2026 the Asterium authors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA  02110-1335, USA.
 */

#include "AstroCalcPages.hpp"
#include "AstroCalcPages_p.hpp"
#include "AndroidUi.hpp"

#include "StelApp.hpp"
#include "StelCore.hpp"
#include "StelModuleMgr.hpp"
#include "StelMovementMgr.hpp"
#include "StelObjectMgr.hpp"
#include "StelTranslator.hpp"
#include "StelUtils.hpp"

#include "Nebula.hpp"
#include "NebulaMgr.hpp"
#include "Planet.hpp"
#include "SolarSystem.hpp"
#include "StarMgr.hpp"

#include <QJsonArray>
#include <QMap>
#include <QRegularExpression>
#include <QStringList>
#include <QVector>

#include <algorithm>
#include <cmath>

namespace AsteriumAstroCalc {
namespace Phenomena {
namespace
{
const double kBrightLimit = 10.;

enum Mode
{
	Conjunction        = 0,
	Opposition         = 1,
	GreatestElongation = 2,
	StationaryPoint    = 3,
	OrbitalPoint       = 4,
	Shadows            = 5,
	Quadrature         = 6
};

enum Category
{
	PHCLatestSelectedObject     = -1,
	PHCSolarSystem              =  0,
	PHCPlanets                  =  1,
	PHCAsteroids                =  2,
	PHCPlutinos                 =  3,
	PHCComets                   =  4,
	PHCDwarfPlanets             =  5,
	PHCCubewanos                =  6,
	PHCScatteredDiscObjects     =  7,
	PHCOortCloudObjects         =  8,
	PHCSednoids                 =  9,
	PHCBrightStars              = 10,
	PHCBrightDoubleStars        = 11,
	PHCBrightVariableStars      = 12,
	PHCBrightStarClusters       = 13,
	PHCPlanetaryNebulae         = 14,
	PHCBrightNebulae            = 15,
	PHCDarkNebulae              = 16,
	PHCBrightGalaxies           = 17,
	PHCSymbioticStars           = 18,
	PHCEmissionLineStars        = 19,
	PHCInterstellarObjects      = 20,
	PHCPlanetsSun               = 21,
	PHCSunPlanetsMoons          = 22,
	PHCBrightSolarSystemObjects = 23,
	PHCSolarSystemMinorBodies   = 24,
	PHCMoonsFirstBody           = 25,
	PHCBrightCarbonStars        = 26,
	PHCBrightBariumStars        = 27,
	PHCSunPlanetsTheirMoons     = 28
};

QJsonArray kRows;

double findInitialStep(double startJD, double stopJD, const QStringList& objects)
{
	static const QRegularExpression mp("^[(](\\d+)[)]\\s(.+)$");
	static const QMap<QString, double> steps = {
		{ "Moon",     0.25 },
		{ "C/",       0.5  },
		{ "P/",       0.5  },
		{ "Earth",    1.0  },
		{ "Sun",      1.0  },
		{ "Venus",    2.5  },
		{ "Mercury",  2.5  },
		{ "Mars",     5.0  },
		{ "Jupiter", 15.0  },
		{ "Saturn",  15.0  },
		{ "Neptune", 20.0  },
		{ "Uranus",  20.0  },
		{ "Pluto",   20.0  },
	};

	double limit = 10.;
	if (objects.contains(mp))
	{
		limit = 24.8 * 365.25;
		QMap<QString, double>::const_iterator it = steps.constBegin();
		while (it != steps.constEnd())
		{
			if (objects.contains(it.key(), Qt::CaseInsensitive))
				limit = qMin(it.value(), limit);
			++it;
		}
	}

	double step = (stopJD - startJD) / 16.0;
	if (step > limit)
		step = limit;

	return step;
}

double findDistance(StelCore* core, double JD, PlanetP object1, StelObjectP object2, int mode)
{
	core->setJD(JD);
	core->update(0);
	double angle = object1->getJ2000EquatorialPos(core).angle(object2->getJ2000EquatorialPos(core));
	if (mode == Opposition)
		angle = M_PI - angle;
	else if (mode == Shadows)
		angle = object1->getHeliocentricEclipticPos().angle(
				qSharedPointerCast<Planet>(object2)->getHeliocentricEclipticPos());
	return angle;
}

bool isSecondObjectRight(StelCore* core, double JD, PlanetP object1, StelObjectP object2)
{
	core->setJD(JD);
	core->update(0);
	const double angle1 = object1->getJ2000EquatorialPos(core).longitude() * M_180_PI;
	const double angle2 = object2->getJ2000EquatorialPos(core).longitude() * M_180_PI;
	return (StelUtils::fmodpos(angle2 - angle1, 360.) < 180.);
}

bool findPrecise(StelCore* core, QPair<double, double>* out, PlanetP object1, StelObjectP object2,
                 double JD, double step, int prevSign, int mode)
{
	if (out == nullptr)
		return false;

	double prevDist = findDistance(core, JD, object1, object2, mode);
	step = -step / 2.;
	prevSign = -prevSign;

	while (true)
	{
		JD += step;
		double dist = findDistance(core, JD, object1, object2, mode);

		if (qAbs(step) < 1. / 1440.)
		{
			out->first = JD - step / 2.0;
			out->second = findDistance(core, JD - step / 2.0, object1, object2, mode);
			return out->second < findDistance(core, JD - 5.0, object1, object2, mode);
		}
		int sgn = StelUtils::sign(dist - prevDist);
		if (sgn != prevSign)
		{
			step = -step / 2.0;
			sgn = -sgn;
		}
		prevDist = dist;
		prevSign = sgn;
	}
}

QMap<double, double> findClosestApproach(StelCore* core, PlanetP& object1, StelObjectP& object2,
                                         const double startJD, const double stopJD,
                                         const double maxSeparation, const int mode)
{
	QMap<double, double> separations;
	QPair<double, double> extremum;

	if (object2->getType() == "Planet")
	{
		PlanetP planet = qSharedPointerCast<Planet>(object2);

		if (mode == Shadows && object2->getEnglishName() != "Sun" && planet->getParent() != object1)
			return separations;

		const Vec2d planetValidityLimits = planet->getValidPositionalDataRange(Planet::PositionQuality::Position);
		if ((planetValidityLimits[0] > stopJD) || (planetValidityLimits[1] < startJD))
			return separations;
	}

	QStringList objects;
	objects.append(object1->getEnglishName());
	objects.append(object2->getEnglishName());
	double step0 = findInitialStep(startJD, stopJD, objects);
	double step = step0;
	double jd = startJD;
	double prevDist = findDistance(core, jd, object1, object2, mode);
	int prevSgn = 0;
	jd += step;
	while (jd <= stopJD)
	{
		double dist = findDistance(core, jd, object1, object2, mode);
		int sgn = StelUtils::sign(dist - prevDist);

		double factor = qAbs((dist - prevDist) / dist);
		if (factor > 10.)
			step = step0 * factor / 10.;
		else
			step = step0;

		if (sgn != prevSgn && prevSgn == -1)
		{
			if (step > step0)
			{
				jd -= step;
				step = step0;
				sgn = prevSgn;
				while (jd <= stopJD)
				{
					dist = findDistance(core, jd, object1, object2, mode);
					sgn = StelUtils::sign(dist - prevDist);
					if (sgn != prevSgn)
						break;

					prevDist = dist;
					prevSgn = sgn;
					jd += step;
				}
			}

			if (findPrecise(core, &extremum, object1, object2, jd, step, sgn, mode))
			{
				double sep = extremum.second * 180. / M_PI;
				if (sep < maxSeparation)
					separations.insert(extremum.first, extremum.second);
			}
		}

		prevDist = dist;
		prevSgn = sgn;
		jd += step;
	}
	return separations;
}

bool findPreciseGreatestElongation(StelCore* core, QPair<double, double>* out, PlanetP object1,
                                   StelObjectP object2, double JD, double stopJD, double step)
{
	if (out == nullptr)
		return false;

	double prevDist = findDistance(core, JD, object1, object2, Conjunction);
	step = -step / 2.;

	while (true)
	{
		JD += step;
		double dist = findDistance(core, JD, object1, object2, Conjunction);

		if (qAbs(step) < 1. / 1440.)
		{
			out->first = JD - step / 2.0;
			out->second = findDistance(core, JD - step / 2.0, object1, object2, Conjunction);
			if (out->second > findDistance(core, JD - 5.0, object1, object2, Conjunction))
			{
				if (!isSecondObjectRight(core, out->first, object1, object2))
					out->second *= -1.0;
				return true;
			}
			return false;
		}
		if (dist < prevDist)
			step = -step / 2.0;
		prevDist = dist;

		if (JD > stopJD)
			return false;
	}
}

QMap<double, double> findGreatestElongationApproach(StelCore* core, PlanetP& object1, StelObjectP& object2,
                                                    double startJD, double stopJD)
{
	QMap<double, double> separations;
	QPair<double, double> extremum;

	QStringList objects;
	objects.append(object1->getEnglishName());
	objects.append(object2->getEnglishName());
	double step0 = findInitialStep(startJD, stopJD, objects);
	double step = step0;
	double jd = startJD;
	double prevDist = findDistance(core, jd, object1, object2, Conjunction);
	jd += step;
	while (jd <= stopJD)
	{
		double dist = findDistance(core, jd, object1, object2, Conjunction);
		double factor = qAbs((dist - prevDist) / dist);
		if (factor > 10.)
			step = step0 * factor / 10.;
		else
			step = step0;

		if (dist > prevDist)
		{
			if (step > step0)
			{
				jd -= step;
				step = step0;
				while (jd <= stopJD)
				{
					dist = findDistance(core, jd, object1, object2, Conjunction);
					if (dist < prevDist)
						break;

					prevDist = dist;
					jd += step;
				}
			}

			double steps = step;
			if (findPreciseGreatestElongation(core, &extremum, object1, object2, jd, stopJD, step))
			{
				separations.insert(extremum.first, extremum.second);
				jd += 2. * steps;
			}
		}

		prevDist = dist;
		jd += step;
	}
	return separations;
}

bool findPreciseQuadrature(StelCore* core, QPair<double, double>* out, PlanetP object1,
                           StelObjectP object2, double JD, double stopJD, double step)
{
	if (out == nullptr)
		return false;

	Q_UNUSED(stopJD)
	step = -step / 288.;

	JD += step;
	const double dist = findDistance(core, JD, object1, object2, Conjunction);
	const double distNext = findDistance(core, JD + 1., object1, object2, Conjunction);
	if (((dist - M_PI_2) < 0. && (distNext - M_PI_2) > 0.) || ((dist - M_PI_2) > 0. && (distNext - M_PI_2) < 0.))
	{
		const double quadratureJD = JD + (qAbs(dist - M_PI_2) / qAbs(dist - distNext));
		out->first = quadratureJD;
		out->second = findDistance(core, quadratureJD, object1, object2, Conjunction);
		if (!isSecondObjectRight(core, quadratureJD, object1, object2))
			out->second *= -1.0;
		return true;
	}
	return false;
}

QMap<double, double> findQuadratureApproach(StelCore* core, PlanetP& object1, StelObjectP& object2,
                                            double startJD, double stopJD)
{
	QMap<double, double> separations;
	QPair<double, double> extremum;

	QStringList objects;
	objects.append(object1->getEnglishName());
	objects.append(object2->getEnglishName());
	double step0 = findInitialStep(startJD, stopJD, objects);
	double step = step0;
	double jd = startJD;
	double prevDist = findDistance(core, jd, object1, object2, Conjunction);
	const double limit = M_PI_2 + 0.02;
	jd += step;
	while (jd <= stopJD)
	{
		double dist = findDistance(core, jd, object1, object2, Conjunction);
		double factor = qAbs((dist - prevDist) / dist);
		if (factor > 10.)
			step = step0 * factor / 10.;
		else
			step = step0;

		if (dist < limit)
		{
			if (step > step0)
			{
				jd -= step;
				step = step0;
				while (jd <= stopJD)
				{
					dist = findDistance(core, jd, object1, object2, Conjunction);
					if (dist > limit)
						break;

					jd += step;
				}
			}

			double steps = step;
			if (findPreciseQuadrature(core, &extremum, object1, object2, jd, stopJD, step))
			{
				separations.insert(extremum.first, extremum.second);
				jd += 2.0 * steps;
			}
		}

		prevDist = dist;
		jd += step;
	}
	return separations;
}

double findRightAscension(StelCore* core, double JD, PlanetP object)
{
	const double JDE = JD + core->computeDeltaT(JD) / 86400.;
	const Vec3d obs = core->getCurrentPlanet()->getHeliocentricEclipticPos(JDE);
	Vec3d body = object->getHeliocentricEclipticPos(JDE);
	const double distanceCorrection = (body - obs).norm() * (AU / (SPEED_OF_LIGHT * 86400.));
	body = object->getHeliocentricEclipticPos(JDE - distanceCorrection);
	Vec3d bodyJ2000 = StelCore::matVsop87ToJ2000.multiplyWithoutTranslation(body - obs);
	double ra = 0., dec = 0.;
	StelUtils::rectToSphe(&ra, &dec, bodyJ2000);
	return ra * M_180_PI;
}

double findHeliocentricDistance(StelCore* core, double JD, PlanetP object1)
{
	return object1->getHeliocentricEclipticPos(JD + core->computeDeltaT(JD) / 86400.).norm();
}

bool findPreciseStationaryPoint(StelCore* core, QPair<double, double>* out, PlanetP object,
                                double JD, double stopJD, double step, bool retrograde)
{
	if (out == nullptr)
		return false;

	double prevRA = findRightAscension(core, JD, object);
	step /= -2.;

	while (true)
	{
		JD += step;
		double RA = findRightAscension(core, JD, object);

		if (qAbs(step) < 1. / 1440.)
		{
			out->first = JD - step / 2.0;
			out->second = findRightAscension(core, JD - step / 2.0, object);
			if (retrograde)
			{
				if (out->second > findRightAscension(core, JD - 5.0, object))
				{
					out->second = -1.0;
					return true;
				}
				return false;
			}
			if (out->second < findRightAscension(core, JD - 5.0, object))
			{
				out->second = 1.0;
				return true;
			}
			return false;
		}
		if (retrograde)
		{
			if (RA < prevRA)
				step /= -2.0;
		}
		else
		{
			if (RA > prevRA)
				step /= -2.0;
		}
		prevRA = RA;

		if (JD > stopJD)
			return false;
	}
}

QMap<double, double> findStationaryPointApproach(StelCore* core, PlanetP& object1, double startJD, double stopJD)
{
	double RA = 0., prevRA = 0., step = 0., step0 = 0.;
	QMap<double, double> separations;
	QPair<double, double> extremum;

	QStringList objects;
	objects.append(object1->getEnglishName());
	step0 = findInitialStep(startJD, stopJD, objects);
	step = step0;
	double jd = startJD;
	prevRA = findRightAscension(core, jd, object1);
	jd += step;
	while (jd <= stopJD)
	{
		RA = findRightAscension(core, jd, object1);
		double factor = qAbs((RA - prevRA) / RA);
		if (factor > 10.)
			step = step0 * factor / 10.;
		else
			step = step0;

		if (RA > prevRA && qAbs(RA - prevRA) < 180.)
		{
			if (step > step0)
			{
				jd -= step;
				step = step0;
				while (jd <= stopJD)
				{
					RA = findRightAscension(core, jd, object1);
					if (RA < prevRA)
						break;

					prevRA = RA;
					jd += step;
				}
			}

			if (findPreciseStationaryPoint(core, &extremum, object1, jd, stopJD, step, true))
				separations.insert(extremum.first, extremum.second);
		}
		prevRA = RA;
		jd += step;
	}

	step0 = findInitialStep(startJD, stopJD, objects);
	step = step0;
	jd = startJD;
	prevRA = findRightAscension(core, jd, object1);
	jd += step;
	while (jd <= stopJD)
	{
		RA = findRightAscension(core, jd, object1);
		double factor = qAbs((RA - prevRA) / RA);
		if (factor > 10.)
			step = step0 * factor / 10.;
		else
			step = step0;

		if (RA < prevRA && qAbs(RA - prevRA) < 180.)
		{
			if (step > step0)
			{
				jd -= step;
				step = step0;
				while (jd <= stopJD)
				{
					RA = findRightAscension(core, jd, object1);
					if (RA > prevRA)
						break;

					prevRA = RA;
					jd += step;
				}
			}

			if (findPreciseStationaryPoint(core, &extremum, object1, jd, stopJD, step, false))
				separations.insert(extremum.first, extremum.second);
		}
		prevRA = RA;
		jd += step;
	}

	return separations;
}

bool findPreciseOrbitalPoint(StelCore* core, QPair<double, double>* out, PlanetP object1,
                             double JD, double stopJD, double step, bool minimal)
{
	if (out == nullptr)
		return false;

	const double timeDist = qAbs(stopJD - JD);
	double prevDist = findHeliocentricDistance(core, JD, object1);
	step /= -2.;

	bool result = false;
	while (true)
	{
		JD += step;
		double dist = findHeliocentricDistance(core, JD, object1);

		if (qAbs(step) < 1. / 1440.)
		{
			out->first = JD - step / 2.0;
			out->second = findHeliocentricDistance(core, JD - step / 2.0, object1);
			if (minimal)
			{
				result = (out->second > findHeliocentricDistance(core, JD - step / 5.0, object1));
				if (result)
					out->second *= -1;
			}
			else
				result = (out->second < findHeliocentricDistance(core, JD - step / 5.0, object1));

			return result;
		}
		if (minimal)
		{
			if (dist > prevDist)
				step /= -2.0;
		}
		else
		{
			if (dist < prevDist)
				step /= -2.0;
		}
		prevDist = dist;

		if (JD > stopJD || JD < (stopJD - 2 * timeDist))
			return false;
	}
}

QMap<double, double> findOrbitalPointApproach(StelCore* core, PlanetP& object1, double startJD, double stopJD)
{
	QMap<double, double> separations;
	QPair<double, double> extremum;

	QStringList objects;
	objects.append(object1->getEnglishName());
	double step0 = findInitialStep(startJD, stopJD, objects);
	double step = step0;
	double jd = startJD - step;

	double prevDistance = findRightAscension(core, jd, object1);
	jd += step;
	const double stopJDfx = stopJD + step;
	while (jd <= stopJDfx)
	{
		double distance = findHeliocentricDistance(core, jd, object1);
		double factor = qAbs((distance - prevDistance) / distance);
		if (factor > 10.)
			step = step0 * factor / 10.;
		else
			step = step0;

		if (distance > prevDistance)
		{
			if (step > step0)
			{
				jd -= step;
				step = step0;
				while (jd <= stopJDfx)
				{
					distance = findHeliocentricDistance(core, jd, object1);
					if (distance < prevDistance)
						break;

					prevDistance = distance;
					jd += step;
				}
			}

			if (findPreciseOrbitalPoint(core, &extremum, object1, jd, stopJDfx, step, false))
			{
				if (extremum.first > startJD && extremum.first < stopJD)
					separations.insert(extremum.first, extremum.second);
			}
		}

		prevDistance = distance;
		jd += step;
	}

	step0 = findInitialStep(startJD, stopJD, objects);
	step = step0;
	jd = startJD - step;
	prevDistance = findRightAscension(core, jd, object1);
	jd += step;
	while (jd <= stopJDfx)
	{
		double distance = findHeliocentricDistance(core, jd, object1);
		double factor = qAbs((distance - prevDistance) / distance);
		if (factor > 10.)
			step = step0 * factor / 10.;
		else
			step = step0;

		if (distance < prevDistance)
		{
			if (step > step0)
			{
				jd -= step;
				step = step0;
				while (jd <= stopJDfx)
				{
					distance = findHeliocentricDistance(core, jd, object1);
					if (distance > prevDistance)
						break;

					prevDistance = distance;
					jd += step;
				}
			}

			if (findPreciseOrbitalPoint(core, &extremum, object1, jd, stopJDfx, step, true))
			{
				if (extremum.first > startJD && extremum.first < stopJD)
					separations.insert(extremum.first, extremum.second);
			}
		}

		prevDistance = distance;
		jd += step;
	}

	return separations;
}

QString magnitudeString(float magnitude)
{
	return magnitude > 90.f ? kDash : QString::number(static_cast<double>(magnitude), 'f', 2);
}

void appendRow(QJsonArray& rows, const QString& phenomenType, int mode, double JD,
               const QString& firstObjectName, float firstObjectMagnitude,
               const QString& secondObjectName, float secondObjectMagnitude,
               const QString& separation, const QString& elevation, QString elongation,
               const QString& angularDistance, const QString& select,
               const QString& elongTooltip, const QString& angDistTooltip)
{
	QJsonObject row;
	row["type"] = phenomenType;
	row["mode"] = mode;
	row["jd"] = JD;
	row["when"] = asteriumFormatSimTime(JD, "yyyy-MM-dd HH:mm:ss");
	row["obj1"] = firstObjectName;
	row["mag1"] = magnitudeString(firstObjectMagnitude);
	row["obj2"] = secondObjectName;
	row["mag2"] = magnitudeString(secondObjectMagnitude);
	row["sep"] = separation;
	row["elev"] = elevation;
	row["elong"] = elongation.remove('+');
	row["elongLabel"] = elongTooltip.isEmpty() ? ct_("Angular distance from the Sun") : elongTooltip;
	row["angDist"] = angularDistance;
	row["angDistLabel"] = angDistTooltip.isEmpty() ? ct_("Angular distance from the Moon") : angDistTooltip;
	row["select"] = select;
	rows.append(row);
}

QString elevationString(StelCore* core, const PlanetP& object1, bool withDecimalDegree)
{
	double az = 0., alt = 0.;
	StelUtils::rectToSphe(&az, &alt, object1->getAltAzPosAuto(core));
	return withDecimalDegree ? StelUtils::radToDecDegStr(alt, 5) : StelUtils::radToDmsPStr(alt, 2);
}

QString angle(double radians, bool withDecimalDegree)
{
	return withDecimalDegree ? StelUtils::radToDecDegStr(radians, 5, false, true)
	                         : StelUtils::radToDmsStr(radians, true);
}

void fillPhenomenaTable(StelCore* core, QJsonArray& rows, const QMap<double, double>& list,
                        const PlanetP object1, const PlanetP object2, int mode)
{
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	PlanetP sun = solarSystem->getSun();
	PlanetP moon = solarSystem->getMoon();
	PlanetP earth = solarSystem->getEarth();
	PlanetP planet = core->getCurrentPlanet();
	const bool withDecimalDegree = StelApp::getInstance().getFlagShowDecimalDegrees();
	for (QMap<double, double>::ConstIterator it = list.constBegin(); it != list.constEnd(); ++it)
	{
		core->setJD(it.key());
		core->update(0);

		QString phenomenType = ct_("Conjunction");
		double separation = it.value();
		bool occultation = false;
		const double s1 = object1->getSpheroidAngularRadius(core);
		const double s2 = object2->getSpheroidAngularRadius(core);
		const double d1 = object1->getJ2000EquatorialPos(core).norm();
		const double d2 = object2->getJ2000EquatorialPos(core).norm();
		if (mode == Shadows)
		{
			phenomenType = ct_("Shadow transit");
			separation = object1->getJ2000EquatorialPos(core).angle(object2->getJ2000EquatorialPos(core));
			if (d1 < d2)
				phenomenType = ct_("Eclipse");
		}
		else if (mode == Opposition)
		{
			phenomenType = ct_("Opposition");

			if (qAbs(separation) <= 0.02 && ((object1 == moon && object2 == sun) || (object1 == sun && object2 == moon)))
				phenomenType = ct_("Eclipse");

			separation = M_PI - separation;
		}
		else if (mode == GreatestElongation)
		{
			if (separation < 0.0)
			{
				separation *= -1.0;
				phenomenType = ct_("Greatest eastern elongation");
			}
			else
				phenomenType = ct_("Greatest western elongation");
		}
		else if (mode == Quadrature)
		{
			if (separation < 0.0)
			{
				separation *= -1.0;
				phenomenType = ct_("Eastern quadrature");
			}
			else
				phenomenType = ct_("Western quadrature");
		}
		else if (mode == StationaryPoint)
		{
			if (separation < 0.0)
			{
				phenomenType = ct_("Stationary (begin retrograde motion)");
			}
			else
			{
				phenomenType = ct_("Stationary (begin prograde motion)");
			}
		}
		else if (mode == OrbitalPoint)
		{
			if (separation < 0.0)
				phenomenType = ct_("Perihelion");
			else
				phenomenType = ct_("Aphelion");
		}
		else if (separation < (s2 * M_PI / 180.) || separation < (s1 * M_PI / 180.))
		{
			if ((d1 < d2 && s1 <= s2) || (d1 > d2 && s1 > s2))
			{
				phenomenType = qc_("Transit", "passage of a celestial body in front of another");
			}
			else
				phenomenType = ct_("Occultation");

			if (qAbs(s1 - s2) <= 0.05 && (object1 == sun || object2 == sun))
				phenomenType = ct_("Eclipse");

			occultation = true;
		}
		else if (qAbs(separation) <= 0.0087 && ((object1 == moon && object2 == sun) || (object1 == sun && object2 == moon)))
		{
			phenomenType = ct_("Eclipse");
		}
		else if (object1 == sun || object2 == sun)
		{
			double dcp = planet->getHeliocentricEclipticPos().norm();
			double dp = (object1 == sun) ? object2->getHeliocentricEclipticPos().norm()
			                             : object1->getHeliocentricEclipticPos().norm();
			if (dp < dcp)
			{
				if (object1 == sun)
					phenomenType = d1 < d2 ? ct_("Superior conjunction") : ct_("Inferior conjunction");
				else
					phenomenType = d2 < d1 ? ct_("Superior conjunction") : ct_("Inferior conjunction");
			}
		}

		QString elongStr;
		if (((object1 == sun || object2 == sun) && mode == Conjunction) || (object2 == sun && mode == Opposition))
			elongStr = kDash;
		else
		{
			double elongation = object1->getElongation(core->getObserverHeliocentricEclipticPos());
			if (mode == Opposition)
				elongation = object2->getElongation(core->getObserverHeliocentricEclipticPos());
			elongStr = angle(elongation, withDecimalDegree);
		}

		QString angDistStr;
		if (planet != earth || object1 == moon || object2 == moon)
			angDistStr = kDash;
		else
		{
			double angularDistance = object1->getJ2000EquatorialPos(core).angle(moon->getJ2000EquatorialPos(core));
			if (mode == Opposition)
				angularDistance = object2->getJ2000EquatorialPos(core).angle(moon->getJ2000EquatorialPos(core));
			angDistStr = angle(angularDistance, withDecimalDegree);
		}

		QString elongationInfo = ct_("Angular distance from the Sun");
		QString angularDistanceInfo = ct_("Angular distance from the Moon");
		if (mode == Opposition)
		{
			elongationInfo = ct_("Angular distance from the Sun for second object");
			angularDistanceInfo = ct_("Angular distance from the Moon for second object");
		}

		QString separationStr = kDash;
		float magnitude = object2->getVMagnitude(core);
		if (!occultation)
			separationStr = angle(separation, withDecimalDegree);
		else
			magnitude = 99.f;

		QString nameObj2 = object2->getNameI18n();
		if (mode == StationaryPoint)
		{
			nameObj2 = kDash;
			magnitude = 99.f;
			separationStr = kDash;
		}

		const QString select = (mode == Opposition) ? object2->getEnglishName() : object1->getEnglishName();

		appendRow(rows, phenomenType, mode, it.key(), object1->getNameI18n(),
		          object1->getVMagnitude(core), nameObj2, magnitude, separationStr,
		          elevationString(core, object1, withDecimalDegree), elongStr, angDistStr,
		          select, elongationInfo, angularDistanceInfo);
	}
}

void fillPhenomenaTable(StelCore* core, QJsonArray& rows, const QMap<double, double>& list,
                        const PlanetP object1, const NebulaP object2)
{
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	PlanetP sun = solarSystem->getSun();
	PlanetP moon = solarSystem->getMoon();
	PlanetP earth = solarSystem->getEarth();
	PlanetP planet = core->getCurrentPlanet();
	const bool withDecimalDegree = StelApp::getInstance().getFlagShowDecimalDegrees();
	for (QMap<double, double>::ConstIterator it = list.constBegin(); it != list.constEnd(); ++it)
	{
		core->setJD(it.key());
		core->update(0);

		QString phenomenType = ct_("Conjunction");
		double separation = it.value();
		bool occultation = false;
		if (separation < (object2->getAngularRadius(core) * M_PI / 180.)
		    || separation < (object1->getSpheroidAngularRadius(core) * M_PI / 180.))
		{
			phenomenType = ct_("Occultation");
			occultation = true;
		}

		QString elongStr = kDash;
		if (object1 != sun)
			elongStr = angle(object1->getElongation(core->getObserverHeliocentricEclipticPos()), withDecimalDegree);

		QString angDistStr = kDash;
		if (planet == earth && object1 != moon)
		{
			const double angularDistance =
					object1->getJ2000EquatorialPos(core).angle(moon->getJ2000EquatorialPos(core));
			angDistStr = angle(angularDistance, withDecimalDegree);
		}

		QString commonName = object2->getNameI18n();
		if (commonName.isEmpty())
			commonName = object2->getDSODesignation();
		if (commonName.isEmpty())
			commonName = object2->getDSODesignationWIC();

		QString separationStr = kDash;
		float magnitude = object2->getVMagnitude(core);
		if (!occultation)
			separationStr = angle(separation, withDecimalDegree);
		else
			magnitude = 99.f;

		appendRow(rows, phenomenType, Conjunction, it.key(), object1->getNameI18n(),
		          object1->getVMagnitude(core), commonName, magnitude, separationStr,
		          elevationString(core, object1, withDecimalDegree), elongStr, angDistStr,
		          object1->getEnglishName(), QString(), QString());
	}
}

void fillPhenomenaTable(StelCore* core, QJsonArray& rows, const QMap<double, double>& list,
                        const PlanetP object1, const StelObjectP object2, int mode)
{
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	PlanetP sun = solarSystem->getSun();
	PlanetP moon = solarSystem->getMoon();
	PlanetP earth = solarSystem->getEarth();
	PlanetP planet = core->getCurrentPlanet();
	const bool withDecimalDegree = StelApp::getInstance().getFlagShowDecimalDegrees();
	for (QMap<double, double>::ConstIterator it = list.constBegin(); it != list.constEnd(); ++it)
	{
		core->setJD(it.key());
		core->update(0);

		QString phenomenType = ct_("Conjunction");
		double separation = it.value();
		bool occultation = false;
		const double s1 = object1->getSpheroidAngularRadius(core);
		const double s2 = object2->getAngularRadius(core);
		const double d1 = object1->getJ2000EquatorialPos(core).norm();
		const double d2 = object2->getJ2000EquatorialPos(core).norm();
		if (mode == Opposition)
		{
			phenomenType = ct_("Opposition");

			if (qAbs(separation) <= 0.02 && ((object1 == moon && object2 == sun) || (object1 == sun && object2 == moon)))
				phenomenType = ct_("Eclipse");

			separation = M_PI - separation;
		}
		else if (separation < (s2 * M_PI / 180.) || separation < (s1 * M_PI / 180.))
		{
			if ((d1 < d2 && s1 <= s2) || (d1 > d2 && s1 > s2))
			{
				phenomenType = qc_("Transit", "passage of a celestial body in front of another");
			}
			else
				phenomenType = ct_("Occultation");

			if (qAbs(s1 - s2) <= 0.05 && (object1 == sun || object2 == sun))
				phenomenType = ct_("Eclipse");

			occultation = true;
		}
		else if (qAbs(separation) <= 0.0087 && ((object1 == moon && object2 == sun) || (object1 == sun && object2 == moon)))
		{
			phenomenType = ct_("Eclipse");
		}
		else if (object1 == sun || object2 == sun)
		{
			double dcp = (planet->getEquinoxEquatorialPos(core) - sun->getEquinoxEquatorialPos(core)).norm();
			double dp;
			if (object1 == sun)
				dp = (object2->getEquinoxEquatorialPos(core) - sun->getEquinoxEquatorialPos(core)).norm();
			else
				dp = (object1->getEquinoxEquatorialPos(core) - sun->getEquinoxEquatorialPos(core)).norm();
			if (dp < dcp)
			{
				if (object1 == sun)
					phenomenType = d1 < d2 ? ct_("Superior conjunction") : ct_("Inferior conjunction");
				else
					phenomenType = d2 < d1 ? ct_("Superior conjunction") : ct_("Inferior conjunction");
			}
		}

		QString elongStr = kDash;
		if (object1 != sun)
			elongStr = angle(object1->getElongation(core->getObserverHeliocentricEclipticPos()), withDecimalDegree);

		QString angDistStr = kDash;
		if (planet == earth && object1 != moon)
		{
			const double angularDistance =
					object1->getJ2000EquatorialPos(core).angle(moon->getJ2000EquatorialPos(core));
			angDistStr = angle(angularDistance, withDecimalDegree);
		}

		QString commonName = object2->getNameI18n();
		if (commonName.isEmpty())
			commonName = object2->getID();

		QString separationStr = kDash;
		float magnitude = object2->getVMagnitude(core);
		if (!occultation)
			separationStr = angle(separation, withDecimalDegree);
		else
			magnitude = 99.f;

		const QString select = (mode == Opposition) ? object2->getEnglishName() : object1->getEnglishName();

		appendRow(rows, phenomenType, mode, it.key(), object1->getNameI18n(),
		          object1->getVMagnitude(core), commonName, magnitude, separationStr,
		          elevationString(core, object1, withDecimalDegree), elongStr, angDistStr,
		          select, QString(), QString());
	}
}

QJsonArray groupList()
{
	const QString brLimit = QString::number(kBrightLimit, 'f', 1);
	const QMap<QString, int> itemsMap = {
		{ ct_("Latest selected object"), PHCLatestSelectedObject }, { ct_("Solar system"), PHCSolarSystem },
		{ ct_("Planets"), PHCPlanets }, { ct_("Asteroids"), PHCAsteroids }, { ct_("Plutinos"), PHCPlutinos },
		{ ct_("Comets"), PHCComets }, { ct_("Dwarf planets"), PHCDwarfPlanets },
		{ ct_("Cubewanos"), PHCCubewanos }, { ct_("Scattered disc objects"), PHCScatteredDiscObjects },
		{ ct_("Oort cloud objects"), PHCOortCloudObjects }, { ct_("Sednoids"), PHCSednoids },
		{ ct_("Bright stars (<%1 mag)").arg(QString::number(kBrightLimit - 5.0, 'f', 1)), PHCBrightStars },
		{ ct_("Bright double stars (<%1 mag)").arg(QString::number(kBrightLimit - 5.0, 'f', 1)), PHCBrightDoubleStars },
		{ ct_("Bright variable stars (<%1 mag)").arg(QString::number(kBrightLimit - 5.0, 'f', 1)), PHCBrightVariableStars },
		{ ct_("Bright star clusters (<%1 mag)").arg(brLimit), PHCBrightStarClusters },
		{ ct_("Planetary nebulae (<%1 mag)").arg(brLimit), PHCPlanetaryNebulae },
		{ ct_("Bright nebulae (<%1 mag)").arg(brLimit), PHCBrightNebulae },
		{ ct_("Dark nebulae"), PHCDarkNebulae },
		{ ct_("Bright galaxies (<%1 mag)").arg(brLimit), PHCBrightGalaxies },
		{ ct_("Symbiotic stars"), PHCSymbioticStars }, { ct_("Emission-line stars"), PHCEmissionLineStars },
		{ ct_("Interstellar objects"), PHCInterstellarObjects }, { ct_("Planets and Sun"), PHCPlanetsSun },
		{ ct_("Sun, planets and moons of observer's planet"), PHCSunPlanetsMoons },
		{ ct_("Bright Solar system objects (<%1 mag)").arg(QString::number(kBrightLimit + 2.0, 'f', 1)), PHCBrightSolarSystemObjects },
		{ ct_("Solar system objects: minor bodies"), PHCSolarSystemMinorBodies },
		{ ct_("Moons of first body"), PHCMoonsFirstBody },
		{ ct_("Bright carbon stars"), PHCBrightCarbonStars }, { ct_("Bright barium stars"), PHCBrightBariumStars },
		{ ct_("Sun, planets and moons of first body and observer's planet"), PHCSunPlanetsTheirMoons }
	};

	QJsonArray out;
	for (QMap<QString, int>::const_iterator it = itemsMap.constBegin(); it != itemsMap.constEnd(); ++it)
	{
		QJsonObject entry;
		entry["id"] = QString::number(it.value());
		entry["name"] = it.key();
		out.append(entry);
	}
	return out;
}

StelObjectP selectedObject()
{
	const QList<StelObjectP> selection = StelApp::getInstance().getStelObjectMgr().getSelectedObject();
	return selection.isEmpty() ? StelObjectP() : selection.first();
}

void calculate(StelCore* core, const QString& arg)
{
	kRows = QJsonArray();
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	NebulaMgr* dsoMgr = GETSTELMODULE(NebulaMgr);
	StarMgr* starMgr = GETSTELMODULE(StarMgr);
	if (!solarSystem || !dsoMgr || !starMgr)
		return;

	const QString currentPlanet = setting("phenomena_celestial_body", "Venus");
	const double separation = settingDouble("phenomena_angular_separation", 1.0);
	const bool opposition = settingBool("flag_phenomena_opposition", false);
	const bool perihelion = settingBool("flag_phenomena_perihelion", false);
	const bool quadrature = settingBool("flag_phenomena_quadratures", false);
	const int obj2Type = settingInt("phenomena_celestial_group", PHCPlanets);

	static const QRegularExpression monthPattern(QStringLiteral("^(-?\\d+)-(\\d+)$"));
	const QRegularExpressionMatch match = monthPattern.match(arg.trimmed());
	int year = 0, month = 1, day = 1;
	if (match.hasMatch())
	{
		year = match.captured(1).toInt();
		month = match.captured(2).toInt();
	}
	else
		StelUtils::getDateFromJulianDay(core->getJD() + core->getUTCOffset(core->getJD()) / 24., &year, &month, &day);

	double startJD = 0.;
	StelUtils::getJDFromDate(&startJD, year, month, 1, 0, 0, 1);

	double stopJD = startJD + qBound(1, settingInt("phenomena_duration_months", 1), 60) * 30.4375;

	QVector<PlanetP> objects;
	const QList<PlanetP> allSSObjects = solarSystem->getAllPlanets();
	QVector<NebulaP> dso;
	const QVector<NebulaP> allDSO = dsoMgr->getAllDeepSkyObjects();
	QVector<StelObjectP> star;
	const QVector<StelObjectP> hipStars = starMgr->getHipparcosStars().toVector();
	const QVector<StelObjectP> carbonStars = starMgr->getHipparcosCarbonStars().toVector();
	const QVector<StelObjectP> bariumStars = starMgr->getHipparcosBariumStars().toVector();
	const QVector<StelACStarData> doubleHipStars = starMgr->getHipparcosDoubleStars().toVector();
	const QVector<StelACStarData> variableHipStars = starMgr->getHipparcosVariableStars().toVector();

	switch (obj2Type)
	{
		case PHCSolarSystem:
			for (const auto& object : allSSObjects)
			{
				if (object->getPlanetType() != Planet::isUNDEFINED && object->getPlanetType() != Planet::isObserver)
					objects.append(object);
			}
			break;
		case PHCPlanets:
			for (const auto& object : allSSObjects)
			{
				if (object->getPlanetType() == Planet::isPlanet
				    && object->getEnglishName() != core->getCurrentPlanet()->getEnglishName()
				    && object->getEnglishName() != currentPlanet)
					objects.append(object);
			}
			break;
		case PHCAsteroids:
		case PHCPlutinos:
		case PHCComets:
		case PHCDwarfPlanets:
		case PHCCubewanos:
		case PHCScatteredDiscObjects:
		case PHCOortCloudObjects:
		case PHCSednoids:
		case PHCInterstellarObjects:
		{
			static const QMap<int, Planet::PlanetType> map = {
				{ PHCAsteroids,            Planet::isAsteroid },
				{ PHCPlutinos,             Planet::isPlutino },
				{ PHCComets,               Planet::isComet },
				{ PHCDwarfPlanets,         Planet::isDwarfPlanet },
				{ PHCCubewanos,            Planet::isCubewano },
				{ PHCScatteredDiscObjects, Planet::isSDO },
				{ PHCOortCloudObjects,     Planet::isOCO },
				{ PHCSednoids,             Planet::isSednoid },
				{ PHCInterstellarObjects,  Planet::isInterstellar } };
			const Planet::PlanetType pType = map.value(obj2Type, Planet::isUNDEFINED);

			for (const auto& object : allSSObjects)
			{
				if (object->getPlanetType() == pType)
					objects.append(object);
			}
			break;
		}
		case PHCBrightStars:
		case PHCBrightCarbonStars:
		case PHCBrightBariumStars:
		{
			QVector<StelObjectP> stars;
			if (obj2Type == PHCBrightStars)
				stars = hipStars;
			else if (obj2Type == PHCBrightBariumStars)
				stars = bariumStars;
			else
				stars = carbonStars;
			for (const auto& object : std::as_const(stars))
			{
				if (object->getVMagnitude(core) < static_cast<float>(kBrightLimit - 5.0))
					star.append(object);
			}
			break;
		}
		case PHCBrightDoubleStars:
			for (const auto& object : doubleHipStars)
			{
				if (object.first->getVMagnitude(core) < static_cast<float>(kBrightLimit - 5.0))
					star.append(object.first);
			}
			break;
		case PHCBrightVariableStars:
			for (const auto& object : variableHipStars)
			{
				if (object.first->getVMagnitude(core) < static_cast<float>(kBrightLimit - 5.0))
					star.append(object.first);
			}
			break;
		case PHCBrightStarClusters:
			for (const auto& object : allDSO)
			{
				if (object->getVMagnitude(core) < static_cast<float>(kBrightLimit)
				    && (object->getDSOType() == Nebula::NebCl || object->getDSOType() == Nebula::NebOc
				        || object->getDSOType() == Nebula::NebGc || object->getDSOType() == Nebula::NebSA
				        || object->getDSOType() == Nebula::NebSC || object->getDSOType() == Nebula::NebCn))
					dso.append(object);
			}
			break;
		case PHCPlanetaryNebulae:
			for (const auto& object : allDSO)
			{
				if (object->getVMagnitude(core) < kBrightLimit
				    && (object->getDSOType() == Nebula::NebPn || object->getDSOType() == Nebula::NebPossPN
				        || object->getDSOType() == Nebula::NebPPN))
					dso.append(object);
			}
			break;
		case PHCBrightNebulae:
			for (const auto& object : allDSO)
			{
				if (object->getVMagnitude(core) < kBrightLimit
				    && (object->getDSOType() == Nebula::NebN || object->getDSOType() == Nebula::NebBn
				        || object->getDSOType() == Nebula::NebEn || object->getDSOType() == Nebula::NebRn
				        || object->getDSOType() == Nebula::NebHII || object->getDSOType() == Nebula::NebISM
				        || object->getDSOType() == Nebula::NebCn || object->getDSOType() == Nebula::NebSNR))
					dso.append(object);
			}
			break;
		case PHCDarkNebulae:
			for (const auto& object : allDSO)
			{
				if (object->getDSOType() == Nebula::NebDn || object->getDSOType() == Nebula::NebMolCld
				    || object->getDSOType() == Nebula::NebYSO)
					dso.append(object);
			}
			break;
		case PHCBrightGalaxies:
			for (const auto& object : allDSO)
			{
				if (object->getVMagnitude(core) < kBrightLimit
				    && (object->getDSOType() == Nebula::NebGx || object->getDSOType() == Nebula::NebAGx
				        || object->getDSOType() == Nebula::NebRGx || object->getDSOType() == Nebula::NebQSO
				        || object->getDSOType() == Nebula::NebPossQSO || object->getDSOType() == Nebula::NebBLL
				        || object->getDSOType() == Nebula::NebBLA || object->getDSOType() == Nebula::NebIGx))
					dso.append(object);
			}
			break;
		case PHCSymbioticStars:
			for (const auto& object : allDSO)
			{
				if (object->getDSOType() == Nebula::NebSymbioticStar)
					dso.append(object);
			}
			break;
		case PHCEmissionLineStars:
			for (const auto& object : allDSO)
			{
				if (object->getDSOType() == Nebula::NebEmissionLineStar)
					dso.append(object);
			}
			break;
		case PHCPlanetsSun:
			for (const auto& object : allSSObjects)
			{
				if ((object->getPlanetType() == Planet::isPlanet || object->getPlanetType() == Planet::isStar)
				    && object->getEnglishName() != core->getCurrentPlanet()->getEnglishName()
				    && object->getEnglishName() != currentPlanet)
					objects.append(object);
			}
			break;
		case PHCSunPlanetsMoons:
		{
			PlanetP cp = core->getCurrentPlanet();
			for (const auto& object : allSSObjects)
			{
				if ((object->getPlanetType() == Planet::isPlanet || object->getPlanetType() == Planet::isStar
				     || (object->getParent() == cp && object->getPlanetType() == Planet::isMoon))
				    && object->getEnglishName() != cp->getEnglishName()
				    && object->getEnglishName() != currentPlanet)
					objects.append(object);
			}
			break;
		}
		case PHCBrightSolarSystemObjects:
			for (const auto& object : allSSObjects)
			{
				if (object->getVMagnitude(core) < (kBrightLimit + 2.0)
				    && object->getPlanetType() != Planet::isUNDEFINED)
					objects.append(object);
			}
			break;
		case PHCSolarSystemMinorBodies:
			for (const auto& object : allSSObjects)
			{
				if (object->getPlanetType() != Planet::isUNDEFINED && object->getPlanetType() != Planet::isPlanet
				    && object->getPlanetType() != Planet::isStar && object->getPlanetType() != Planet::isMoon
				    && object->getPlanetType() != Planet::isComet && object->getPlanetType() != Planet::isArtificial
				    && object->getPlanetType() != Planet::isObserver
				    && !object->getEnglishName().contains("Pluto", Qt::CaseInsensitive))
					objects.append(object);
			}
			break;
		case PHCMoonsFirstBody:
		{
			PlanetP firstPlanet = solarSystem->searchByEnglishName(currentPlanet);
			for (const auto& object : allSSObjects)
			{
				if (object->getParent() == firstPlanet && object->getPlanetType() == Planet::isMoon)
					objects.append(object);
			}
			break;
		}
		case PHCSunPlanetsTheirMoons:
		{
			PlanetP firstPlanet = solarSystem->searchByEnglishName(currentPlanet);
			PlanetP cp = core->getCurrentPlanet();
			for (const auto& object : allSSObjects)
			{
				if (object->getEnglishName() != cp->getEnglishName() && object->getEnglishName() != currentPlanet)
				{
					if (object->getPlanetType() == Planet::isPlanet || object->getPlanetType() == Planet::isStar)
						objects.append(object);

					if ((object->getParent() == cp || object->getParent() == firstPlanet)
					    && object->getPlanetType() == Planet::isMoon)
						objects.append(object);
				}
			}
			break;
		}
	}

	PlanetP planet = solarSystem->searchByEnglishName(currentPlanet);
	PlanetP sun = solarSystem->getSun();
	if (planet)
	{
		const double currentJD = core->getJD();
		startJD -= core->getUTCOffset(startJD) / 24.;
		stopJD -= core->getUTCOffset(stopJD) / 24.;

		if (obj2Type == PHCLatestSelectedObject)
		{
			StelObjectP latest = selectedObject();
			if (!latest.isNull() && latest != planet && latest->getType() != "Satellite")
			{
				fillPhenomenaTable(core, kRows,
				                   findClosestApproach(core, planet, latest, startJD, stopJD, separation, Conjunction),
				                   planet, latest, Conjunction);
				if (opposition)
					fillPhenomenaTable(core, kRows,
					                   findClosestApproach(core, planet, latest, startJD, stopJD, separation, Opposition),
					                   planet, latest, Opposition);
			}
		}
		else if ((obj2Type >= PHCSolarSystem && obj2Type < PHCBrightStars)
		         || (obj2Type >= PHCInterstellarObjects && obj2Type <= PHCMoonsFirstBody)
		         || (obj2Type == PHCSunPlanetsTheirMoons))
		{
			for (auto& obj : objects)
			{
				StelObjectP mObj = qSharedPointerCast<StelObject>(obj);
				fillPhenomenaTable(core, kRows,
				                   findClosestApproach(core, planet, mObj, startJD, stopJD, separation, Conjunction),
				                   planet, obj, Conjunction);
				if (opposition)
					fillPhenomenaTable(core, kRows,
					                   findClosestApproach(core, planet, mObj, startJD, stopJD, separation, Opposition),
					                   planet, obj, Opposition);

				if (obj2Type == PHCMoonsFirstBody || obj2Type == PHCSolarSystem
				    || obj2Type == PHCBrightSolarSystemObjects || obj2Type == PHCSunPlanetsTheirMoons)
					fillPhenomenaTable(core, kRows,
					                   findClosestApproach(core, planet, mObj, startJD, stopJD, separation, Shadows),
					                   planet, obj, Shadows);
			}
		}
		else if (obj2Type == PHCBrightStars || obj2Type == PHCBrightDoubleStars || obj2Type == PHCBrightVariableStars
		         || obj2Type == PHCBrightCarbonStars || obj2Type == PHCBrightBariumStars)
		{
			for (auto& obj : star)
			{
				StelObjectP mObj = qSharedPointerCast<StelObject>(obj);
				fillPhenomenaTable(core, kRows,
				                   findClosestApproach(core, planet, mObj, startJD, stopJD, separation, Conjunction),
				                   planet, obj, Conjunction);
			}
		}
		else
		{
			for (auto& obj : dso)
			{
				StelObjectP mObj = qSharedPointerCast<StelObject>(obj);
				fillPhenomenaTable(core, kRows,
				                   findClosestApproach(core, planet, mObj, startJD, stopJD, separation, Conjunction),
				                   planet, obj);
			}
		}

		if (planet != sun && planet->getPlanetType() != Planet::isMoon)
		{
			StelObjectP mObj = qSharedPointerCast<StelObject>(sun);
			if (quadrature)
			{
				if (planet->getHeliocentricEclipticPos().norm()
				    < core->getCurrentPlanet()->getHeliocentricEclipticPos().norm())
				{
					fillPhenomenaTable(core, kRows,
					                   findGreatestElongationApproach(core, planet, mObj, startJD, stopJD),
					                   planet, sun, GreatestElongation);
				}
				else
				{
					fillPhenomenaTable(core, kRows,
					                   findQuadratureApproach(core, planet, mObj, startJD, stopJD),
					                   planet, sun, Quadrature);
				}
			}

			fillPhenomenaTable(core, kRows, findStationaryPointApproach(core, planet, startJD, stopJD),
			                   planet, sun, StationaryPoint);

			if (perihelion)
				fillPhenomenaTable(core, kRows, findOrbitalPointApproach(core, planet, startJD, stopJD),
				                   planet, sun, OrbitalPoint);
		}

		core->setJD(currentJD);
		core->update(0);
	}

	QList<QJsonValue> sorted;
	for (const QJsonValue& row : std::as_const(kRows))
		sorted.append(row);
	std::sort(sorted.begin(), sorted.end(), [](const QJsonValue& a, const QJsonValue& b)
	          { return a.toObject().value("jd").toDouble() < b.toObject().value("jd").toDouble(); });
	kRows = QJsonArray();
	for (const QJsonValue& row : std::as_const(sorted))
		kRows.append(row);
}

void goToObject(StelCore* core, const QString& name, double JD)
{
	StelObjectMgr& objects = StelApp::getInstance().getStelObjectMgr();

	auto select = [&objects, &name]()
	{
		return objects.findAndSelectI18n(name, "Planet") || objects.findAndSelect(name, "Planet")
		    || objects.findAndSelectI18n(name) || objects.findAndSelect(name);
	};
	if (!select())
		return;

	core->setJD(JD);
	core->update(0.0);

	select();
	const QList<StelObjectP> newSelected = objects.getSelectedObject();
	if (newSelected.isEmpty())
		return;

	StelMovementMgr* movement = core->getMovementMgr();
	if (!movement)
		return;

	if (newSelected.first()->getEnglishName() == core->getCurrentLocation().planetName)
		objects.unSelect();
	else
	{
		asteriumMoveToSelected(movement, newSelected.first(), movement->getAutoMoveDuration());
	}
}
}

bool answer(const QString& verb, const QString& arg, QJsonObject& out)
{
	if (verb != QLatin1String("phenomena") && verb != QLatin1String("phenomena.generate"))
		return false;

	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	if (verb == QLatin1String("phenomena.generate"))
		calculate(core, arg);

	out["body"] = setting("phenomena_celestial_body", "Venus");
	out["group"] = setting("phenomena_celestial_group", QString::number(PHCPlanets));
	out["separation"] = settingDouble("phenomena_angular_separation", 1.0);
	out["opposition"] = settingBool("flag_phenomena_opposition", false);
	out["perihelion"] = settingBool("flag_phenomena_perihelion", false);
	out["quadratures"] = settingBool("flag_phenomena_quadratures", false);
	out["months"] = settingInt("phenomena_duration_months", 1);
	out["bodies"] = bodies(GETSTELMODULE(SolarSystem), core);
	out["groups"] = groupList();

	out["from"] = asteriumFormatSimTime(core->getJD(), "yyyy-MM");

	const StelObjectP latest = selectedObject();
	out["selected"] = latest.isNull() ? QString() : latest->getNameI18n();
	out["rows"] = kRows;
	return true;
}

bool perform(const QString& verb, const QString& arg)
{
	if (verb == QLatin1String("phenomena.set"))
	{
		const int split = arg.indexOf('=');
		if (split < 0)
			return true;
		const QString key = arg.left(split);
		const QString value = arg.mid(split + 1);

		writeSetting(key, key == QLatin1String("phenomena_angular_separation")
		                  ? QString::number(value.toDouble(), 'f', 5) : value);
		return true;
	}
	if (verb == QLatin1String("phenomena.clear"))
	{
		kRows = QJsonArray();
		return true;
	}
	if (verb == QLatin1String("phenomena.goto"))
	{
		const int index = arg.toInt();
		if (index < 0 || index >= kRows.size())
			return true;
		const QJsonObject row = kRows.at(index).toObject();
		const QString name = row.value("select").toString();
		if (!name.isEmpty())
			goToObject(StelApp::getInstance().getCore(), name, row.value("jd").toDouble());
		return true;
	}
	return false;
}
}
}
