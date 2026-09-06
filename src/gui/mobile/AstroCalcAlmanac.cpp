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

#include "Planet.hpp"
#include "SolarSystem.hpp"
#include "SpecificTimeMgr.hpp"
#include "StelApp.hpp"
#include "StelCore.hpp"
#include "StelModuleMgr.hpp"
#include "StelLocaleMgr.hpp"
#include "StelTranslator.hpp"
#include "StelUtils.hpp"

#include <QJsonArray>
#include <QSettings>

#include <cmath>

namespace AsteriumAstroCalc {
namespace Almanac {
namespace {
struct AlmanacKey { const char* name; double min; double max; };
const AlmanacKey kKeys[] = {
	{ "custom_minutes",       1.,   120. },
	{ "custom_sun_altitude",  -90., 90.  },
	{ "custom_moon_altitude", -90., 90.  },
};

QSettings* config()
{
	return StelApp::getInstance().getSettings();
}

double astroSetting(const char* key, double fallback)
{
	return config()->value(QStringLiteral("astro/") + QLatin1String(key), fallback).toDouble();
}

void row(QJsonArray& into, const QString& label, const QString& value, double jd = 0.)
{
	QJsonObject entry;
	entry["label"] = label;
	entry["value"] = value;
	entry["jd"] = jd;
	into.append(entry);
}

void section(QJsonArray& into, const QString& title)
{
	QJsonObject entry;
	entry["section"] = title;
	into.append(entry);
}

QString clock(double jd)
{
	return asteriumFormatSimTime(jd, "HH:mm:ss");
}

QString duration(double hours)
{
	return StelUtils::hoursToHmsStr(hours, true);
}

void seasons(StelCore* core, QJsonObject& out)
{
	SpecificTimeMgr* specMgr = GETSTELMODULE(SpecificTimeMgr);
	if (!specMgr)
		return;

	const double jd = core->getJD() + core->getUTCOffset(core->getJD()) / 24.;
	int year = 0, month = 0, day = 0;
	StelUtils::getDateFromJulianDay(jd, &year, &month, &day);
	double jdFirstDay = 0., jdLastDay = 0.;
	StelUtils::getJDFromDate(&jdFirstDay, year, 1, 1, 0, 0, 0);
	StelUtils::getJDFromDate(&jdLastDay, year, 12, 31, 24, 0, 0);

	const double marchEquinox = specMgr->getEquinox(year, SpecificTimeMgr::Equinox::March);
	const double juneSolstice = specMgr->getSolstice(year, SpecificTimeMgr::Solstice::June);
	const double septemberEquinox = specMgr->getEquinox(year, SpecificTimeMgr::Equinox::September);
	const double decemberSolstice = specMgr->getSolstice(year, SpecificTimeMgr::Solstice::December);
	const QString days = qc_("days", "duration");

	const bool north = core->getCurrentLocation().getLatitude() >= 0.f;

	const QString spring = qc_("Spring", "season");

	const QString summer = qc_("Summer", "season");

	const QString fall = qc_("Fall", "season");

	const QString winter = qc_("Winter", "season");

	struct Season { QString name; QString event; double jd; double length; };
	const Season list[] = {
		{ north ? spring : fall,   ct_("March equinox"),      marchEquinox,     juneSolstice - marchEquinox },
		{ north ? summer : winter, ct_("June solstice"),      juneSolstice,     septemberEquinox - juneSolstice },
		{ north ? fall : spring,   ct_("September equinox"),  septemberEquinox, decemberSolstice - septemberEquinox },
		{ north ? winter : summer, ct_("December solstice"),  decemberSolstice,
		  (marchEquinox - jdFirstDay) + (jdLastDay - decemberSolstice) },
	};

	QJsonArray entries;
	for (const Season& season : list)
	{
		QJsonObject entry;
		entry["season"] = season.name;
		entry["event"] = season.event;
		entry["jd"] = season.jd;
		entry["julian"] = QString::number(season.jd, 'f', 5);
		entry["local"] = asteriumFormatSimTime(season.jd, "yyyy-MM-dd HH:mm:ss");
		entry["duration"] = QString("%1 %2").arg(QString::number(season.length, 'f', 2), days);
		entries.append(entry);
	}
	out["seasons"] = entries;
	out["year"] = year;
	out["yearDuration"] = QString("%1 %2").arg(QString::number(jdLastDay - jdFirstDay), days);
}

void today(StelCore* core, QJsonObject& out)
{
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	if (!solarSystem)
		return;
	out["onEarth"] = (core->getCurrentPlanet() == solarSystem->getEarth());
	if (!out["onEarth"].toBool())
		return;

	const double jd = core->getJD();
	const double utcOffsetHrs = core->getUTCOffset(jd);
	const PlanetP sun = solarSystem->getSun();
	const PlanetP moon = solarSystem->getMoon();
	const double minutesJD = astroSetting("custom_minutes", 60.) / (24. * 60.);
	const double customSunAltitude = astroSetting("custom_sun_altitude", -7.);
	const double customMoonAltitude = astroSetting("custom_moon_altitude", 18.);

	const Vec4d day = sun->getRTSTime(core, 0.);
	const Vec4d civil = sun->getRTSTime(core, -6.);
	const Vec4d nautical = sun->getRTSTime(core, -12.);
	const Vec4d astronomical = sun->getRTSTime(core, -18.);
	const Vec4d customSun = sun->getRTSTime(core, customSunAltitude);
	const Vec4d moonRts = moon->getRTSTime(core, 0.);
	const Vec4d customMoon = moon->getRTSTime(core, customMoonAltitude);

	QJsonArray rows;

	const auto twilightDuration = [](const Vec4d& outer, const Vec4d& inner)
	{
		double evening = (inner[2] - outer[2]) * 24.;
		if (evening > 0.)
			evening -= 24.;
		double morning = (outer[0] - inner[0]) * 24.;
		if (morning > 0.)
			morning -= 24.;
		return std::abs(evening) + std::abs(morning);
	};

	section(rows, ct_("Twilight"));
	const bool hasAstronomical = (astronomical[3] == 0.);
	const bool hasNautical = (nautical[3] == 0.);
	const bool hasCivil = (civil[3] == 0.);

	row(rows, QString("%1 - %2").arg(qc_("Astronomical", "twilight"), ct_("Dawn")),
	    hasAstronomical ? clock(astronomical[0]) : kDash, hasAstronomical ? astronomical[0] : 0.);
	row(rows, QString("%1 - %2").arg(qc_("Astronomical", "twilight"), ct_("Dusk")),
	    hasAstronomical ? clock(astronomical[2]) : kDash, hasAstronomical ? astronomical[2] : 0.);
	row(rows, QString("%1 - %2").arg(qc_("Astronomical", "twilight"), ct_("Duration")),
	    duration(hasAstronomical ? twilightDuration(astronomical, nautical) : 0.));

	row(rows, QString("%1 - %2").arg(qc_("Nautical", "twilight"), ct_("Dawn")),
	    hasNautical ? clock(nautical[0]) : kDash, hasNautical ? nautical[0] : 0.);
	row(rows, QString("%1 - %2").arg(qc_("Nautical", "twilight"), ct_("Dusk")),
	    hasNautical ? clock(nautical[2]) : kDash, hasNautical ? nautical[2] : 0.);
	row(rows, QString("%1 - %2").arg(qc_("Nautical", "twilight"), ct_("Duration")),
	    duration(hasNautical ? twilightDuration(nautical, civil) : 0.));

	row(rows, QString("%1 - %2").arg(qc_("Civil", "twilight"), ct_("Dawn")),
	    hasCivil ? clock(civil[0]) : kDash, hasCivil ? civil[0] : 0.);
	row(rows, QString("%1 - %2").arg(qc_("Civil", "twilight"), ct_("Dusk")),
	    hasCivil ? clock(civil[2]) : kDash, hasCivil ? civil[2] : 0.);
	row(rows, QString("%1 - %2").arg(qc_("Civil", "twilight"), ct_("Duration")),
	    duration(hasCivil ? twilightDuration(civil, day) : 0.));

	section(rows, ct_("Sun"));
	const bool hasDay = (day[3] == 0.);

	const double dayHours = hasDay ? std::abs(day[2] - day[0]) * 24. : (day[3] > 99. ? 24. : 0.);
	row(rows, ct_("Rise"), hasDay ? clock(day[0]) : kDash, hasDay ? day[0] : 0.);
	row(rows, ct_("Set"), hasDay ? clock(day[2]) : kDash, hasDay ? day[2] : 0.);
	row(rows, ct_("Duration"), duration(dayHours));

	double nightHours = 0.;
	if (hasAstronomical)
	{
		nightHours = (astronomical[2] - astronomical[0]) * 24.;
		nightHours = (nightHours < 0.) ? std::abs(nightHours) : 24. - nightHours;
	}
	else if (day[3] < -99.)
		nightHours = 24.;
	row(rows, ct_("Astronomical night"), duration(nightHours));

	section(rows, ct_("Moon"));

	const bool hasMoonrise = !(moonRts[3] == 30 || moonRts[3] < 0 || moonRts[3] > 50);
	const bool hasMoonset = !(moonRts[3] == 40 || moonRts[3] < 0 || moonRts[3] > 50);
	row(rows, ct_("Rise"), hasMoonrise ? clock(moonRts[0]) : kDash, hasMoonrise ? moonRts[0] : 0.);
	row(rows, ct_("Set"), hasMoonset ? clock(moonRts[2]) : kDash, hasMoonset ? moonRts[2] : 0.);

	section(rows, ct_("Custom"));
	const int minutes = qRound(astroSetting("custom_minutes", 60.));

	const QString m = qc_("m", "duration, suffix");
	row(rows, QString("%1%2 %3").arg(QString::number(minutes), m, ct_("before sunrise")),
	    hasDay ? clock(day[0] - minutesJD) : kDash, hasDay ? day[0] - minutesJD : 0.);
	row(rows, QString("%1%2 %3").arg(QString::number(minutes), m, ct_("after sunset")),
	    hasDay ? clock(day[2] + minutesJD) : kDash, hasDay ? day[2] + minutesJD : 0.);

	const bool hasCustomSun = (customSun[3] == 0.);
	const QString sunAt = QString("%1 %2°").arg(ct_("Sun at"), QString::number(customSunAltitude, 'f', 1));
	row(rows, QString("%1 - %2").arg(sunAt, ct_("Rise")),
	    hasCustomSun ? clock(customSun[0]) : kDash, hasCustomSun ? customSun[0] : 0.);
	row(rows, QString("%1 - %2").arg(sunAt, ct_("Set")),
	    hasCustomSun ? clock(customSun[2]) : kDash, hasCustomSun ? customSun[2] : 0.);

	const bool hasCustomMoonrise = !(customMoon[3] == 30 || customMoon[3] < 0 || customMoon[3] > 50);
	const bool hasCustomMoonset = !(customMoon[3] == 40 || customMoon[3] < 0 || customMoon[3] > 50);
	const QString moonAt = QString("%1 %2°").arg(ct_("Moon at"), QString::number(customMoonAltitude, 'f', 1));
	row(rows, QString("%1 - %2").arg(moonAt, ct_("Rise")),
	    hasCustomMoonrise ? clock(customMoon[0]) : kDash, hasCustomMoonrise ? customMoon[0] : 0.);
	row(rows, QString("%1 - %2").arg(moonAt, ct_("Set")),
	    hasCustomMoonset ? clock(customMoon[2]) : kDash, hasCustomMoonset ? customMoon[2] : 0.);

	out["rows"] = rows;
	out["today"] = QString("%1 (%2)").arg(ct_("Today"),
			StelApp::getInstance().getLocaleMgr().getPrintableDateLocal(jd, utcOffsetHrs));
}
}

bool answer(const QString& verb, const QString& arg, QJsonObject& out)
{
	Q_UNUSED(arg)
	if (verb != QLatin1String("almanac"))
		return false;

	StelCore* core = StelApp::getInstance().getCore();
	out["onEarth"] = false;
	seasons(core, out);
	today(core, out);
	out["minutes"] = qRound(astroSetting("custom_minutes", 60.));
	out["sunAltitude"] = astroSetting("custom_sun_altitude", -7.);
	out["moonAltitude"] = astroSetting("custom_moon_altitude", 18.);
	return true;
}

bool perform(const QString& verb, const QString& arg)
{
	if (verb == QLatin1String("almanac.set"))
	{
		const QString key = arg.section('=', 0, 0);
		const double value = arg.section('=', 1).toDouble();
		for (const AlmanacKey& known : kKeys)
		{
			if (key != QLatin1String(known.name))
				continue;
			const double clamped = qBound(known.min, value, known.max);

			if (key == QLatin1String("custom_minutes"))
				config()->setValue(QStringLiteral("astro/") + key, qRound(clamped));
			else
				config()->setValue(QStringLiteral("astro/") + key, clamped);
			break;
		}
		return true;
	}
	if (verb == QLatin1String("almanac.goto"))
	{
		bool ok = false;
		const double jd = arg.toDouble(&ok);
		if (ok && jd != 0.)
			StelApp::getInstance().getCore()->setJD(jd);
		return true;
	}
	if (verb == QLatin1String("almanac.day"))
	{
		StelApp::getInstance().getCore()->addSolarDays(arg.toDouble());
		return true;
	}
	if (verb != QLatin1String("almanac.season"))
		return false;

	SpecificTimeMgr* specMgr = GETSTELMODULE(SpecificTimeMgr);
	if (!specMgr)
		return true;
	const int which = arg.section(',', 0, 0).toInt();
	const int delta = arg.section(',', 1, 1).toInt();
	switch (which)
	{
		case 0:
			if (delta < 0)      specMgr->previousMarchEquinox();
			else if (delta > 0) specMgr->nextMarchEquinox();
			else                specMgr->currentMarchEquinox();
			break;
		case 1:
			if (delta < 0)      specMgr->previousJuneSolstice();
			else if (delta > 0) specMgr->nextJuneSolstice();
			else                specMgr->currentJuneSolstice();
			break;
		case 2:
			if (delta < 0)      specMgr->previousSeptemberEquinox();
			else if (delta > 0) specMgr->nextSeptemberEquinox();
			else                specMgr->currentSeptemberEquinox();
			break;
		case 3:
			if (delta < 0)      specMgr->previousDecemberSolstice();
			else if (delta > 0) specMgr->nextDecemberSolstice();
			else                specMgr->currentDecemberSolstice();
			break;
		default:
			break;
	}
	return true;
}
}
}
