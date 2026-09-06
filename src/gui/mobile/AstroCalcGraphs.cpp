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

#include "AstroCalcChart.hpp"
#include "Planet.hpp"
#include "SolarSystem.hpp"
#include "NebulaMgr.hpp"
#include "StelApp.hpp"
#include "StelCore.hpp"
#include "StelModuleMgr.hpp"
#include "StelObject.hpp"
#include "StelObjectMgr.hpp"
#include "StelTranslator.hpp"
#include "StelUtils.hpp"

#ifdef USE_STATIC_PLUGIN_SATELLITES
#include "../plugins/Satellites/src/Satellites.hpp"
#endif

#include <QJsonArray>
#include <QList>
#include <QMap>
#include <QPair>
#include <QString>

#include <algorithm>
#include <cmath>

namespace AsteriumAstroCalc {
namespace Graphs {
namespace {
const double kUnixEpochJD = 2440587.5;

const int kMaxSamples = 1200;

double millis(double jd)
{
	return (jd - kUnixEpochJD) * 86400000.;
}

QJsonObject point(double jd, double value)
{
	QJsonObject entry;
	entry["x"] = millis(jd);
	entry["y"] = value;
	return entry;
}

QJsonObject curve(const QString& id, const QString& name, int axis, const QJsonArray& points)
{
	QJsonObject series;
	series["id"] = id;
	series["name"] = name;
	series["axis"] = axis;
	series["points"] = points;
	return series;
}

QPair<double, double> range(const QJsonArray& series, int axis)
{
	double low = 0., high = 0.;
	bool any = false;
	for (const QJsonValue& value : series)
	{
		const QJsonObject one = value.toObject();
		if (one["axis"].toInt() != axis)
			continue;
		for (const QJsonValue& sample : one["points"].toArray())
		{
			const double y = sample.toObject()["y"].toDouble();
			low = any ? std::min(low, y) : y;
			high = any ? std::max(high, y) : y;
			any = true;
		}
	}
	return any ? qMakePair(low, high) : qMakePair(0., 0.);
}

void setRange(QJsonObject& out, const QJsonArray& series, int axis, double fallbackLow,
              double fallbackHigh)
{
	const QPair<double, double> span = range(series, axis);
	const bool empty = qFuzzyCompare(span.first + 1., span.second + 1.);
	const QString prefix = axis == 0 ? QStringLiteral("y") : QStringLiteral("y2");
	out[prefix + "Min"] = empty ? fallbackLow : span.first;
	out[prefix + "Max"] = empty ? fallbackHigh : span.second;
}

QString selectedName(const StelObjectP& object)
{
	QString name = object->getNameI18n();
	if (!name.isEmpty())
		return name;
	const QString type = object->getType();
	if (type == QLatin1String("Nebula"))
	{
		NebulaMgr* dsoMgr = GETSTELMODULE(NebulaMgr);
		name = dsoMgr ? dsoMgr->getLatestSelectedDSODesignation() : QString();
		if (name.isEmpty() && dsoMgr)
			name = dsoMgr->getLatestSelectedDSODesignationWIC();
	}
	else if (type == QLatin1String("Star") || type == QLatin1String("Pulsar"))
		name = object->getID().isEmpty() ? ct_("Unnamed star") : object->getID();
	return name;
}

StelObjectP selection()
{
	const QList<StelObjectP> found = StelApp::getInstance().getStelObjectMgr().getSelectedObject();
	return found.isEmpty() ? StelObjectP() : found.first();
}

bool travelling(StelCore* core)
{
	return core->getCurrentPlanet()->getEnglishName().contains(QLatin1String("->"));
}

struct Function { const char* label; AstroCalcChart::Series first, second; };
const Function kFunctions[] = {
	{ "Magnitude vs. Time",    AstroCalcChart::Magnitude1,    AstroCalcChart::Magnitude2 },
	{ "Phase vs. Time",        AstroCalcChart::Phase1,        AstroCalcChart::Phase2 },
	{ "Distance vs. Time",     AstroCalcChart::Distance1,     AstroCalcChart::Distance2 },
	{ "Elongation vs. Time",   AstroCalcChart::Elongation1,   AstroCalcChart::Elongation2 },
	{ "Angular size vs. Time", AstroCalcChart::AngularSize1,  AstroCalcChart::AngularSize2 },
	{ "Phase angle vs. Time",  AstroCalcChart::PhaseAngle1,   AstroCalcChart::PhaseAngle2 },
	{ "Heliocentric distance vs. Time", AstroCalcChart::HeliocentricDistance1,
	                                    AstroCalcChart::HeliocentricDistance2 },
	{ "Transit altitude vs. Time", AstroCalcChart::TransitAltitude1,
	                               AstroCalcChart::TransitAltitude2 },
	{ "Right ascension vs. Time", AstroCalcChart::RightAscension1,
	                              AstroCalcChart::RightAscension2 },
	{ "Declination vs. Time",  AstroCalcChart::Declination1,  AstroCalcChart::Declination2 },
};

double graphValue(const PlanetP& body, StelCore* core, int graph)
{
	switch (graph)
	{
		case AstroCalcChart::Magnitude1:
		case AstroCalcChart::Magnitude2:
			return static_cast<double>(body->getVMagnitude(core));
		case AstroCalcChart::Phase1:
		case AstroCalcChart::Phase2:
			return static_cast<double>(body->getPhase(core->getObserverHeliocentricEclipticPos())) * 100.;
		case AstroCalcChart::Distance1:
		case AstroCalcChart::Distance2:
		{
			double value = body->getJ2000EquatorialPos(core).norm();

			if (body->getEnglishName() == QLatin1String("Moon"))
				value *= AU * 0.001;
			return value;
		}
		case AstroCalcChart::Elongation1:
		case AstroCalcChart::Elongation2:
			return body->getElongation(core->getObserverHeliocentricEclipticPos()) * M_180_PI;
		case AstroCalcChart::AngularSize1:
		case AstroCalcChart::AngularSize2:
		{
			double value = body->getSpheroidAngularRadius(core) * 7200.;
			if (value >= 60.)
				value /= 60.;
			return value;
		}
		case AstroCalcChart::PhaseAngle1:
		case AstroCalcChart::PhaseAngle2:
			return body->getPhaseAngle(core->getObserverHeliocentricEclipticPos()) * M_180_PI;
		case AstroCalcChart::HeliocentricDistance1:
		case AstroCalcChart::HeliocentricDistance2:
			return body->getHeliocentricEclipticPos().norm();
		case AstroCalcChart::TransitAltitude1:
		case AstroCalcChart::TransitAltitude2:
		{
			double azimuth = 0., altitude = 0.;
			StelUtils::rectToSphe(&azimuth, &altitude, body->getAltAzPosAuto(core));
			return altitude * M_180_PI;
		}
		case AstroCalcChart::RightAscension1:
		case AstroCalcChart::RightAscension2:
		{
			double ra = 0., dec = 0.;
			StelUtils::rectToSphe(&ra, &dec, body->getEquinoxEquatorialPos(core));
			ra = 2. * M_PI - ra;
			double value = ra * 12. / M_PI;
			if (value > 24.)
				value -= 24.;
			return value;
		}
		case AstroCalcChart::Declination1:
		case AstroCalcChart::Declination2:
		{
			double ra = 0., dec = 0.;
			StelUtils::rectToSphe(&ra, &dec, body->getEquinoxEquatorialPos(core));
			return dec * M_180_PI;
		}
		default:
			return 0.;
	}
}

QString graphAxisTitle(int graph, const QString& englishName)
{
	const QChar degree(0x00B0);
	const bool nearby = (englishName == QLatin1String("Moon") || englishName == QLatin1String("Sun"));
	switch (graph)
	{
		case AstroCalcChart::Magnitude1:
		case AstroCalcChart::Magnitude2:
			return ct_("Magnitude");
		case AstroCalcChart::Phase1:
		case AstroCalcChart::Phase2:
			return QString("%1, %").arg(ct_("Phase"));
		case AstroCalcChart::Distance1:
		case AstroCalcChart::Distance2:
			return QString("%1, %2").arg(ct_("Distance"),
			                             englishName == QLatin1String("Moon")
			                             ? qc_("Mm", "distance, Megameters")
			                             : qc_("AU", "distance, astronomical unit"));
		case AstroCalcChart::Elongation1:
		case AstroCalcChart::Elongation2:
			return QString("%1, %2").arg(ct_("Elongation"), degree);
		case AstroCalcChart::AngularSize1:
		case AstroCalcChart::AngularSize2:
			return QString("%1, %2").arg(ct_("Angular size"), nearby ? QString("'") : QString("\""));
		case AstroCalcChart::PhaseAngle1:
		case AstroCalcChart::PhaseAngle2:
			return QString("%1, %2").arg(ct_("Phase angle"), degree);
		case AstroCalcChart::HeliocentricDistance1:
		case AstroCalcChart::HeliocentricDistance2:
			return QString("%1, %2").arg(ct_("Heliocentric distance"),
			                             qc_("AU", "distance, astronomical unit"));
		case AstroCalcChart::TransitAltitude1:
		case AstroCalcChart::TransitAltitude2:
			return QString("%1, %2").arg(ct_("Culmination altitude"), degree);
		case AstroCalcChart::RightAscension1:
		case AstroCalcChart::RightAscension2:
			return QString("%1, %2").arg(ct_("Right ascension"), qc_("h", "time"));
		default:
			return QString("%1, %2").arg(ct_("Declination"), degree);
	}
}

bool reversed(int graph)
{
	return graph == AstroCalcChart::Magnitude1 || graph == AstroCalcChart::Magnitude2;
}

void altVsTime(StelCore* core, QJsonObject& out)
{
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	out["xLabel"] = ct_("Local Time");
	out["xFormat"] = QStringLiteral("HH:mm");
	out["yLabel"] = QString("%1, %2").arg(ct_("Altitude"), QChar(0x00B0));

	const bool withSun = settingBool("altvstime_sun", false);
	const bool withMoon = settingBool("altvstime_moon", false);
	const bool positiveOnly = settingBool("altvstime_positive_only", false);
	const double positiveLimit = settingInt("altvstime_positive_limit", 0);
	out["sun"] = withSun;
	out["moon"] = withMoon;
	out["positiveOnly"] = positiveOnly;
	out["positiveLimit"] = positiveLimit;

	const StelObjectP object = selection();
	const double currentJD = core->getJD();
	const double shift = core->getUTCOffset(currentJD) / 24.;
	const double noon = std::floor(currentJD + shift);
	out["xMin"] = millis(noon);
	out["xMax"] = millis(noon + 1.);
	out["now"] = millis(currentJD + shift);
	if (object.isNull() || !solarSystem)
	{
		out["note"] = ct_("Please select object to plot its graph 'Altitude vs. Time'.");
		out["yMin"] = -90.;
		out["yMax"] = 90.;
		return;
	}
	out["title"] = QString("%1 (%2)").arg(selectedName(object), object->getObjectTypeI18n());

	const PlanetP sun = solarSystem->getSun();
	const PlanetP moon = solarSystem->getMoon();
	const bool onEarth = core->getCurrentPlanet() == solarSystem->getEarth();

	bool isSatellite = false;
#ifdef USE_STATIC_PLUGIN_SATELLITES
	SatelliteP satellite;
	if (object->getType() == QLatin1String("Satellite"))
	{
		isSatellite = true;
		satellite = GETSTELMODULE(Satellites)->getById(
				object.staticCast<Satellite>()->getCatalogNumberString());
	}
#endif

	QJsonArray altitudes, sunElevation, civil, nautical, astronomical, moonAltitude;

	for (int i = 0; i <= 86400; i += (isSatellite ? 600 : 3600))
	{
		const double jd = noon + i / 86400. - shift;
		core->setJD(jd);
		core->update(0.);
#ifdef USE_STATIC_PLUGIN_SATELLITES
		if (isSatellite && !satellite.isNull())
			satellite->update(core, jd);
#endif
		double azimuth = 0., altitude = 0.;
		StelUtils::rectToSphe(&azimuth, &altitude, object->getAltAzPosAuto(core));
		altitudes.append(point(jd + shift, altitude * M_180_PI));

		if (withSun)
		{
			StelUtils::rectToSphe(&azimuth, &altitude, sun->getAltAzPosAuto(core));
			const double degrees = altitude * M_180_PI;
			sunElevation.append(point(jd + shift, degrees));

			civil.append(point(jd + shift, degrees + 6.));
			nautical.append(point(jd + shift, degrees + 12.));
			astronomical.append(point(jd + shift, degrees + 18.));
		}
		if (withMoon && onEarth)
		{
			StelUtils::rectToSphe(&azimuth, &altitude, moon->getAltAzPosAuto(core));
			moonAltitude.append(point(jd + shift, altitude * M_180_PI));
		}
	}
	core->setJD(currentJD);
	core->update(0.);

	QJsonArray series;
	series.append(curve("alt", ct_("Altitude"), 0, altitudes));
	if (withSun)
	{
		series.append(curve("sun", ct_("Sun"), 0, sunElevation));
		series.append(curve("civil", ct_("Civil Twilight"), 0, civil));
		series.append(curve("nautical", ct_("Nautical Twilight"), 0, nautical));
		series.append(curve("astro", ct_("Astronomical Twilight"), 0, astronomical));
	}
	if (withMoon && onEarth)
		series.append(curve("moon", ct_("Moon"), 0, moonAltitude));
	out["series"] = series;

	setRange(out, series, 0, -90., 90.);
	if (positiveOnly && out["yMin"].toDouble() < positiveLimit)
		out["yMin"] = positiveLimit;

	double best = 0., bestX = 0.;
	bool any = false;
	for (const QJsonValue& value : std::as_const(altitudes))
	{
		const QJsonObject sample = value.toObject();
		if (!any || sample["y"].toDouble() > best)
		{
			best = sample["y"].toDouble();
			bestX = sample["x"].toDouble();
			any = true;
		}
	}
	if (any)
		out["transit"] = bestX;
}

void aziVsTime(StelCore* core, QJsonObject& out)
{
	out["xLabel"] = ct_("Local Time");
	out["xFormat"] = QStringLiteral("HH:mm");
	out["yLabel"] = QString("%1, %2").arg(ct_("Azimuth"), QChar(0x00B0));

	const StelObjectP object = selection();
	const double currentJD = core->getJD();
	const double shift = core->getUTCOffset(currentJD) / 24.;
	const double noon = std::floor(currentJD + shift);
	out["xMin"] = millis(noon);
	out["xMax"] = millis(noon + 1.);
	out["now"] = millis(currentJD + shift);
	if (object.isNull())
	{
		out["note"] = ct_("Please select object to plot its graph 'Azimuth vs. Time'.");
		out["yMin"] = 0.;
		out["yMax"] = 360.;
		return;
	}
	out["title"] = QString("%1 (%2)").arg(selectedName(object), object->getObjectTypeI18n());

	const bool southAzimuth = StelApp::getInstance().getFlagSouthAzimuthUsage();
	bool isSatellite = false;
#ifdef USE_STATIC_PLUGIN_SATELLITES
	SatelliteP satellite;
	if (object->getType() == QLatin1String("Satellite"))
	{
		isSatellite = true;
		satellite = GETSTELMODULE(Satellites)->getById(
				object.staticCast<Satellite>()->getCatalogNumberString());
	}
#endif

	QJsonArray azimuths;
	for (int i = 0; i <= 86400; i += 180)
	{
		const double jd = noon + i / 86400. - shift;
		core->setJD(jd);
#ifdef USE_STATIC_PLUGIN_SATELLITES
		if (isSatellite && !satellite.isNull())
			satellite->update(core, jd);
		else
#endif
			core->update(0.);
		double azimuth = 0., altitude = 0.;
		StelUtils::rectToSphe(&azimuth, &altitude, object->getAltAzPosAuto(core));

		azimuth = (southAzimuth ? 2. : 3.) * M_PI - azimuth;
		if (azimuth > M_PI * 2)
			azimuth -= M_PI * 2;
		azimuths.append(point(jd + shift, azimuth * M_180_PI));
	}
	core->setJD(currentJD);
	core->update(0.);
	Q_UNUSED(isSatellite)

	QJsonArray series;
	series.append(curve("azi", ct_("Azimuth"), 0, azimuths));
	out["series"] = series;
	setRange(out, series, 0, 0., 360.);
}

void monthlyElevation(StelCore* core, QJsonObject& out)
{
	out["xLabel"] = ct_("Date");
	out["xFormat"] = QStringLiteral("d MMM");
	out["yLabel"] = QString("%1, %2").arg(ct_("Altitude"), QChar(0x00B0));

	const int hour = qBound(0, settingInt("me_time", 0), 23);
	const bool positiveOnly = settingBool("me_positive_only", false);
	const double positiveLimit = settingInt("me_positive_limit", 0);
	out["hour"] = hour;
	out["positiveOnly"] = positiveOnly;
	out["positiveLimit"] = positiveLimit;

	const double currentJD = core->getJD();
	const double utcOffset = core->getUTCOffset(currentJD) / 24.;
	out["now"] = millis(currentJD + utcOffset);

	const StelObjectP object = selection();
	if (object.isNull() || object->getType() == QLatin1String("Satellite"))
	{
		out["note"] = ct_("Please select object to plot its 'Monthly Elevation' graph for the current year at selected time.");
		out["yMin"] = -90.;
		out["yMax"] = 90.;
		return;
	}
	out["title"] = QString("%1 (%2)").arg(selectedName(object), object->getObjectTypeI18n());

	int year = 0, month = 0, day = 0;
	StelUtils::getDateFromJulianDay(currentJD, &year, &month, &day);
	double startJD = 0.;
	StelUtils::getJDFromDate(&startJD, year, 1, 1, hour, 0, 0);
	startJD -= utcOffset;

	const int steps = static_cast<int>(core->getCurrentPlanet()->getSiderealPeriod() / 5.) + 3;

	QJsonArray elevations;
	for (int i = 0; i <= steps; i += 3)
	{
		const double jd = startJD + i * 5;
		core->setJD(jd);
		core->update(0.);
		double azimuth = 0., altitude = 0.;
		StelUtils::rectToSphe(&azimuth, &altitude, object->getAltAzPosAuto(core));

		elevations.append(point(jd - hour / 24. + utcOffset, altitude * M_180_PI));
	}
	core->setJD(currentJD);
	core->update(0.);

	QJsonArray series;
	series.append(curve("me", ct_("Monthly Elevation"), 0, elevations));
	out["series"] = series;
	setRange(out, series, 0, -90., 90.);
	if (positiveOnly && out["yMin"].toDouble() < positiveLimit)
		out["yMin"] = positiveLimit;

	if (!elevations.isEmpty())
	{
		out["xMin"] = elevations.first().toObject()["x"].toDouble();
		out["xMax"] = elevations.last().toObject()["x"].toDouble();
	}
}

void lunarElongation(StelCore* core, QJsonObject& out)
{
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	out["xLabel"] = ct_("Date");
	out["xFormat"] = QStringLiteral("d MMM");
	out["yLabel"] = QString("%1, %2").arg(ct_("Lunar elongation"), QChar(0x00B0));

	const double currentJD = core->getJD();
	out["now"] = millis(currentJD + core->getUTCOffset(currentJD) / 24.);
	out["yMin"] = 0.;
	out["yMax"] = 180.;
	if (!solarSystem || core->getCurrentPlanet() != solarSystem->getEarth())
	{
		out["note"] = ct_("This tool works on Earth only!");
		return;
	}

	const PlanetP moon = solarSystem->getMoon();
	const StelObjectP object = selection();
	if (object.isNull())
	{
		out["note"] = ct_("Angular distance between the Moon and selected object");
		return;
	}
	if (object->getEnglishName() == moon->getEnglishName()
	    || object->getType() == QLatin1String("Satellite"))
	{
		out["note"] = ct_("Angular distance between the Moon and selected object");
		return;
	}

	int year = 0, month = 0, day = 0;
	StelUtils::getDateFromJulianDay(currentJD, &year, &month, &day);
	double firstJD = 0.;
	StelUtils::getJDFromDate(&firstJD, year, month, day, 0, 0, 0.f);
	const double utcOffset = core->getUTCOffset(firstJD) / 24.;
	firstJD += utcOffset;

	QJsonArray distances;
	for (int i = -2; i <= 32; i += 2)
	{
		const double jd = firstJD + i;
		core->setJD(jd);
		core->update(0.);
		const double distance = moon->getJ2000EquatorialPos(core)
		                            .angle(object->getJ2000EquatorialPos(core));
		distances.append(point(jd + utcOffset, distance * M_180_PI));
	}
	core->setJD(currentJD);
	core->update(0.);

	out["title"] = QString("%1: %2 - %3 (%4)").arg(ct_("Angular distance"), moon->getNameI18n(),
	                                               selectedName(object), object->getObjectTypeI18n());
	QJsonArray series;
	series.append(curve("lunar", ct_("Lunar Elongation"), 0, distances));
	out["series"] = series;
	setRange(out, series, 0, 0., 180.);
	if (!distances.isEmpty())
	{
		out["xMin"] = distances.first().toObject()["x"].toDouble();
		out["xMax"] = distances.last().toObject()["x"].toDouble();
	}
}

void xVsTime(StelCore* core, QJsonObject& out)
{
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	out["xLabel"] = ct_("Date");
	out["xFormat"] = QStringLiteral("d MMM");

	const QString bodyName = setting("graphs_celestial_body", QStringLiteral("Moon"));
	const int duration = qBound(1, settingInt("graphs_duration", 1), 600);
	const int step = qBound(1, settingInt("graphs_step", 1), 240);
	int first = settingInt("graphs_first_id", AstroCalcChart::Magnitude1);
	int second = settingInt("graphs_second_id", AstroCalcChart::Phase2);

	if (first == AstroCalcChart::AltVsTime)
		first = AstroCalcChart::AngularSize1;
	if (second == AstroCalcChart::AltVsTime)
		second = AstroCalcChart::Magnitude2;

	out["body"] = bodyName;
	out["bodies"] = bodies(solarSystem, core);
	out["duration"] = duration;
	out["step"] = step;
	out["first"] = QString::number(first);
	out["second"] = QString::number(second);

	QJsonArray firstList, secondList;
	for (const Function& function : kFunctions)
	{
		QJsonObject one, two;
		one["id"] = QString::number(function.first);
		one["name"] = ct_(QString::fromUtf8(function.label));
		two["id"] = QString::number(function.second);
		two["name"] = one["name"];
		firstList.append(one);
		secondList.append(two);
	}
	out["firstFunctions"] = firstList;
	out["secondFunctions"] = secondList;

	const PlanetP body = solarSystem ? solarSystem->searchByEnglishName(bodyName) : PlanetP();
	out["yLabel"] = graphAxisTitle(first, bodyName);
	out["y2Label"] = graphAxisTitle(second, bodyName);
	out["yReverse"] = reversed(first);
	out["y2Reverse"] = reversed(second);

	const double currentJD = core->getJD();
	if (body.isNull() || !solarSystem || core->getCurrentPlanet() != solarSystem->getEarth())
	{
		out["note"] = ct_("This tool works on Earth only!");
		out["yMin"] = 0.;
		out["yMax"] = 10.;
		out["y2Min"] = 0.;
		out["y2Max"] = 10.;
		return;
	}

	int year = 0, month = 0, day = 0;
	StelUtils::getDateFromJulianDay(currentJD, &year, &month, &day);
	double startJD = 0.;
	StelUtils::getJDFromDate(&startJD, year, month, 1, 0, 0, 0);
	const double utcOffset = core->getUTCOffset(startJD) / 24.;
	startJD += utcOffset;

	const int lastHour = (30 * 24) * duration + 24;
	int hourStep = step;
	if ((lastHour + 24) / hourStep + 1 > kMaxSamples)
		hourStep = (lastHour + 24) / kMaxSamples + 1;
	out["effectiveStep"] = hourStep;

	const bool firstIsTransit = (first == AstroCalcChart::TransitAltitude1);
	const bool secondIsTransit = (second == AstroCalcChart::TransitAltitude2);
	QJsonArray firstPoints, secondPoints;
	for (int i = -24; i <= lastHour; i += hourStep)
	{
		double jd = startJD + i / 24.;
		if (firstIsTransit || secondIsTransit)
		{
			core->setJD(jd);
			core->update(0.);
			jd = body->getRTSTime(core)[1];
		}
		core->setJD(jd);
		core->update(0.);
		firstPoints.append(point(jd + utcOffset, graphValue(body, core, first)));
		secondPoints.append(point(jd + utcOffset, graphValue(body, core, second)));
	}
	core->setJD(currentJD);
	core->update(0.);

	QJsonArray series;
	series.append(curve("first", graphAxisTitle(first, bodyName), 0, firstPoints));
	series.append(curve("second", graphAxisTitle(second, bodyName), 1, secondPoints));
	out["series"] = series;
	setRange(out, series, 0, 0., 10.);
	setRange(out, series, 1, 0., 10.);

	out["now"] = millis(currentJD + utcOffset);
	out["xMin"] = millis(startJD - 1.);
	out["xMax"] = millis(startJD + lastHour / 24.);
	int firstYear = 0, lastYear = 0;
	StelUtils::getDateFromJulianDay(startJD, &firstYear, &month, &day);
	StelUtils::getDateFromJulianDay(startJD + lastHour / 24., &lastYear, &month, &day);
	if (firstYear != lastYear)
		out["xFormat"] = QStringLiteral("d MMM yy");
}

QMap<QString, QString> kSignatures;
QMap<QString, QJsonObject> kCharts;

QString signatureOf(StelCore* core, const QString& tab)
{
	const StelObjectP object = selection();
	const double jd = core->getJD();
	return QStringList({
		tab,
		object.isNull() ? QString() : object->getEnglishName() + object->getType(),
		QString::number(std::floor(jd + core->getUTCOffset(jd) / 24.)),
		core->getCurrentLocation().serializeToLine(),
		setting("graphs_celestial_body", QStringLiteral("Moon")),
		QString::number(settingInt("graphs_first_id", AstroCalcChart::Magnitude1)),
		QString::number(settingInt("graphs_second_id", AstroCalcChart::Phase2)),
		QString::number(settingInt("graphs_duration", 1)),
		QString::number(settingInt("graphs_step", 1)),
		QString::number(settingBool("altvstime_sun", false)),
		QString::number(settingBool("altvstime_moon", false)),
		QString::number(settingBool("altvstime_positive_only", false)),
		QString::number(settingInt("altvstime_positive_limit", 0)),
		QString::number(settingBool("me_positive_only", false)),
		QString::number(settingInt("me_positive_limit", 0)),
		QString::number(settingInt("me_time", 0)),
	}).join(QChar('|'));
}
}

bool answer(const QString& verb, const QString& arg, QJsonObject& out)
{
	if (verb != QLatin1String("graphs"))
		return false;

	StelCore* core = StelApp::getInstance().getCore();
	const QString tab = arg.isEmpty() ? QStringLiteral("alt") : arg;

	QJsonArray tabs;
	const char* const names[] = { "Altitude vs. Time", "Azimuth vs. Time", "Monthly Elevation",
	                              "Graphs", "Lunar Elongation" };
	const char* const ids[] = { "alt", "azi", "me", "xy", "lunar" };
	for (int i = 0; i < 5; ++i)
	{
		QJsonObject entry;
		entry["id"] = QString::fromUtf8(ids[i]);
		entry["name"] = ct_(QString::fromUtf8(names[i]));
		tabs.append(entry);
	}
	out["tabs"] = tabs;
	out["tab"] = tab;

	const QString signature = signatureOf(core, tab);
	if (kSignatures.value(tab) == signature && kCharts.contains(tab))
	{
		const QJsonObject cached = kCharts.value(tab);
		for (auto it = cached.constBegin(); it != cached.constEnd(); ++it)
			out.insert(it.key(), it.value());
		out["now"] = millis(core->getJD() + core->getUTCOffset(core->getJD()) / 24.);
		return true;
	}

	QJsonObject chart;
	if (travelling(core))
		chart["note"] = QStringLiteral("No graph while the observer is between planets.");
	else if (tab == QLatin1String("azi"))
		aziVsTime(core, chart);
	else if (tab == QLatin1String("me"))
		monthlyElevation(core, chart);
	else if (tab == QLatin1String("xy"))
		xVsTime(core, chart);
	else if (tab == QLatin1String("lunar"))
		lunarElongation(core, chart);
	else
		altVsTime(core, chart);

	kSignatures.insert(tab, signature);
	kCharts.insert(tab, chart);
	for (auto it = chart.constBegin(); it != chart.constEnd(); ++it)
		out.insert(it.key(), it.value());
	out["now"] = millis(core->getJD() + core->getUTCOffset(core->getJD()) / 24.);
	return true;
}

bool perform(const QString& verb, const QString& arg)
{
	if (verb != QLatin1String("graphs.set"))
		return false;
	writeSetting(arg.section('=', 0, 0), arg.section('=', 1));

	kSignatures.clear();
	return true;
}
}
}
