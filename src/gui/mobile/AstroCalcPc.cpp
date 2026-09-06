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
#include "StelApp.hpp"
#include "StelCore.hpp"
#include "StelModuleMgr.hpp"
#include "StelTranslator.hpp"
#include "StelUtils.hpp"

#include <QJsonArray>

#include <algorithm>
#include <cmath>

namespace AsteriumAstroCalc {
namespace Pc {
namespace {
QJsonObject kGraph;
QString kGraphFor;

QJsonArray allBodies(SolarSystem* solarSystem)
{
	QList<QPair<QString, QString>> items;
	if (solarSystem)
	{
		for (const PlanetP& planet : solarSystem->getAllPlanets())
		{
			if (planet->getPlanetType() == Planet::isObserver)
				continue;
			items.append(qMakePair(planet->getNameI18n(), planet->getEnglishName()));
		}
	}
	std::sort(items.begin(), items.end(),
	          [](const QPair<QString, QString>& a, const QPair<QString, QString>& b)
	          { return a.first.localeAwareCompare(b.first) < 0; });

	QJsonArray out;
	for (const auto& item : std::as_const(items))
	{
		QJsonObject entry;
		entry["id"] = item.second;
		entry["name"] = item.first;
		out.append(entry);
	}
	return out;
}

void dataRow(QJsonArray& into, const QString& label, const QString& value)
{
	QJsonObject entry;
	entry["label"] = label;
	entry["value"] = value;
	into.append(entry);
}

void computeData(StelCore* core, SolarSystem* solarSystem, const PlanetP& first, const PlanetP& second,
                 QJsonObject& out)
{
	if (!solarSystem || first.isNull() || second.isNull())
		return;
	const QString firstName = first->getEnglishName();
	const QString secondName = second->getEnglishName();
	const QString currentPlanet = core->getCurrentPlanet()->getEnglishName();

	const Vec3d posFCB = first->getJ2000EquatorialPos(core);
	const Vec3d posSCB = second->getJ2000EquatorialPos(core);
	const double distanceAu = (posFCB - posSCB).norm();
	const double distanceKm = AU * distanceAu;
	const QString degree = QChar(0x00B0);

	const QString km = qc_("km", "distance");

	const QString Mkm = qc_("M km", "distance");
	const bool useKM = (distanceAu < 0.1);
	const QString distAU = QString::number(distanceAu, 'f', 5);
	const QString distKM = useKM ? QString::number(distanceKm, 'f', 3)
	                             : QString::number(distanceKm / 1.0e6, 'f', 3);

	const double r = posFCB.angle(posSCB);
	unsigned int d = 0, m = 0;
	double s = 0.;
	bool sign = false;
	StelUtils::radToDms(r, sign, d, m, s);
	const double dd = r * M_180_PI;

	const double spcb1 = first->getSiderealPeriod();
	const double spcb2 = second->getSiderealPeriod();
	const QString parentFCBName = (firstName != QLatin1String("Sun"))
	                              ? first->getParent()->getEnglishName() : QString();
	const QString parentSCBName = (secondName != QLatin1String("Sun"))
	                              ? second->getParent()->getEnglishName() : QString();

	QJsonArray rows;
	dataRow(rows, ct_("Linear distance"),
	        QString("%1 %2 (%3 %4)").arg(distAU, qc_("AU", "distance, astronomical unit"),
	                                     distKM, useKM ? km : Mkm));

	QString angularDistance = kDash;
	if (firstName != currentPlanet && secondName != currentPlanet)
		angularDistance = QString("%1%2 %3' %4\" (%5%2)")
		                  .arg(d).arg(degree).arg(m).arg(s, 0, 'f', 2).arg(dd, 0, 'f', 5);
	dataRow(rows, ct_("Angular distance"), angularDistance);

	const QString day = qc_("day", "mean motion");

	const QString days = qc_("days", "duration");
	QString synodicPeriod = kDash;
	QString orbitalPeriodsRatio = kDash;

	if (spcb1 > 0. && spcb2 > 0. && parentFCBName == parentSCBName && firstName != QLatin1String("Sun"))
	{
		const double sp = qAbs(1 / (1 / spcb1 - 1 / spcb2));
		synodicPeriod = QString("%1 %2 (%3 a)").arg(QString::number(sp, 'f', 3), days,
		                                            QString::number(sp / 365.25, 'f', 5));
		double minp = spcb2;
		if (qAbs(spcb1) <= qAbs(spcb2))
			minp = spcb1;
		const int a = qRound(qAbs(spcb1 / minp) * 10);
		const int b = qRound(qAbs(spcb2 / minp) * 10);
		const int lcm = qAbs(a * b) / StelUtils::gcd(a, b);
		orbitalPeriodsRatio = QString("%1:%2").arg(lcm / a).arg(lcm / b);
	}
	dataRow(rows, ct_("Synodic period"), synodicPeriod);
	dataRow(rows, ct_("Orbital periods ratio"), orbitalPeriodsRatio);

	const auto meanMotion = [&](double period, const QString& name)
	{
		return (period > 0. && name != QLatin1String("Sun"))
		       ? QString("%1 %2/%3").arg(QString::number(360. / period, 'f', 5), degree, day)
		       : kDash;
	};
	dataRow(rows, QString("%1 (%2)").arg(ct_("Mean motion"), first->getNameI18n()),
	        meanMotion(spcb1, firstName));
	dataRow(rows, QString("%1 (%2)").arg(ct_("Mean motion"), second->getNameI18n()),
	        meanMotion(spcb2, secondName));

	const QString kms = qc_("km/s", "speed");
	const auto velocity = [&](const PlanetP& body)
	{
		const double v = body->getEclipticVelocity().norm();
		return v <= 0. ? kDash : QString("%1 %2").arg(QString::number(v * AU / 86400., 'f', 3), kms);
	};
	dataRow(rows, QString("%1 (%2)").arg(ct_("Orbital velocity"), first->getNameI18n()), velocity(first));
	dataRow(rows, QString("%1 (%2)").arg(ct_("Orbital velocity"), second->getNameI18n()), velocity(second));

	const double fcbs = 2.0 * AU * first->getEquatorialRadius();
	const double scbs = 2.0 * AU * second->getEquatorialRadius();
	const double sratio = fcbs / scbs;
	const int ss = (sratio < 1.0 ? 6 : 2);
	dataRow(rows, ct_("Equatorial radii ratio"),
	        QString("%1 (%2 %4 / %3 %4)").arg(QString::number(sratio, 'f', ss),
	                                          QString::number(fcbs, 'f', 1),
	                                          QString::number(scbs, 'f', 1), km));
	out["data"] = rows;
}

QJsonObject computeGraph(StelCore* core, const PlanetP& first, const PlanetP& second)
{
	QJsonObject out;
	QJsonArray au, deg;
	if (first.isNull() || second.isNull() || first == second)
	{
		out["au"] = au;
		out["deg"] = deg;
		return out;
	}

	const PlanetP currentPlanet = core->getCurrentPlanet();
	int limit = 40, step = 4;
	if (first->getParent() == currentPlanet || second->getParent() == currentPlanet)
	{
		limit = 80;
		step = 2;
	}

	const double currentJD = core->getJD();
	const double utcOffset = core->getUTCOffset(currentJD) / 24.;
	const double baseJD = std::floor(currentJD) + 0.5 - utcOffset;
	const bool angular = (first != currentPlanet && second != currentPlanet);

	for (int i = -limit; i <= limit; ++i)
	{
		const double jd = baseJD + i * step;
		core->setJD(jd);
		core->update(0.0);
		const Vec3d posFCB = first->getJ2000EquatorialPos(core);
		const Vec3d posSCB = second->getJ2000EquatorialPos(core);
		au.append((posFCB - posSCB).norm());
		if (angular)
			deg.append(posFCB.angle(posSCB) * M_180_PI);
	}
	core->setJD(currentJD);
	core->update(0.0);

	out["au"] = au;
	out["deg"] = deg;

	out["now"] = 0.5;

	QJsonArray ticks;
	for (int i = 0; i < 5; ++i)
	{
		QJsonObject tick;
		const double at = baseJD + (-limit + i * limit / 2.) * step;
		tick["pos"] = i / 4.;
		tick["label"] = asteriumFormatSimTime(at, "MMM d");
		ticks.append(tick);
	}
	out["ticks"] = ticks;
	return out;
}
}

bool answer(const QString& verb, const QString& arg, QJsonObject& out)
{
	Q_UNUSED(arg)
	const bool wantsGraph = (verb == QLatin1String("pc.graph"));
	if (verb != QLatin1String("pc") && !wantsGraph)
		return false;

	StelCore* core = StelApp::getInstance().getCore();
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	const QString firstId = setting("first_celestial_body", "Sun");
	const QString secondId = setting("second_celestial_body", "Earth");
	const PlanetP first = solarSystem ? solarSystem->searchByEnglishName(firstId) : PlanetP();
	const PlanetP second = solarSystem ? solarSystem->searchByEnglishName(secondId) : PlanetP();

	out["first"] = firstId;
	out["second"] = secondId;
	out["bodies"] = allBodies(solarSystem);
	out["title"] = ct_("Linear and angular distances between selected objects");
	out["auLabel"] = ct_("Distance, AU");
	out["degLabel"] = QString("%1, %2").arg(ct_("Distance"), QChar(0x00B0));
	computeData(core, solarSystem, first, second, out);

	if (wantsGraph)
	{
		const QString key = QString("%1|%2|%3").arg(firstId, secondId,
				QString::number(std::floor(core->getJD()), 'f', 0));
		if (key != kGraphFor)
		{
			kGraph = computeGraph(core, first, second);
			kGraphFor = key;
		}
		out["graph"] = kGraph;
	}
	return true;
}

bool perform(const QString& verb, const QString& arg)
{
	if (verb != QLatin1String("pc.set"))
		return false;
	writeSetting(arg.section('=', 0, 0), arg.section('=', 1));

	kGraphFor.clear();
	return true;
}
}
}
