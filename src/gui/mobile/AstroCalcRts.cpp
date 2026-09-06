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

#include "NebulaMgr.hpp"
#include "Planet.hpp"
#include "SolarSystem.hpp"
#include "StelApp.hpp"
#include "StelCore.hpp"
#include "StelModuleMgr.hpp"
#include "StelMovementMgr.hpp"
#include "StelObjectMgr.hpp"
#include "StelTranslator.hpp"
#include "StelUtils.hpp"

#include <QJsonArray>

namespace AsteriumAstroCalc {
namespace Rts {
namespace {
QJsonArray kRows;

QString kComputedFor;

QString celestialBodyName(const StelObjectP& obj)
{
	if (obj.isNull())
		return QString();
	QString name = obj->getNameI18n();
	if (name.isEmpty())
	{
		const QString type = obj->getType();
		if (type == QLatin1String("Nebula"))
		{
			NebulaMgr* nebulae = GETSTELMODULE(NebulaMgr);
			if (nebulae)
			{
				name = nebulae->getLatestSelectedDSODesignation();
				if (name.isEmpty())
					name = nebulae->getLatestSelectedDSODesignationWIC();
			}
		}
		if (type == QLatin1String("Star") || type == QLatin1String("Pulsar"))
			name = obj->getID();
	}
	if (obj->getType() == QLatin1String("Satellite"))
		return QString();
	return name;
}

StelObjectP selection()
{
	const QList<StelObjectP> selected = StelApp::getInstance().getStelObjectMgr().getSelectedObject();
	return selected.isEmpty() ? StelObjectP() : selected.first();
}

void generate(StelCore* core, const QString& startText)
{
	kRows = QJsonArray();
	kComputedFor.clear();

	const StelObjectP obj = selection();
	const QString name = celestialBodyName(obj);
	if (name.isEmpty())
		return;
	const QString englishName = obj->getEnglishName().isEmpty() ? name : obj->getEnglishName();

	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	if (!solarSystem)
		return;
	const bool decimalDegrees = StelApp::getInstance().getFlagShowDecimalDegrees();

	double currentStep = 1.;
	const PlanetP planet = core->getCurrentPlanet();
	const PlanetP sun = solarSystem->getSun();
	const PlanetP moon = solarSystem->getMoon();
	const PlanetP earth = solarSystem->getEarth();
	if (!planet.isNull() && planet->getPlanetType() != Planet::isObserver)
		currentStep = (planet == earth) ? 1. : planet->getMeanSolarDay();

	const double currentJD = core->getJD();

	const int year = startText.section('-', 0, 0).toInt();
	const int month = qBound(1, startText.section('-', 1, 1).toInt(), 12);
	const int months = qBound(1, settingInt("rts_duration_months", 1), 60);
	double startJD = 0.;
	StelUtils::getJDFromDate(&startJD, year, month, 1, 0, 0, 1);
	const double stopJD = startJD + months * 30.4375;
	const int elements = static_cast<int>((stopJD - startJD) / currentStep);

	for (int i = 0; i <= elements; ++i)
	{
		QString riseStr, setStr, transitStr, altStr, magStr;
		QString elongSStr = kDash, elongLStr = kDash;
		double jd = startJD + i * currentStep;

		const double jdNoon = jd + .5 - core->getUTCOffset(jd) * StelCore::JD_HOUR;
		core->setJD(jdNoon);
		core->update(0);
		const Vec4d rts = obj->getRTSTime(core);

		jd = rts[1];
		core->setJD(jd);
		core->update(0);

		double az = 0., alt = 0.;
		StelUtils::rectToSphe(&az, &alt, obj->getAltAzPosAuto(core));
		altStr = decimalDegrees ? StelUtils::radToDecDegStr(alt, 5, false, true)
		                        : StelUtils::radToDmsStr(alt, true);
		const Vec3d objPos = obj->getJ2000EquatorialPos(core);
		if (obj != sun)
			elongSStr = angleString(objPos.angle(sun->getJ2000EquatorialPos(core)), decimalDegrees);
		if (obj != moon && planet == earth)
			elongLStr = angleString(objPos.angle(moon->getJ2000EquatorialPos(core)), decimalDegrees);

		const float magnitude = obj->getVMagnitudeWithExtinction(core);
		magStr = (magnitude > 50.f || englishName.contains("marker", Qt::CaseInsensitive))
		         ? kDash : QString::number(magnitude, 'f', 2);

		int y = 0, m = 0, day = 0, currentDate = 0;
		const double utcShift = core->getUTCOffset(rts[1]) * StelCore::JD_HOUR;
		StelUtils::getDateFromJulianDay(jdNoon + utcShift, &y, &m, &currentDate);
		StelUtils::getDateFromJulianDay(rts[1] + utcShift, &y, &m, &day);

		const bool hasTransit = !(rts[3] == 20 || day != currentDate);
		if (!hasTransit)
			transitStr = altStr = magStr = elongSStr = elongLStr = kDash;
		else
			transitStr = asteriumFormatSimTime(jd, "yyyy-MM-dd HH:mm");

		const bool hasRise = !(rts[3] == 30 || rts[3] < 0 || rts[3] > 50);
		riseStr = hasRise ? asteriumFormatSimTime(rts[0], "yyyy-MM-dd HH:mm") : kDash;
		const bool hasSet = !(rts[3] == 40 || rts[3] < 0 || rts[3] > 50);
		setStr = hasSet ? asteriumFormatSimTime(rts[2], "yyyy-MM-dd HH:mm") : kDash;

		QJsonObject row;
		row["name"] = name;
		row["select"] = englishName;

		row["riseJD"] = hasRise ? rts[0] : 0.;
		row["transitJD"] = hasTransit ? jd : 0.;
		row["setJD"] = hasSet ? rts[2] : 0.;
		row["rise"] = riseStr;
		row["transit"] = transitStr;
		row["set"] = setStr;

		row["date"] = asteriumFormatSimTime(hasTransit ? jd : jdNoon, "yyyy-MM-dd");
		row["alt"] = altStr;
		row["mag"] = magStr;
		row["elongSun"] = elongSStr;
		row["elongMoon"] = elongLStr;
		kRows.append(row);
	}

	core->setJD(currentJD);
	core->update(0);
	kComputedFor = name;
}
}

bool answer(const QString& verb, const QString& arg, QJsonObject& out)
{
	if (verb != QLatin1String("rts") && verb != QLatin1String("rts.generate"))
		return false;

	StelCore* core = StelApp::getInstance().getCore();
	if (verb == QLatin1String("rts.generate"))
		generate(core, arg);

	const StelObjectP obj = selection();
	out["object"] = celestialBodyName(obj);
	out["months"] = qBound(1, settingInt("rts_duration_months", 1), 60);
	out["computedFor"] = kComputedFor;
	out["rows"] = kRows;

	out["note"] = ct_("Note: artificial satellites and unnamed stars are excluded from calculation");

	int year = 0, month = 0, day = 0;
	StelUtils::getDateFromJulianDay(core->getJD() + core->getUTCOffset(core->getJD()) / 24.,
	                                &year, &month, &day);
	out["year"] = year;
	out["month"] = month;
	return true;
}

bool perform(const QString& verb, const QString& arg)
{
	if (verb == QLatin1String("rts.set"))
	{
		writeSetting(arg.section('=', 0, 0), arg.section('=', 1));
		return true;
	}
	if (verb == QLatin1String("rts.clear"))
	{
		kRows = QJsonArray();
		kComputedFor.clear();
		return true;
	}
	if (verb != QLatin1String("rts.goto"))
		return false;

	const int index = arg.section(',', 0, 0).toInt();
	if (index < 0 || index >= kRows.size())
		return true;
	const QJsonObject row = kRows.at(index).toObject();
	const int which = arg.section(',', 1, 1).toInt();
	const char* const keys[] = { "riseJD", "transitJD", "setJD" };
	const double jd = row.value(QLatin1String(keys[qBound(0, which, 2)])).toDouble();
	if (jd == 0.)
		return true;

	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	StelObjectMgr& objects = app.getStelObjectMgr();
	const QString name = row.value("select").toString();
	if (!objects.findAndSelectI18n(name) && !objects.findAndSelect(name))
		return true;
	core->setJD(jd);
	core->update(0.);

	const QList<StelObjectP> selected = objects.getSelectedObject();
	StelMovementMgr* movement = core->getMovementMgr();
	if (selected.isEmpty() || !movement)
		return true;

	if (selected.first()->getEnglishName() == core->getCurrentLocation().planetName)
		objects.unSelect();
	else
	{
		asteriumMoveToSelected(movement, selected.first(), movement->getAutoMoveDuration());
	}
	return true;
}
}
}
