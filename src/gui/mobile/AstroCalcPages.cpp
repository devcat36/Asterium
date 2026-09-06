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
#include "StelLocation.hpp"
#include "StelObject.hpp"
#include "StelUtils.hpp"
#include "SolarSystem.hpp"
#include "Planet.hpp"

#include <QRegularExpression>
#include <QSettings>

#include <algorithm>
#include <cmath>

namespace AsteriumAstroCalc
{
const QString kDash = QChar(0x2014);

namespace
{
const char* const kSettingKeys[] = {
	"altvstime_moon", "altvstime_positive_limit", "altvstime_positive_only", "altvstime_sun",
	"angular_distance_limit", "celestial_category", "celestial_magnitude_limit",
	"custom_time_step", "custom_time_step_unit", "eclipse_filter", "eclipse_future_years",
	"ephemeris_celestial_body", "ephemeris_nakedeye_planets", "ephemeris_opposition_planet",
	"ephemeris_second_celestial_body", "ephemeris_sun_altitude", "ephemeris_sun_altitude_evening",
	"ephemeris_time_duration", "ephemeris_time_step", "ephemeris_time_unit",
	"first_celestial_body", "flag_ephemeris_ignore_date_test", "flag_hec_bright_comets",
	"flag_hec_minor_planets", "flag_horizontal_coordinates", "flag_phenomena_opposition",
	"flag_phenomena_perihelion", "flag_phenomena_quadratures", "graphs_celestial_body",
	"graphs_duration", "graphs_first_id", "graphs_second_id", "graphs_step",
	"hec_magnitude_limit", "me_positive_limit", "me_positive_only", "me_time",
	"phenomena_angular_separation", "phenomena_celestial_body", "phenomena_celestial_group",
	"second_celestial_body", "wut_altitude_min", "wut_angular_limit_flag",
	"wut_angular_limit_max", "wut_angular_limit_min", "wut_magnitude_limit",
	"wut_time_interval",
	"flag_ephemeris_horizontal_coordinates", "rts_duration_months", "phenomena_duration_months",
};

const char* const kSettingPrefixes[] = {
	"eclipse_filter/",
};

QSettings* config()
{
	return StelApp::getInstance().getSettings();
}
}

QPair<QString, QString> coordinates(const Vec3d& coord, bool horizontal, bool southAzimuth,
                                    bool decimalDegrees, bool polarDistance)
{
	double lng = 0., lat = 0.;
	StelUtils::rectToSphe(&lng, &lat, coord);
	if (horizontal)
	{
		lng = (southAzimuth ? 2. : 3.) * M_PI - lng;
		if (lng > M_PI * 2)
			lng -= M_PI * 2;
	}
	else if (polarDistance)
		lat = M_PI_2 - lat;

	const QString lngStr = decimalDegrees
	                       ? StelUtils::radToDecDegStr(lng, 5, false, true)
	                       : (horizontal ? StelUtils::radToDmsStr(lng, true) : StelUtils::radToHmsStr(lng));
	const QString latStr = decimalDegrees
	                       ? StelUtils::radToDecDegStr(lat, 5)
	                       : StelUtils::radToDmsStr(lat, true);
	return qMakePair(lngStr, latStr);
}

double culminationElevation(const StelObjectP& obj, StelCore* core)
{
	double ra = 0., dec = 0.;
	StelUtils::rectToSphe(&ra, &dec, obj->getEquinoxEquatorialPos(core));
	dec *= M_180_PI;
	const double lat = static_cast<double>(core->getCurrentLocation().getLatitude());
	const double elevation = (lat >= 0.)
	                         ? ((dec <= lat) ? (90. - lat + dec) : (90. + lat - dec))
	                         : ((dec >= lat) ? (90. + lat - dec) : (90. - lat + dec));
	return elevation * M_PI_180;
}

void transitStrings(const StelObjectP& obj, StelCore* core, double utcShift, bool decimalDegrees,
                    QString& transit, QString& elevation)
{
	transit = elevation = kDash;
	const Vec4d rts = obj->getRTSTime(core);

	if (rts[3] == 20)
		return;
	transit = StelUtils::hoursToHmsStr(StelUtils::getHoursFromJulianDay(rts[1] + utcShift), true);
	const double top = culminationElevation(obj, core);
	elevation = decimalDegrees ? StelUtils::radToDecDegStr(top, 5, false, true)
	                           : StelUtils::radToDmsPStr(top, 2);
}

QString angleString(double radians, bool decimalDegrees)
{
	QString text = decimalDegrees ? StelUtils::radToDecDegStr(radians, 5, false, true)
	                              : StelUtils::radToDmsStr(radians, true);
	return text.remove('+');
}

QJsonArray bodies(SolarSystem* solarSystem, StelCore* core)
{
	QJsonArray out;
	if (!solarSystem || !core)
		return out;

	QList<QPair<QString, QString>> items;
	const PlanetP here = core->getCurrentPlanet();
	for (const PlanetP& planet : solarSystem->getAllPlanets())
	{
		if (planet->getPlanetType() == Planet::isObserver)
			continue;
		if (!here.isNull() && planet->getEnglishName() == here->getEnglishName())
			continue;
		if (planet->getPlanetType() == Planet::isMoon && planet->getParent() != here)
			continue;
		items.append(qMakePair(planet->getNameI18n(), planet->getEnglishName()));
	}
	std::sort(items.begin(), items.end(),
	          [](const QPair<QString, QString>& a, const QPair<QString, QString>& b)
	          { return a.first.localeAwareCompare(b.first) < 0; });

	for (const auto& item : std::as_const(items))
	{
		QJsonObject entry;
		entry["id"] = item.second;
		entry["name"] = item.first;
		out.append(entry);
	}
	return out;
}

QString setting(const char* key, const QString& fallback)
{
	return config()->value(QStringLiteral("astrocalc/") + QLatin1String(key), fallback).toString();
}

bool settingBool(const char* key, bool fallback)
{
	return config()->value(QStringLiteral("astrocalc/") + QLatin1String(key), fallback).toBool();
}

double settingDouble(const char* key, double fallback)
{
	return config()->value(QStringLiteral("astrocalc/") + QLatin1String(key), fallback).toDouble();
}

int settingInt(const char* key, int fallback)
{
	return config()->value(QStringLiteral("astrocalc/") + QLatin1String(key), fallback).toInt();
}

bool writeSetting(const QString& key, const QString& value)
{
	for (const char* const name : kSettingKeys)
	{
		if (key != QLatin1String(name))
			continue;
		config()->setValue(QStringLiteral("astrocalc/") + key, value);
		return true;
	}
	for (const char* const prefix : kSettingPrefixes)
	{
		if (!key.startsWith(QLatin1String(prefix)))
			continue;
		config()->setValue(QStringLiteral("astrocalc/") + key, value);
		return true;
	}
	return false;
}

double parseLocalTime(const QString& text, StelCore* core)
{
	static const QRegularExpression pattern(
			QStringLiteral("^(-?\\d+)-(\\d+)-(\\d+)[ T](\\d+):(\\d+)$"));
	const QRegularExpressionMatch match = pattern.match(text.trimmed());
	if (!match.hasMatch())
		return core->getJD();

	double jd = 0.;
	if (!StelUtils::getJDFromDate(&jd, match.captured(1).toInt(), match.captured(2).toInt(),
	                              match.captured(3).toInt(), match.captured(4).toInt(),
	                              match.captured(5).toInt(), 0.f))
		return core->getJD();

	return jd - core->getUTCOffset(jd) / 24.;
}

bool answer(const QString& verb, const QString& arg, QJsonObject& out)
{
	return Rts::answer(verb, arg, out)
	    || Phenomena::answer(verb, arg, out)
	    || Graphs::answer(verb, arg, out)
	    || Wut::answer(verb, arg, out)
	    || Pc::answer(verb, arg, out)
	    || Eclipses::answer(verb, arg, out)
	    || Almanac::answer(verb, arg, out);
}

bool perform(const QString& verb, const QString& arg)
{
	return Rts::perform(verb, arg)
	    || Phenomena::perform(verb, arg)
	    || Graphs::perform(verb, arg)
	    || Wut::perform(verb, arg)
	    || Pc::perform(verb, arg)
	    || Eclipses::perform(verb, arg)
	    || Almanac::perform(verb, arg);
}
}
