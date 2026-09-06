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

#include "AstroCalcDialog.hpp"
#include "SolarEclipseComputer.hpp"

#include "Planet.hpp"
#include "SolarSystem.hpp"
#include "StelApp.hpp"
#include "StelCore.hpp"
#include "StelLocaleMgr.hpp"
#include "StelLocation.hpp"
#include "StelModuleMgr.hpp"
#include "StelMovementMgr.hpp"
#include "StelObjectMgr.hpp"
#include "StelTranslator.hpp"
#include "StelUtils.hpp"

#include <QJsonArray>
#include <QtMath>

#include <algorithm>
#include <cmath>

namespace AsteriumAstroCalc
{
namespace Eclipses
{
namespace
{
QJsonArray kSolarRows, kLocalRows, kLunarRows, kTransitRows;

int kFromYear = 0;
bool kFromYearSet = false;

const double kApproxNewMoon = 2451550.09765;
const double kSynodicMonth = 29.530588853;

StelCore* core()
{
	return StelApp::getInstance().getCore();
}

SolarSystem* system()
{
	return GETSTELMODULE(SolarSystem);
}

bool onEarth()
{
	SolarSystem* ss = system();
	return ss && core()->getCurrentPlanet() == ss->getEarth();
}

QString centralDuration(double minutes)
{
	const double length = std::abs(minutes);
	int durationMinute = int(length);
	int durationSecond = qRound((length - durationMinute) * 60.);
	if (durationSecond > 59)
	{
		durationMinute += 1;
		durationSecond = 0;
	}
	return durationSecond > 9
	       ? QString("%1m %2s").arg(QString::number(durationMinute), QString::number(durationSecond))
	       : QString("%1m 0%2s").arg(QString::number(durationMinute), QString::number(durationSecond));
}

int sarosNumber(double jd, bool lunar)
{
	const double q = std::round((jd - 2423436.40347) / 29.530588 - (lunar ? 0.25 : 0.));
	const int ln = int(q) + 1 - 953;
	const int nd = ln + 105;
	const int s = (lunar ? 148 : 136) + 38 * nd;
	const int nx = -61 * nd;
	const int nc = qFloor(nx / 358. + 0.5 - nd / (12. * 358 * 358));
	int saros = 1 + ((s + nc * 223 - 1) % 223);
	if ((s + nc * 223 - 1) < 0)
		saros -= 223;
	if (saros < -223)
		saros += 223;
	return saros;
}

void searchRange(int fromYear, int years, double& startJD, double& stopJD)
{
	StelUtils::getJDFromDate(&startJD, fromYear, 1, 1, 0, 0, 0);
	StelUtils::getJDFromDate(&stopJD, fromYear + years, 12, 31, 23, 59, 59);
	startJD -= core()->getUTCOffset(startJD) / 24.;
	stopJD -= core()->getUTCOffset(stopJD) / 24.;
}

void observerRadii(double& rc, double& rs)
{
	const StelLocation& here = core()->getCurrentLocation();
	SolarSystem* ss = system();
	const Vec4d geocentric = ss->getEarth()->getRectangularCoordinates(
			static_cast<double>(here.getLongitude()), static_cast<double>(here.getLatitude()),
			static_cast<double>(here.altitude));
	const double earthRadius = ss->getEarth()->getEquatorialRadius();
	rc = geocentric[0] / earthRadius;
	rs = geocentric[1] / earthRadius;
}

struct LocalSolar
{
	double dt;
	double L1;
	double L2;
	double ce;
	double magnitude;
	double altitude;
};

LocalSolar localSolarEclipse(double jd, int contact, bool central)
{
	LocalSolar result;
	const double lon = static_cast<double>(core()->getCurrentLocation().getLongitude());
	double rc = 0., rs = 0.;
	observerRadii(rc, rs);

	core()->setUseTopocentricCoordinates(false);
	core()->setJD(jd);
	core()->update(0);

	const EclipseBesselParameters bp = calcBesselParameters(true);
	const double xdot = bp.xdot, ydot = bp.ydot, ddot = bp.ddot, mudot = bp.mudot;
	const double x = bp.elems.x, y = bp.elems.y, d = bp.elems.d;
	const double tf1 = bp.elems.tf1, tf2 = bp.elems.tf2, mu = bp.elems.mu;
	double L1 = bp.elems.L1, L2 = bp.elems.L2;

	double theta = (mu + lon) * M_PI_180;
	theta = StelUtils::fmodpos(theta, 2. * M_PI);
	const double xi = rc * std::sin(theta);
	const double eta = rs * std::cos(d) - rc * std::sin(d) * std::cos(theta);
	const double zeta = rs * std::sin(d) + rc * std::cos(d) * std::cos(theta);
	const double xidot = mudot * rc * std::cos(theta);
	const double etadot = mudot * xi * std::sin(d) - zeta * ddot;
	const double u = x - xi;
	const double v = y - eta;
	const double udot = xdot - xidot;
	const double vdot = ydot - etadot;
	const double n2 = udot * udot + vdot * vdot;
	const double delta = (u * vdot - udot * v) / std::sqrt(n2);
	L1 = L1 - zeta * tf1;
	L2 = L2 - zeta * tf2;
	const double L = central ? L2 : L1;
	const double sfi = delta / L;
	const double ce = 1. - sfi * sfi;
	const double cfi = (ce > 0.) ? contact * std::sqrt(ce) : 0.;
	const double m = std::sqrt(u * u + v * v);

	result.magnitude = (L1 - m) / (L1 + L2);
	result.altitude = std::asin(rc * std::cos(d) * std::cos(theta) + rs * std::sin(d)) * M_180_PI;
	result.dt = (L * cfi / std::sqrt(n2)) - (u * udot + v * vdot) / n2;
	result.L1 = L1;
	result.L2 = L2;
	result.ce = ce;
	return result;
}

LocalSolar localTransit(double jd, int contact, bool central, PlanetP object, bool topocentric)
{
	LocalSolar result;
	const double lon = static_cast<double>(core()->getCurrentLocation().getLongitude());
	double rc = 0., rs = 0.;
	if (topocentric)
		observerRadii(rc, rs);

	core()->setUseTopocentricCoordinates(false);
	core()->setJD(jd);
	core()->update(0);

	double x, y, d, tf1, tf2, L1, L2, mu;
	TransitBessel::computeElements(object, x, y, d, tf1, tf2, L1, L2, mu);

	core()->setJD(jd - 5. / 1440.);
	core()->update(0);
	double x1, y1, d1, mu1;
	TransitBessel::computeElements(object, x1, y1, d1, tf1, tf2, L1, L2, mu1);

	core()->setJD(jd + 5. / 1440.);
	core()->update(0);
	double x2, y2, d2, mu2;
	TransitBessel::computeElements(object, x2, y2, d2, tf1, tf2, L1, L2, mu2);

	const double xdot = (x2 - x1) * 6.;
	const double ydot = (y2 - y1) * 6.;
	const double ddot = (d2 - d1) * 6.;
	double mudot = mu2 - mu1;
	if (mudot < 0.)
		mudot += 360.;
	mudot = mudot * 6. * M_PI_180;

	double theta = (mu + lon) * M_PI_180;
	theta = StelUtils::fmodpos(theta, 2. * M_PI);
	const double xi = rc * std::sin(theta);
	const double eta = rs * std::cos(d) - rc * std::sin(d) * std::cos(theta);
	const double zeta = rs * std::sin(d) + rc * std::cos(d) * std::cos(theta);
	const double xidot = mudot * rc * std::cos(theta);
	const double etadot = mudot * xi * std::sin(d) - zeta * ddot;
	const double u = x - xi;
	const double v = y - eta;
	const double udot = xdot - xidot;
	const double vdot = ydot - etadot;
	const double n2 = udot * udot + vdot * vdot;
	const double delta = (u * vdot - udot * v) / std::sqrt(n2);
	L1 -= zeta * tf1;
	L2 -= zeta * tf2;
	const double L = central ? L2 : L1;
	const double sfi = delta / L;
	const double ce = 1. - sfi * sfi;
	const double cfi = (ce > 0.) ? contact * std::sqrt(ce) : 0.;
	const double m = std::sqrt(u * u + v * v);

	result.magnitude = (L1 - m) / (L1 + L2);
	result.altitude = std::asin(rc * std::cos(d) * std::cos(theta) + rs * std::sin(d)) / M_PI_180;
	result.dt = (L * cfi / std::sqrt(n2)) - (u * udot + v * vdot) / n2;
	result.L1 = L1;
	result.L2 = L2;
	result.ce = ce;
	return result;
}

struct LunarContact
{
	double dt;
	double positionAngle;
	double axisDistance;
};

LunarContact lunarEclipseContacts(double jd, bool beforeMaximum, int eclipseType)
{
	LunarContact result;
	core()->setJD(jd);
	core()->update(0);
	double x, y, L1, L2, L3, latitude, longitude, L = 0.;
	LunarEclipseBessel::computeElements(x, y, L1, L2, L3, latitude, longitude);
	switch (eclipseType)
	{
		case 0: L = L1; break;
		case 1: L = L2; break;
		case 2: L = L3; break;
	}

	core()->setJD(jd - 5. / 1440.);
	core()->update(0);
	double x1, y1;
	LunarEclipseBessel::computeElements(x1, y1, L1, L2, L3, latitude, longitude);
	core()->setJD(jd + 5. / 1440.);
	core()->update(0);
	double x2, y2;
	LunarEclipseBessel::computeElements(x2, y2, L1, L2, L3, latitude, longitude);

	const double xdot = (x2 - x1) * 6.;
	const double ydot = (y2 - y1) * 6.;
	const double n2 = xdot * xdot + ydot * ydot;
	const double delta = (x * ydot - y * xdot) / std::sqrt(n2);
	const double semiDuration = std::sqrt((L * L - delta * delta) / n2);
	result.dt = -(x * xdot + y * ydot) / n2;
	if (beforeMaximum)
		result.dt -= semiDuration;
	else
		result.dt += semiDuration;

	result.positionAngle = StelUtils::fmodpos(std::atan2(x, y), 2. * M_PI);
	result.axisDistance = std::sqrt(x * x + y * y) * M_PI_180 / 3600.;
	core()->setJD(jd);
	core()->update(0);
	return result;
}

void generateSolar(int fromYear, int years)
{
	kSolarRows = QJsonArray();
	if (!onEarth())
		return;

	StelApp& app = StelApp::getInstance();
	StelCore* c = core();
	const SolarEclipseComputer ecliptor(c, &app.getLocaleMgr());
	const bool decimalDegrees = app.getFlagShowDecimalDegrees();

	const QString km = qc_("km", "distance");

	const bool wantTotal = settingBool("eclipse_filter/global_solar/total_enabled", true);
	const bool wantHybrid = settingBool("eclipse_filter/global_solar/hybrid_enabled", true);
	const bool wantAnnular = settingBool("eclipse_filter/global_solar/annular_enabled", true);
	const bool wantPartial = settingBool("eclipse_filter/global_solar/partial_enabled", true);

	const double currentJD = c->getJD();
	const bool saveTopocentric = c->getUseTopocentricCoordinates();
	double startJD = 0., stopJD = 0.;
	searchRange(fromYear, years, startJD, stopJD);
	const int elements = static_cast<int>((stopJD - startJD) / kSynodicMonth);

	const double tmp = (startJD - kApproxNewMoon - kSynodicMonth) / kSynodicMonth;
	const double initJD = kApproxNewMoon + int(tmp) * kSynodicMonth;

	c->setUseTopocentricCoordinates(false);
	c->update(0);
	for (int i = 0; i <= elements + 2; ++i)
	{
		double jd = initJD + kSynodicMonth * i;
		if (jd <= startJD)
			continue;

		jd = ecliptor.getJDofMinimumDistance(jd);
		c->setJD(jd);
		c->update(0);

		const EclipseBesselElements ep = calcSolarEclipseBessel();
		const double x = ep.x, y = ep.y, L2 = ep.L2;
		double gamma = std::sqrt(x * x + y * y);
		if (y < 0.)
			gamma = -gamma;
		double dRatio, latDeg, lngDeg, altitude, pathWidth, duration, magnitude;
		calcSolarEclipseData(jd, dRatio, latDeg, lngDeg, altitude, pathWidth, duration, magnitude);

		if (std::abs(gamma) > (1.5433 + L2))
			continue;

		bool noncentral = false;
		bool add = false;
		QString type;
		if (std::abs(gamma) > 0.9972 && std::abs(gamma) < (1.5433 + L2))
		{
			if (std::abs(gamma) < 0.9972 + std::abs(L2) && dRatio > 1.)
			{
				type = qc_("Total", "eclipse type");
				add = wantTotal;
			}
			else if (std::abs(gamma) < 0.9972 + std::abs(L2) && dRatio < 1.)
			{
				type = qc_("Annular", "eclipse type");
				add = wantAnnular;
			}
			else
			{
				type = qc_("Partial", "eclipse type");
				add = wantPartial;
			}
			noncentral = true;
		}
		else if (L2 < 0.)
		{
			type = qc_("Total", "eclipse type");
			add = wantTotal;
		}
		else if (L2 > 0.0047)
		{
			type = qc_("Annular", "eclipse type");
			add = wantAnnular;
		}
		else if (L2 > 0. && L2 < 0.0047)
		{
			if (L2 < (0.00464 * std::sqrt(1. - gamma * gamma)))
			{
				type = qc_("Hybrid", "eclipse type");
				add = wantHybrid;
			}
			else
			{
				type = qc_("Annular", "eclipse type");
				add = wantAnnular;
			}
		}
		if (!add)
			continue;

		QJsonObject row;
		row["jd"] = jd;
		row["when"] = asteriumFormatSimTime(jd, "yyyy-MM-dd HH:mm:ss");
		row["saros"] = QString::number(sarosNumber(jd, false));
		row["type"] = type;
		row["gamma"] = QString::number(gamma, 'f', 3);
		row["central"] = !noncentral;
		if (noncentral)
		{
			row["mag"] = QString::number(magnitude, 'f', 3);
			row["alt"] = QString("0%1").arg(QChar(0x00B0));
			row["width"] = kDash;
			row["duration"] = kDash;
		}
		else
		{
			row["mag"] = QString::number(dRatio, 'f', 3);
			row["alt"] = QString("%1%2").arg(QString::number(std::round(altitude)), QChar(0x00B0));
			row["width"] = QString("%1 %2").arg(QString::number(std::round(pathWidth)), km);
			row["duration"] = centralDuration(duration);
		}
		row["lat"] = StelUtils::decDegToLatitudeStr(latDeg, !decimalDegrees);
		row["lng"] = StelUtils::decDegToLongitudeStr(lngDeg, true, false, !decimalDegrees);
		kSolarRows.append(row);
	}

	c->setJD(currentJD);
	c->setUseTopocentricCoordinates(saveTopocentric);
	c->update(0);
}

void generateLocal(int fromYear, int years)
{
	kLocalRows = QJsonArray();
	if (!onEarth())
		return;

	StelCore* c = core();
	const SolarEclipseComputer ecliptor(c, &StelApp::getInstance().getLocaleMgr());
	const bool wantTotal = settingBool("eclipse_filter/local_solar/total_enabled", true);
	const bool wantAnnular = settingBool("eclipse_filter/local_solar/annular_enabled", true);
	const bool wantPartial = settingBool("eclipse_filter/local_solar/partial_enabled", true);

	const double currentJD = c->getJD();
	const bool saveTopocentric = c->getUseTopocentricCoordinates();
	c->setUseTopocentricCoordinates(false);
	double startJD = 0., stopJD = 0.;
	searchRange(fromYear, years, startJD, stopJD);
	const int elements = static_cast<int>((stopJD - startJD) / kSynodicMonth);
	const double tmp = (startJD - kApproxNewMoon - kSynodicMonth) / kSynodicMonth;
	const double initJD = kApproxNewMoon + int(tmp) * kSynodicMonth;

	for (int i = 0; i <= elements + 2; ++i)
	{
		double jd = initJD + kSynodicMonth * i;
		if (jd <= startJD)
			continue;

		jd = ecliptor.getJDofMinimumDistance(jd);
		c->setJD(jd);
		c->update(0);
		const EclipseBesselElements ep = calcSolarEclipseBessel();
		const double x = ep.x, y = ep.y, L2 = ep.L2;
		double gamma = std::sqrt(x * x + y * y);
		if (y < 0.)
			gamma *= -1.;
		if (std::abs(gamma) > (1.5433 + L2))
			continue;

		double magLocal = 0., altitudeMideclipse = 0.;
		double dt = 1.;
		int iteration = 0;
		LocalSolar data = localSolarEclipse(jd, 0, false);
		while (std::abs(dt) > 0.000001 && iteration < 20)
		{
			data = localSolarEclipse(jd, 0, false);
			dt = data.dt;
			jd += dt / 24.;
			iteration += 1;
			magLocal = data.magnitude;
			altitudeMideclipse = data.altitude;
		}
		if (magLocal <= 0.)
			continue;

		double altitudeFirst = 0., altitudeLast = 0.;
		double jd1 = jd, jdMax = jd, jd4 = jd;

		iteration = 0;
		data = localSolarEclipse(jd1, -1, false);
		dt = data.dt;
		while (std::abs(dt) > 0.000001 && iteration < 20)
		{
			data = localSolarEclipse(jd1, -1, false);
			dt = data.dt;
			jd1 += dt / 24.;
			iteration += 1;
			altitudeFirst = data.altitude;
		}

		iteration = 0;
		data = localSolarEclipse(jd4, 1, false);
		dt = data.dt;
		while (std::abs(dt) > 0.000001 && iteration < 20)
		{
			data = localSolarEclipse(jd4, 1, false);
			dt = data.dt;
			jd4 += dt / 24.;
			iteration += 1;
			altitudeLast = data.altitude;
		}

		if (altitudeMideclipse <= -.3 && altitudeFirst <= -.3 && altitudeLast <= -.3)
			continue;

		if (altitudeFirst < -.3 && altitudeLast > -.3)
		{
			for (int j = 0; j <= 5; ++j)
			{
				data = localSolarEclipse(jd - 5. / 1440., 0, false);
				const double alt1 = data.altitude + .3;
				data = localSolarEclipse(jd + 5. / 1440., 0, false);
				const double alt2 = data.altitude + .3;
				const double step = .006944444 * alt1 / (alt2 - alt1);
				jd = jd - 5. / 1440. - step;
				data = localSolarEclipse(jd, 0, false);
			}
			jd1 = jd;
			if (altitudeMideclipse <= -.3)
			{
				jdMax = jd;
				magLocal = data.magnitude;
			}
		}

		if (altitudeFirst > -.3 && altitudeLast < -.3)
		{
			for (int j = 0; j <= 5; ++j)
			{
				data = localSolarEclipse(jd - 5. / 1440., 0, false);
				const double alt1 = data.altitude + .3;
				data = localSolarEclipse(jd + 5. / 1440., 0, false);
				const double alt2 = data.altitude + .3;
				const double step = .006944444 * alt1 / (alt2 - alt1);
				jd = jd - 5. / 1440. - step;
				data = localSolarEclipse(jd, 0, false);
			}
			jd4 = jd;
			if (altitudeMideclipse <= -.3)
			{
				jdMax = jd;
				magLocal = data.magnitude;
			}
		}

		iteration = 0;
		double jd2 = jd;
		data = localSolarEclipse(jd2, -1, true);
		dt = data.dt;
		while (std::abs(dt) > 0.000001 && iteration < 20)
		{
			data = localSolarEclipse(jd2, -1, true);
			dt = data.dt;
			jd2 += dt / 24.;
			iteration += 1;
		}
		const double c2altitude = data.altitude;

		iteration = 0;
		double jd3 = jd;
		data = localSolarEclipse(jd3, 1, true);
		dt = data.dt;
		while (std::abs(dt) > 0.000001 && iteration < 20)
		{
			data = localSolarEclipse(jd3, 1, true);
			dt = data.dt;
			jd3 += dt / 24.;
			iteration += 1;
		}
		const double c3altitude = data.altitude;

		bool central = false;
		bool add = false;
		QString type;
		if (data.ce > 0. && (c2altitude > -.3 || c3altitude > -.3))
		{
			central = true;
			if (data.L2 < 0.)
			{
				type = qc_("Total", "eclipse type");
				qSwap(jd2, jd3);
				add = wantTotal;
			}
			else
			{
				type = qc_("Annular", "eclipse type");
				add = wantAnnular;
			}
		}
		else
		{
			type = qc_("Partial", "eclipse type");
			add = wantPartial;
		}
		if (!add)
			continue;

		QJsonObject row;
		row["jd"] = jdMax;
		row["when"] = asteriumFormatSimTime(jdMax, "yyyy-MM-dd");
		row["type"] = type;

		row["c1"] = (central && jd2 < jd1) ? kDash : asteriumFormatSimTime(jd1, "HH:mm:ss");
		row["c2"] = central ? asteriumFormatSimTime(jd2, "HH:mm:ss") : kDash;
		row["max"] = asteriumFormatSimTime(jdMax, "HH:mm:ss");
		row["mag"] = QString::number(magLocal, 'f', 3);
		row["c3"] = central ? asteriumFormatSimTime(jd3, "HH:mm:ss") : kDash;
		row["c4"] = (central && jd3 > jd4) ? kDash : asteriumFormatSimTime(jd4, "HH:mm:ss");
		row["duration"] = central ? centralDuration(std::abs(jd3 - jd2) * 1440.) : kDash;
		kLocalRows.append(row);
	}

	c->setJD(currentJD);
	c->setUseTopocentricCoordinates(saveTopocentric);
	c->update(0);
}

void generateLunar(int fromYear, int years)
{
	kLunarRows = QJsonArray();
	if (!onEarth())
		return;

	StelCore* c = core();
	const PlanetP moon = system()->getMoon();
	const bool wantTotal = settingBool("eclipse_filter/lunar/total_enabled", true);
	const bool wantPartial = settingBool("eclipse_filter/lunar/partial_enabled", true);
	const bool wantPenumbral = settingBool("eclipse_filter/lunar/penumbral_enabled", true);

	const double currentJD = c->getJD();
	const bool saveTopocentric = c->getUseTopocentricCoordinates();
	double startJD = 0., stopJD = 0.;
	searchRange(fromYear, years, startJD, stopJD);
	const int elements = static_cast<int>((stopJD - startJD) / kSynodicMonth);

	const double tmp = (startJD - kApproxNewMoon - (kSynodicMonth * 0.5)) / kSynodicMonth;
	const double initJD = kApproxNewMoon + int(tmp) * kSynodicMonth - (kSynodicMonth * 0.5);

	for (int i = 0; i <= elements + 2; ++i)
	{
		double jd = initJD + kSynodicMonth * i;
		if (jd <= startJD)
			continue;

		c->setJD(jd);
		c->setUseTopocentricCoordinates(false);
		c->update(0);

		double dt = 1.;
		int iteration = 0;
		while (std::abs(dt) > (0.1 / 86400.) && iteration < 20)
		{
			c->setJD(jd);
			c->update(0);
			double x, y, L1, L2, L3, latitude, longitude;
			LunarEclipseBessel::computeElements(x, y, L1, L2, L3, latitude, longitude);
			c->setJD(jd - 5. / 1440.);
			c->update(0);
			double x1, y1;
			LunarEclipseBessel::computeElements(x1, y1, L1, L2, L3, latitude, longitude);
			c->setJD(jd + 5. / 1440.);
			c->update(0);
			double x2, y2;
			LunarEclipseBessel::computeElements(x2, y2, L1, L2, L3, latitude, longitude);
			const double xdot = (x2 - x1) * 6.;
			const double ydot = (y2 - y1) * 6.;
			const double n2 = xdot * xdot + ydot * ydot;
			dt = -(x * xdot + y * ydot) / n2;
			jd += dt / 24.;
			iteration += 1;
		}
		c->setJD(jd);
		c->update(0);

		const double dist = moon->getEclipticPos().norm();
		const double mSD = std::atan(moon->getEquatorialRadius() / dist) * M_180_PI * 3600.;
		double x, y, L1, L2, L3, latitude, longitude;
		LunarEclipseBessel::computeElements(x, y, L1, L2, L3, latitude, longitude);
		const double m = std::sqrt(x * x + y * y);
		const double pMag = (L1 - m) / (2. * mSD);
		const double uMag = (L2 - m) / (2. * mSD);
		if (pMag <= 0.)
			continue;

		QString type;
		bool add = false;
		if (uMag >= 1.)
		{
			type = qc_("Total", "eclipse type");
			add = wantTotal;
		}
		else if (uMag > 0.)
		{
			type = qc_("Partial", "eclipse type");
			add = wantPartial;
		}
		else
		{
			type = qc_("Penumbral", "eclipse type");
			add = wantPenumbral;
		}
		if (!add)
			continue;

		double az = 0., alt = 0.;
		StelUtils::rectToSphe(&az, &alt, moon->getAltAzPosAuto(c));
		const double altitude = alt * M_180_PI;

		QString conditions, conditionsNote;
		if (altitude >= 45.)
		{
			conditions = qc_("Perfect", "visibility conditions");
			conditionsNote = ct_("Perfect visibility conditions for current location");
		}
		else if (altitude >= 30.)
		{
			conditions = qc_("Good", "visibility conditions");
			conditionsNote = ct_("Good visibility conditions for current location");
		}
		else
		{
			conditions = qc_("Bad", "visibility conditions");
			conditionsNote = ct_("Bad visibility conditions for current location");
		}

		if (uMag < 1.0 && pMag < 0.7)
		{
			conditions = qc_("Not obs.", "visibility conditions");
			conditionsNote = ct_("Not observable eclipse");
		}
		if (altitude < 0.)
		{
			conditions = qc_("Invisible", "visibility conditions");
			conditionsNote = ct_("The greatest eclipse is invisible in current location");
		}

		double gamma = m * 0.2725076 / mSD;
		if (y < 0.)
			gamma *= -1.;

		QJsonObject row;
		row["jd"] = jd;
		row["when"] = asteriumFormatSimTime(jd, "yyyy-MM-dd HH:mm:ss");
		row["saros"] = QString::number(sarosNumber(jd, true));
		row["type"] = type;
		row["gamma"] = QString::number(gamma, 'f', 3);
		row["pmag"] = QString::number(pMag, 'f', 3);
		row["umag"] = uMag < 0. ? kDash : QString::number(uMag, 'f', 3);
		row["umagValue"] = uMag;
		row["visibility"] = conditions;
		row["visibilityNote"] = conditionsNote;
		kLunarRows.append(row);
	}

	c->setJD(currentJD);
	c->setUseTopocentricCoordinates(saveTopocentric);
	c->update(0);
}

void generateTransits(int fromYear, int years)
{
	kTransitRows = QJsonArray();
	if (!onEarth())
		return;

	StelApp& app = StelApp::getInstance();
	StelCore* c = core();
	const bool decimalDegrees = app.getFlagShowDecimalDegrees();
	const double currentJD = c->getJD();
	const bool saveTopocentric = c->getUseTopocentricCoordinates();

	for (int p = 0; p < 2; ++p)
	{
		double startJD = 0., stopJD = 0.;
		searchRange(fromYear, years, startJD, stopJD);

		const double approxJD = (p == 0) ? 2451612.023 : 2451996.706;
		const double synodicPeriod = (p == 0) ? 115.8774771 : 583.921361;
		const QString name = (p == 0) ? QStringLiteral("Mercury") : QStringLiteral("Venus");
		const PlanetP object = system()->searchByEnglishName(name);
		if (object.isNull())
			continue;
		const QString planetStr = (p == 0) ? ct_("Mercury") : ct_("Venus");

		const int elements = static_cast<int>((stopJD - startJD) / synodicPeriod);
		const double tmp = (startJD - approxJD - synodicPeriod) / synodicPeriod;
		const double initJD = approxJD + int(tmp) * synodicPeriod;

		for (int i = 0; i <= elements + 2; ++i)
		{
			double jd = initJD + synodicPeriod * i;
			if (jd <= startJD)
				continue;

			c->setUseTopocentricCoordinates(false);
			c->update(0);

			double dt = 1.;
			int iteration = 0;
			while (std::abs(dt) > (0.1 / 86400.) && iteration < 20)
			{
				c->setJD(jd);
				c->update(0);
				double x, y, d, tf1, tf2, L1, L2, mu;
				TransitBessel::computeElements(object, x, y, d, tf1, tf2, L1, L2, mu);
				c->setJD(jd - 5. / 1440.);
				c->update(0);
				double x1, y1;
				TransitBessel::computeElements(object, x1, y1, d, tf1, tf2, L1, L2, mu);
				c->setJD(jd + 5. / 1440.);
				c->update(0);
				double x2, y2;
				TransitBessel::computeElements(object, x2, y2, d, tf1, tf2, L1, L2, mu);
				const double xdot = (((x - x1) * 12.) + ((x2 - x) * 12.)) / 2.;
				const double ydot = (((y - y1) * 12.) + ((y2 - y) * 12.)) / 2.;
				const double n2 = xdot * xdot + ydot * ydot;
				dt = -(x * xdot + y * ydot) / n2;
				jd += dt / 24.;
				++iteration;
			}
			c->setJD(jd);
			c->update(0);

			double x, y, d, tf1, tf2, L1, L2, mu;
			TransitBessel::computeElements(object, x, y, d, tf1, tf2, L1, L2, mu);
			const double gamma = std::sqrt(x * x + y * y);
			if (gamma > (0.9972 + L1) || jd > stopJD)
				continue;

			double altitudeMid = -1., altitude1 = -1., altitude2 = -1., altitude3 = -1., altitude4 = -1.;
			double jd1 = 0., jd2 = 0., jd3 = 0., jd4 = 0., jdc1 = 0., jdc4 = 0.;
			double az = 0.;
			Vec4d rts;

			dt = 1.;
			iteration = 0;
			LocalSolar data = localTransit(jd, 0, false, object, saveTopocentric);
			while (std::abs(dt) > 0.000001 && iteration < 20)
			{
				data = localTransit(jd, 0, false, object, saveTopocentric);
				dt = data.dt;
				jd += dt / 24.;
				++iteration;
			}
			const double jdMid = jd;
			const double magnitude = data.magnitude;
			c->setJD(jdMid);
			c->update(0);
			StelUtils::rectToSphe(&az, &altitudeMid, object->getAltAzPosAuto(c));

			if (magnitude > 0.)
			{
				iteration = 0;
				jd1 = jdMid;
				data = localTransit(jd1, -1, false, object, saveTopocentric);
				dt = data.dt;
				while (std::abs(dt) > 0.000001 && iteration < 20)
				{
					data = localTransit(jd1, -1, false, object, saveTopocentric);
					dt = data.dt;
					jd1 += dt / 24.;
					iteration += 1;
				}
				c->setJD(jd1);
				c->update(0);
				StelUtils::rectToSphe(&az, &altitude1, object->getAltAzPosAuto(c));

				iteration = 0;
				jd4 = jdMid;
				data = localTransit(jd4, 1, false, object, saveTopocentric);
				dt = data.dt;
				while (std::abs(dt) > 0.000001 && iteration < 20)
				{
					data = localTransit(jd4, 1, false, object, saveTopocentric);
					dt = data.dt;
					jd4 += dt / 24.;
					iteration += 1;
				}
				c->setJD(jd4);
				c->update(0);
				StelUtils::rectToSphe(&az, &altitude4, object->getAltAzPosAuto(c));
				jdc1 = jd1;
				jdc4 = jd4;

				iteration = 0;
				jd2 = jdMid;
				data = localTransit(jd2, -1, true, object, saveTopocentric);
				dt = data.dt;
				while (std::abs(dt) > 0.000001 && iteration < 20)
				{
					data = localTransit(jd2, -1, true, object, saveTopocentric);
					dt = data.dt;
					jd2 += dt / 24.;
					iteration += 1;
				}
				c->setJD(jd2);
				c->update(0);
				StelUtils::rectToSphe(&az, &altitude2, object->getAltAzPosAuto(c));

				iteration = 0;
				jd3 = jdMid;
				data = localTransit(jd3, 1, true, object, saveTopocentric);
				dt = data.dt;
				while (std::abs(dt) > 0.000001 && iteration < 20)
				{
					data = localTransit(jd3, 1, true, object, saveTopocentric);
					dt = data.dt;
					jd3 += dt / 24.;
					iteration += 1;
				}
				c->setJD(jd3);
				c->update(0);
				StelUtils::rectToSphe(&az, &altitude3, object->getAltAzPosAuto(c));

				if (saveTopocentric)
				{
					if (altitude1 < 0. && altitude4 > 0.)
					{
						for (int j = 0; j <= 5; ++j)
						{
							data = localTransit(jd - 5. / 1440., 0, false, object, true);
							const double alt1 = data.altitude + .3;
							data = localTransit(jd + 5. / 1440., 0, false, object, true);
							const double alt2 = data.altitude + .3;
							const double step = .006944444 * alt1 / (alt2 - alt1);
							jd = jd - 5. / 1440. - step;
							data = localTransit(jd, 0, false, object, true);
						}
						c->setJD(jd);
						c->update(0);
						rts = object->getRTSTime(c);
						jd = rts[0];
						if (jd < jd1)
						{
							c->setJD(jd + 1.);
							c->update(0);
							rts = object->getRTSTime(c);
						}
						else if (jd > jd4)
						{
							c->setJD(jd - 1.);
							c->update(0);
							rts = object->getRTSTime(c);
						}
						jdc1 = rts[0];
					}

					if (altitude1 > 0. && altitude4 < 0.)
					{
						for (int j = 0; j <= 5; ++j)
						{
							data = localTransit(jd - 5. / 1440., 0, false, object, true);
							const double alt1 = data.altitude + .3;
							data = localTransit(jd + 5. / 1440., 0, false, object, true);
							const double alt2 = data.altitude + .3;
							const double step = .006944444 * alt1 / (alt2 - alt1);
							jd = jd - 5. / 1440. - step;
							data = localTransit(jd, 0, false, object, true);
						}
						c->setJD(jd);
						c->update(0);
						rts = object->getRTSTime(c);
						jd = rts[2];
						if (jd < jd1)
						{
							c->setJD(jd + 1.);
							c->update(0);
							rts = object->getRTSTime(c);
						}
						else if (jd > jd4)
						{
							c->setJD(jd - 1.);
							c->update(0);
							rts = object->getRTSTime(c);
						}
						jdc4 = rts[2];
					}
				}
			}

			auto contact = [saveTopocentric](double when, double altitude)
			{
				const QString text = asteriumFormatSimTime(when, "HH:mm:ss");
				return (saveTopocentric && altitude < 0.) ? QString("(%1)").arg(text) : text;
			};

			QJsonObject row;
			row["jd"] = jdMid;
			row["when"] = asteriumFormatSimTime(jdMid, "yyyy-MM-dd HH:mm:ss");
			row["planet"] = planetStr;
			row["select"] = name;
			row["c1"] = magnitude > 0. ? contact(jd1, altitude1) : kDash;
			row["c2"] = data.ce <= 0. ? kDash : contact(jd2, altitude2);
			row["mid"] = magnitude > 0. ? contact(jdMid, altitudeMid) : kDash;
			row["c3"] = data.ce <= 0. ? kDash : contact(jd3, altitude3);
			row["c4"] = magnitude > 0. ? contact(jd4, altitude4) : kDash;

			c->setUseTopocentricCoordinates(saveTopocentric);
			c->setJD(jdMid);
			c->update(0);
			const double elongation = object->getElongation(c->getObserverHeliocentricEclipticPos());
			row["sep"] = decimalDegrees ? StelUtils::radToDecDegStr(elongation, 5, false, true)
			                            : StelUtils::radToDmsStr(elongation, true);

			row["duration"] = magnitude > 0. ? StelUtils::hoursToHmsStr((jd4 - jd1) * 24., true) : kDash;

			QString observable = kDash;
			if (saveTopocentric && magnitude > 0.)
			{
				if (altitude1 < 0. && altitude4 < 0.)
				{
					if (altitudeMid > 0.)
					{
						c->setJD(jdMid);
						c->update(0);
						rts = object->getRTSTime(c);
						if (rts[0] > jd1 && rts[2] < jd4)
							observable = StelUtils::hoursToHmsStr((rts[2] - rts[0]) * 24., true);
					}
				}
				else if (altitude1 > 0. && altitudeMid < 0. && altitude4 > 0.)
				{
					double total = 0.;
					c->setJD(jd1);
					c->update(0);
					rts = object->getRTSTime(c);
					if (rts[2] > jd1)
						total = (rts[2] - jd1) * 24.;
					c->setJD(jd4);
					c->update(0);
					rts = object->getRTSTime(c);
					if (jd4 > rts[0])
						total += (jd4 - rts[0]) * 24.;
					observable = StelUtils::hoursToHmsStr(total, true);
				}
				else
					observable = StelUtils::hoursToHmsStr((jdc4 - jdc1) * 24., true);
			}
			row["observable"] = observable;
			kTransitRows.append(row);
		}
	}

	c->setJD(currentJD);
	c->setUseTopocentricCoordinates(saveTopocentric);
	c->update(0);

	QList<QJsonValue> sorted;
	for (const QJsonValue& row : std::as_const(kTransitRows))
		sorted.append(row);
	std::sort(sorted.begin(), sorted.end(), [](const QJsonValue& a, const QJsonValue& b)
	          { return a.toObject().value("jd").toDouble() < b.toObject().value("jd").toDouble(); });
	kTransitRows = QJsonArray();
	for (const QJsonValue& row : std::as_const(sorted))
		kTransitRows.append(row);
}

QJsonArray solarCircumstances(double jdMid)
{
	QJsonArray rows;
	if (!onEarth())
		return rows;

	StelApp& app = StelApp::getInstance();
	StelCore* c = core();
	const SolarEclipseComputer ecliptor(c, &app.getLocaleMgr());
	const bool decimalDegrees = app.getFlagShowDecimalDegrees();
	const QString km = qc_("km", "distance");
	const double currentJD = c->getJD();
	const bool saveTopocentric = c->getUseTopocentricCoordinates();

	c->setJD(jdMid);
	c->update(0);
	const EclipseBesselElements ep = calcSolarEclipseBessel();
	const double x = ep.x, y = ep.y, L2 = ep.L2;
	const double gamma = std::sqrt(x * x + y * y);
	const bool nonCentral = (gamma > 0.9972) && (gamma < (1.5433 + L2));

	const QString labels[5] = {
		ct_("Eclipse begins; first contact with Earth"),
		ct_("Beginning of center line; central eclipse begins"),
		ct_("Greatest eclipse"),
		ct_("End of center line; central eclipse ends"),
		ct_("Eclipse ends; last contact with Earth"),
	};

	for (int i = 0; i < 5; ++i)
	{
		if ((i == 1 || i == 3) && nonCentral)
			continue;

		double jd = jdMid;
		double dRatio = 0., latDeg = 0., lngDeg = 0., altitude = 0.;
		double pathWidth = 0., duration = 0., magnitude = 0.;
		if (i == 0)
			jd = ecliptor.getJDofContact(jdMid, true, true, true, true);
		else if (i == 4)
			jd = ecliptor.getJDofContact(jdMid, false, true, true, true);
		else if (i == 1 || i == 3)
		{
			jd = ecliptor.getJDofContact(jdMid, i == 1, false, false, true);

			const int sign = (i == 1) ? -1 : 1;
			jd = int(jd) + (int((jd - int(jd)) * 86400.) + sign) / 86400.;
		}
		calcSolarEclipseData(jd, dRatio, latDeg, lngDeg, altitude, pathWidth, duration, magnitude);

		if (i == 1 || i == 3)
		{
			const double step = (i == 1) ? .1 / 86400. : -.1 / 86400.;
			int steps = 0;
			while (pathWidth < 0.0001 && steps < 20)
			{
				jd += step;
				calcSolarEclipseData(jd, dRatio, latDeg, lngDeg, altitude, pathWidth, duration, magnitude);
				steps += 1;
			}
			c->setJD(jd);
			c->update(0);
			const EclipseBesselElements contact = calcSolarEclipseBessel();
			const SolarEclipseComputer::GeoPoint where =
					ecliptor.getContactCoordinates(contact.x, contact.y, contact.d, contact.mu);
			latDeg = where.latitude;
			lngDeg = where.longitude;
		}

		QString type, widthStr = kDash, durationStr = kDash;
		if (i == 0 || i == 4)
			type = qc_("Partial", "eclipse type");
		else if (nonCentral)
		{
			if (std::abs(gamma) < 0.9972 + std::abs(L2) && dRatio > 1.)
				type = qc_("Total", "eclipse type");
			else if (std::abs(gamma) < 0.9972 + std::abs(L2) && dRatio < 1.)
				type = qc_("Annular", "eclipse type");
			else
				type = qc_("Partial", "eclipse type");
		}
		else
		{
			widthStr = QString("%1 %2").arg(QString::number(std::round(pathWidth)), km);
			durationStr = centralDuration(duration);
			type = duration <= 0. ? qc_("Total", "eclipse type") : qc_("Annular", "eclipse type");
		}

		QJsonObject row;
		row["label"] = labels[i];
		row["jd"] = jd;
		row["when"] = asteriumFormatSimTime(jd, "yyyy-MM-dd HH:mm:ss");
		row["lat"] = StelUtils::decDegToLatitudeStr(latDeg, !decimalDegrees);
		row["lng"] = StelUtils::decDegToLongitudeStr(lngDeg, true, false, !decimalDegrees);
		row["width"] = widthStr;
		row["duration"] = durationStr;
		row["type"] = type;
		rows.append(row);
	}

	c->setJD(currentJD);
	c->setUseTopocentricCoordinates(saveTopocentric);
	c->update(0);
	return rows;
}

QJsonArray lunarCircumstances(double jdMid, double uMag)
{
	QJsonArray rows;
	if (!onEarth())
		return rows;

	StelApp& app = StelApp::getInstance();
	StelCore* c = core();
	const PlanetP moon = system()->getMoon();
	const bool southAzimuth = app.getFlagSouthAzimuthUsage();
	const bool decimalDegrees = app.getFlagShowDecimalDegrees();
	const bool polarDistance = app.getFlagPolarDistanceUsage();
	const double currentJD = c->getJD();
	const bool saveTopocentric = c->getUseTopocentricCoordinates();

	const QString labels[7] = {
		ct_("Moon enters penumbra"), ct_("Moon enters umbra"), ct_("Total eclipse begins"),
		ct_("Maximum eclipse"), ct_("Total eclipse ends"), ct_("Moon leaves umbra"),
		ct_("Moon leaves penumbra"),
	};

	for (int i = 0; i < 7; ++i)
	{
		if ((i == 1 || i == 5) && uMag <= 0.)
			continue;
		if ((i == 2 || i == 4) && uMag < 1.)
			continue;

		double x, y, L1, L2, L3, latitude, longitude, positionAngle = 0., axisDistance = 0.;
		double jd = jdMid;
		if (i == 3)
		{
			const LunarContact data = lunarEclipseContacts(jd, true, 2);
			LunarEclipseBessel::computeElements(x, y, L1, L2, L3, latitude, longitude);
			positionAngle = data.positionAngle;
			axisDistance = data.axisDistance;
		}
		else
		{
			const bool beforeMaximum = i < 3;
			const int type = (i < 3) ? i : (6 - i);
			LunarEclipseBessel::iteration(jd, positionAngle, axisDistance, beforeMaximum, type);
			LunarEclipseBessel::computeElements(x, y, L1, L2, L3, latitude, longitude);
		}

		c->setJD(jd);
		c->setUseTopocentricCoordinates(saveTopocentric);
		c->update(0);
		double az = 0., alt = 0.;
		StelUtils::rectToSphe(&az, &alt, moon->getAltAzPosAuto(c));
		const QPair<QString, QString> coords = coordinates(moon->getAltAzPosAuto(c), true,
		                                                   southAzimuth, decimalDegrees, polarDistance);

		QJsonObject row;
		row["label"] = labels[i];
		row["jd"] = jd;
		row["when"] = asteriumFormatSimTime(jd, "yyyy-MM-dd HH:mm:ss");
		row["az"] = coords.first;
		row["alt"] = coords.second;

		row["visible"] = alt >= 0.;
		row["lat"] = StelUtils::decDegToLatitudeStr(latitude, !decimalDegrees);
		row["lng"] = StelUtils::decDegToLongitudeStr(longitude, true, false, !decimalDegrees);
		row["pa"] = decimalDegrees ? StelUtils::radToDecDegStr(positionAngle, 3, false, true)
		                           : StelUtils::radToDmsStr(positionAngle, true);
		row["dist"] = decimalDegrees ? StelUtils::radToDecDegStr(axisDistance, 5, false, true)
		                             : StelUtils::radToDmsStr(axisDistance, true);
		rows.append(row);
	}

	c->setJD(currentJD);
	c->setUseTopocentricCoordinates(saveTopocentric);
	c->update(0);
	return rows;
}

struct Filter { const char* tab; const char* key; const char* label; };
const Filter kFilters[] = {
	{ "solar", "eclipse_filter/global_solar/total_enabled",   "Total"     },
	{ "solar", "eclipse_filter/global_solar/hybrid_enabled",  "Hybrid"    },
	{ "solar", "eclipse_filter/global_solar/annular_enabled", "Annular"   },
	{ "solar", "eclipse_filter/global_solar/partial_enabled", "Partial"   },
	{ "local", "eclipse_filter/local_solar/total_enabled",    "Total"     },
	{ "local", "eclipse_filter/local_solar/annular_enabled",  "Annular"   },
	{ "local", "eclipse_filter/local_solar/partial_enabled",  "Partial"   },
	{ "lunar", "eclipse_filter/lunar/total_enabled",          "Total"     },
	{ "lunar", "eclipse_filter/lunar/partial_enabled",        "Partial"   },
	{ "lunar", "eclipse_filter/lunar/penumbral_enabled",      "Penumbral" },
};

QString tabNote(const QString& tab)
{
	if (tab == QLatin1String("solar"))
		return ct_("Notes: The quantity gamma is the minimum distance from the axis of lunar shadow cone to the center of Earth, in units of Earth’s equatorial radius. This distance is positive or negative, depending on whether the axis of the shadow cone passes north or south of the Earth's center. Path of solar eclipses during thousands of years in the past and future are not reliable due to uncertainty in ΔT which is caused by fluctuations in Earth's rotation.");
	if (tab == QLatin1String("local"))
		return ct_("Note: Local circumstances for eclipses during thousands of years in the past and future are not reliable due to uncertainty in ΔT which is caused by fluctuations in Earth's rotation.");
	if (tab == QLatin1String("lunar"))
		return ct_("Notes: The quantity gamma is the minimum distance from the center of the Moon to the axis of Earth’s umbral shadow cone, in units of Earth’s equatorial radius. This distance is positive or negative, depending on whether the Moon passes north or south of the shadow cone axis. Local circumstances for eclipses during thousands of years in the past and future are not reliable due to uncertainty in ΔT which is caused by fluctuations in Earth's rotation.");
	return ct_("Notes: Time in parentheses means the contact is invisible at current location. Transit times during thousands of years in the past and future are not reliable due to uncertainty in ΔT which is caused by fluctuations in Earth's rotation.");
}

QJsonArray& tableFor(const QString& tab)
{
	if (tab == QLatin1String("local"))
		return kLocalRows;
	if (tab == QLatin1String("lunar"))
		return kLunarRows;
	if (tab == QLatin1String("transits"))
		return kTransitRows;
	return kSolarRows;
}

int currentYear()
{
	int year = 0, month = 0, day = 0;
	StelUtils::getDateFromJulianDay(core()->getJD(), &year, &month, &day);
	return year;
}
}

bool answer(const QString& verb, const QString& arg, QJsonObject& out)
{
	const bool wantsTable = (verb == QLatin1String("eclipses"));
	const bool wantsSearch = (verb == QLatin1String("eclipses.generate"));
	const bool wantsCircumstances = (verb == QLatin1String("eclipses.circumstances"));
	if (!wantsTable && !wantsSearch && !wantsCircumstances)
		return false;

	if (!kFromYearSet)
	{
		kFromYear = currentYear();
		kFromYearSet = true;
	}

	if (wantsCircumstances)
	{
		const QStringList parts = arg.split('|');
		const QString tab = parts.value(0);
		const QJsonObject row = tableFor(tab).at(parts.value(1).toInt()).toObject();
		out["tab"] = tab;
		out["rows"] = (tab == QLatin1String("lunar"))
		              ? lunarCircumstances(row.value("jd").toDouble(), row.value("umagValue").toDouble())
		              : solarCircumstances(row.value("jd").toDouble());
		return true;
	}

	const QStringList parts = arg.split('|');
	const QString tab = parts.value(0).isEmpty() ? QStringLiteral("solar") : parts.value(0);
	int years = settingInt("eclipse_future_years", 10);
	if (wantsSearch && parts.size() >= 3)
	{
		kFromYear = qBound(-13000, parts.value(1).toInt(), 17000);
		years = qBound(1, parts.value(2).toInt(), 500);
		writeSetting(QStringLiteral("eclipse_future_years"), QString::number(years));
	}

	if (wantsSearch)
	{
		if (tab == QLatin1String("local"))
			generateLocal(kFromYear, years);
		else if (tab == QLatin1String("lunar"))
			generateLunar(kFromYear, years);
		else if (tab == QLatin1String("transits"))
			generateTransits(kFromYear, years);
		else
			generateSolar(kFromYear, years);
	}

	out["tab"] = tab;
	out["onEarth"] = onEarth();
	out["fromYear"] = kFromYear;
	out["years"] = years;
	out["note"] = tabNote(tab);
	out["rows"] = tableFor(tab);

	QJsonArray filters;
	for (const Filter& filter : kFilters)
	{
		if (tab != QLatin1String(filter.tab))
			continue;
		QJsonObject entry;
		entry["id"] = QLatin1String(filter.key);
		entry["name"] = qc_(QString::fromUtf8(filter.label), "eclipse type");
		entry["on"] = settingBool(filter.key, true);
		filters.append(entry);
	}
	out["filters"] = filters;
	return true;
}

bool perform(const QString& verb, const QString& arg)
{
	if (verb == QLatin1String("eclipses.set"))
	{
		const int split = arg.indexOf('=');
		if (split > 0)
			writeSetting(arg.left(split), arg.mid(split + 1));
		return true;
	}

	if (verb == QLatin1String("eclipses.clear"))
	{
		tableFor(arg) = QJsonArray();
		return true;
	}

	if (verb == QLatin1String("eclipses.goto"))
	{
		const QStringList parts = arg.split('|');
		if (parts.size() < 2)
			return true;
		const double jd = parts.value(0).toDouble();
		const QString name = parts.value(1);

		StelObjectMgr& objects = StelApp::getInstance().getStelObjectMgr();
		const QString type = QStringLiteral("Planet");
		if (!objects.findAndSelectI18n(name, type) && !objects.findAndSelect(name, type))
			return true;
		core()->setJD(jd);
		core()->update(0.);

		if (!objects.findAndSelectI18n(name, type))
			objects.findAndSelect(name, type);
		const QList<StelObjectP> selected = objects.getSelectedObject(type);
		StelMovementMgr* movement = core()->getMovementMgr();
		if (selected.isEmpty() || !movement)
			return true;

		if (selected.first()->getEnglishName() == core()->getCurrentLocation().planetName)
			objects.unSelect();
		else
		{
			asteriumMoveToSelected(movement, selected.first(), movement->getAutoMoveDuration());
		}
		return true;
	}

	return false;
}
}
}
