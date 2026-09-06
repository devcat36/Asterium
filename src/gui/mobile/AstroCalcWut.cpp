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

#include "Nebula.hpp"
#include "NebulaMgr.hpp"
#include "Planet.hpp"
#include "SolarSystem.hpp"
#include "StarMgr.hpp"
#include "StelApp.hpp"
#include "StelCore.hpp"
#include "StelLocation.hpp"
#include "StelModuleMgr.hpp"
#include "StelMovementMgr.hpp"
#include "StelObject.hpp"
#include "StelObjectMgr.hpp"
#include "StelPropertyMgr.hpp"
#include "StelTranslator.hpp"
#include "StelUtils.hpp"

#ifdef USE_STATIC_PLUGIN_QUASARS
#include "../plugins/Quasars/src/Quasars.hpp"
#endif
#ifdef USE_STATIC_PLUGIN_PULSARS
#include "../plugins/Pulsars/src/Pulsars.hpp"
#endif
#ifdef USE_STATIC_PLUGIN_EXOPLANETS
#include "../plugins/Exoplanets/src/Exoplanets.hpp"
#endif
#ifdef USE_STATIC_PLUGIN_NOVAE
#include "../plugins/Novae/src/Novae.hpp"
#endif
#ifdef USE_STATIC_PLUGIN_SUPERNOVAE
#include "../plugins/Supernovae/src/Supernovae.hpp"
#endif

#include <QJsonArray>
#include <QList>
#include <QMap>
#include <QPair>
#include <QSet>
#include <QString>

#include <algorithm>
#include <cmath>

namespace AsteriumAstroCalc {
namespace Wut {
namespace {
enum Category
{
	EWPlanets                            =  0,
	EWBrightStars                        =  1,
	EWBrightNebulae                      =  2,
	EWDarkNebulae                        =  3,
	EWGalaxies                           =  4,
	EWOpenStarClusters                   =  5,
	EWAsteroids                          =  6,
	EWComets                             =  7,
	EWPlutinos                           =  8,
	EWDwarfPlanets                       =  9,
	EWCubewanos                          = 10,
	EWScatteredDiscObjects               = 11,
	EWOortCloudObjects                   = 12,
	EWSednoids                           = 13,
	EWPlanetaryNebulae                   = 14,
	EWBrightDoubleStars                  = 15,
	EWBrightVariableStars                = 16,
	EWBrightStarsWithHighProperMotion    = 17,
	EWSymbioticStars                     = 18,
	EWEmissionLineStars                  = 19,
	EWSupernovaeCandidates               = 20,
	EWSupernovaeRemnantCandidates        = 21,
	EWSupernovaeRemnants                 = 22,
	EWClustersOfGalaxies                 = 23,
	EWInterstellarObjects                = 24,
	EWGlobularStarClusters               = 25,
	EWRegionsOfTheSky                    = 26,
	EWActiveGalaxies                     = 27,
	EWPulsars                            = 28,
	EWExoplanetarySystems                = 29,
	EWBrightNovaStars                    = 30,
	EWBrightSupernovaStars               = 31,
	EWInteractingGalaxies                = 32,
	EWDeepSkyObjects                     = 33,
	EWMessierObjects                     = 34,
	EWNGCICObjects                       = 35,
	EWCaldwellObjects                    = 36,
	EWHerschel400Objects                 = 37,
	EWAlgolTypeVariableStars             = 38,
	EWClassicalCepheidsTypeVariableStars = 39,
	EWCarbonStars                        = 40,
	EWBariumStars                        = 41,
};

QJsonArray kRows;
QString kWhen;

int kCategory = EWPlanets;

struct Columns
{
	bool opacity;
	bool separation;
	bool magnitude;
	bool size;
	bool angularLimits;
};

Columns columnsFor(int category)
{
	Columns columns = { false, false, true, true, true };
	switch (category)
	{
		case EWBrightStars:
		case EWCarbonStars:
		case EWBariumStars:
		case EWBrightVariableStars:
		case EWAlgolTypeVariableStars:
		case EWClassicalCepheidsTypeVariableStars:
		case EWBrightStarsWithHighProperMotion:
		case EWSymbioticStars:
		case EWEmissionLineStars:
		case EWSupernovaeCandidates:
		case EWActiveGalaxies:
		case EWPulsars:
		case EWExoplanetarySystems:
		case EWBrightNovaStars:
		case EWBrightSupernovaStars:
			columns.angularLimits = false;
			break;
		default:
			break;
	}
	if (category == EWDarkNebulae)
		columns.opacity = true;
	if (category == EWBrightDoubleStars)
		columns.separation = true;
	if (category == EWRegionsOfTheSky)
		columns.magnitude = false;
	if (category == EWPulsars)
		columns.magnitude = false;

	columns.size = columns.angularLimits
	               && category != EWComets && category != EWRegionsOfTheSky;
	return columns;
}

QList<QPair<QString, int>> categories()
{
	StelModuleMgr& moduleMgr = StelApp::getInstance().getModuleMgr();
	StelPropertyMgr* properties = StelApp::getInstance().getStelPropertyManager();

	QList<QPair<QString, int>> items = {
		{ ct_("Planets"),                              EWPlanets },
		{ ct_("Bright stars"),                         EWBrightStars },
		{ ct_("Bright nebulae"),                       EWBrightNebulae },
		{ ct_("Dark nebulae"),                         EWDarkNebulae },
		{ ct_("Galaxies"),                             EWGalaxies },
		{ ct_("Open star clusters"),                   EWOpenStarClusters },
		{ ct_("Asteroids"),                            EWAsteroids },
		{ ct_("Comets"),                               EWComets },
		{ ct_("Plutinos"),                             EWPlutinos },
		{ ct_("Dwarf planets"),                        EWDwarfPlanets },
		{ ct_("Cubewanos"),                            EWCubewanos },
		{ ct_("Scattered disc objects"),               EWScatteredDiscObjects },
		{ ct_("Oort cloud objects"),                   EWOortCloudObjects },
		{ ct_("Sednoids"),                             EWSednoids },
		{ ct_("Planetary nebulae"),                    EWPlanetaryNebulae },
		{ ct_("Bright double stars"),                  EWBrightDoubleStars },
		{ ct_("Bright variable stars"),                EWBrightVariableStars },
		{ ct_("Bright stars with high proper motion"), EWBrightStarsWithHighProperMotion },
		{ ct_("Symbiotic stars"),                      EWSymbioticStars },
		{ ct_("Emission-line stars"),                  EWEmissionLineStars },
		{ ct_("Supernova candidates"),                 EWSupernovaeCandidates },
		{ ct_("Supernova remnant candidates"),         EWSupernovaeRemnantCandidates },
		{ ct_("Supernova remnants"),                   EWSupernovaeRemnants },
		{ ct_("Clusters of galaxies"),                 EWClustersOfGalaxies },
		{ ct_("Interstellar objects"),                 EWInterstellarObjects },
		{ ct_("Globular star clusters"),               EWGlobularStarClusters },
		{ ct_("Regions of the sky"),                   EWRegionsOfTheSky },
		{ ct_("Active galaxies"),                      EWActiveGalaxies },
		{ ct_("Interacting galaxies"),                 EWInteractingGalaxies },
		{ ct_("Deep-sky objects"),                     EWDeepSkyObjects },
		{ ct_("Messier objects"),                      EWMessierObjects },
		{ ct_("NGC/IC objects"),                       EWNGCICObjects },
		{ ct_("Caldwell objects"),                     EWCaldwellObjects },
		{ ct_("Herschel 400 objects"),                 EWHerschel400Objects },
		{ ct_("Algol-type eclipsing systems"),         EWAlgolTypeVariableStars },
		{ ct_("The classical cepheids"),               EWClassicalCepheidsTypeVariableStars },
		{ ct_("Bright carbon stars"),                  EWCarbonStars },
		{ ct_("Bright barium stars"),                  EWBariumStars },
	};
	if (moduleMgr.isPluginLoaded("Novae"))
		items.append({ ct_("Bright nova stars"), EWBrightNovaStars });
	if (moduleMgr.isPluginLoaded("Supernovae"))
		items.append({ ct_("Bright supernova stars"), EWBrightSupernovaStars });

	if (moduleMgr.isPluginLoaded("Pulsars")
	    && properties->getStelPropertyValue("Pulsars.pulsarsVisible").toBool())
		items.append({ ct_("Pulsars"), EWPulsars });
	if (moduleMgr.isPluginLoaded("Exoplanets")
	    && properties->getStelPropertyValue("Exoplanets.showExoplanets").toBool())
		items.append({ ct_("Exoplanetary systems"), EWExoplanetarySystems });

	std::sort(items.begin(), items.end(),
	          [](const QPair<QString, int>& a, const QPair<QString, int>& b)
	          { return a.first.localeAwareCompare(b.first) < 0; });
	return items;
}

QString objectTypeFor(int category)
{
	static const QMap<int, QString> types = {
		{ EWPlanets, "Planet" },                    { EWBrightStars, "Star" },
		{ EWBrightNebulae, "Nebula" },              { EWDarkNebulae, "Nebula" },
		{ EWGalaxies, "Nebula" },                   { EWOpenStarClusters, "Nebula" },
		{ EWAsteroids, "Planet" },                  { EWComets, "Planet" },
		{ EWPlutinos, "Planet" },                   { EWDwarfPlanets, "Planet" },
		{ EWCubewanos, "Planet" },                  { EWScatteredDiscObjects, "Planet" },
		{ EWOortCloudObjects, "Planet" },           { EWSednoids, "Planet" },
		{ EWPlanetaryNebulae, "Nebula" },           { EWBrightDoubleStars, "Star" },
		{ EWBrightVariableStars, "Star" },          { EWBrightStarsWithHighProperMotion, "Star" },
		{ EWSymbioticStars, "Nebula" },             { EWEmissionLineStars, "Nebula" },
		{ EWSupernovaeCandidates, "Nebula" },       { EWSupernovaeRemnantCandidates, "Nebula" },
		{ EWSupernovaeRemnants, "Nebula" },         { EWClustersOfGalaxies, "Nebula" },
		{ EWInterstellarObjects, "Planet" },        { EWGlobularStarClusters, "Nebula" },
		{ EWRegionsOfTheSky, "Nebula" },            { EWActiveGalaxies, "Nebula" },
		{ EWInteractingGalaxies, "Nebula" },        { EWDeepSkyObjects, "Nebula" },
		{ EWMessierObjects, "Nebula" },             { EWNGCICObjects, "Nebula" },
		{ EWCaldwellObjects, "Nebula" },            { EWHerschel400Objects, "Nebula" },
		{ EWAlgolTypeVariableStars, "Star" },       { EWClassicalCepheidsTypeVariableStars, "Star" },
		{ EWCarbonStars, "Star" },                  { EWBariumStars, "Star" },
		{ EWBrightNovaStars, "Nova" },              { EWBrightSupernovaStars, "Supernova" },
		{ EWPulsars, "Pulsar" },                    { EWExoplanetarySystems, "Exoplanet" },
	};
	return types.value(category, QString());
}

QList<double> nightMoments(StelCore* core, SolarSystem* solarSystem, int interval)
{
	const Vec4d rts = solarSystem->getSun()->getRTSTime(core, -6.);
	switch (interval)
	{
		case 1:  return { rts[0] };
		case 2:  return { rts[1] + 0.5 };
		case 3:  return { rts[0], rts[1] + 0.5, rts[2] };
		default: return { rts[2] };
	}
}

QJsonObject row(StelCore* core, const QString& name, const QString& designation, float magnitude,
                const Vec4d& rts, double maxElevation, double angularSize,
                const QString& constellation, const QString& objectType, bool decimalDegrees)
{
	const double utcShift = core->getUTCOffset(core->getJD()) / 24.;

	QString rise = kDash, set = kDash;
	const QString transit =
			StelUtils::hoursToHmsStr(StelUtils::getHoursFromJulianDay(rts[1] + utcShift), true);

	if (rts[3] == 0.)
	{
		rise = StelUtils::hoursToHmsStr(StelUtils::getHoursFromJulianDay(rts[0] + utcShift), true);
		set  = StelUtils::hoursToHmsStr(StelUtils::getHoursFromJulianDay(rts[2] + utcShift), true);
	}

	QString size = kDash;
	if (angularSize > 0.)
	{
		const double radians = angularSize * M_PI_180;
		size = decimalDegrees ? StelUtils::radToDecDegStr(radians, 5, false, true)
		                      : StelUtils::radToDmsPStr(radians, 2);
	}

	QJsonObject entry;
	entry["name"] = name;

	entry["select"] = designation;
	entry["mag"] = magnitude > 98.f ? kDash : QString::number(static_cast<double>(magnitude), 'f', 2);
	entry["rise"] = rise;
	entry["transit"] = transit;
	entry["elev"] = decimalDegrees ? StelUtils::radToDecDegStr(maxElevation, 5, false, true)
	                               : StelUtils::radToDmsPStr(maxElevation, 2);
	entry["set"] = set;
	entry["size"] = size;
	entry["const"] = constellation;
	entry["type"] = objectType;
	return entry;
}

void calculate()
{
	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	NebulaMgr* dsoMgr = GETSTELMODULE(NebulaMgr);
	StarMgr* starMgr = GETSTELMODULE(StarMgr);

	kRows = QJsonArray();
	kWhen.clear();
	if (!solarSystem || !dsoMgr || !starMgr)
		return;

	const Nebula::TypeGroup tflags = static_cast<Nebula::TypeGroup>(dsoMgr->getTypeFilters());
	const bool decimalDegrees = app.getFlagShowDecimalDegrees();
	const bool angularSizeLimit = settingBool("wut_angular_limit_flag", false);

	const double angularSizeLimitMin = settingDouble("wut_angular_limit_min", 10.0) / 60.;
	const double angularSizeLimitMax = settingDouble("wut_angular_limit_max", 600.0) / 60.;
	const double altitudeLimitMin = settingDouble("wut_altitude_min", 0.0);
	const float magLimit = static_cast<float>(settingDouble("wut_magnitude_limit", 10.0));
	const int category = kCategory;

	const double currentJD = core->getJD();
	const QList<double> moments =
			nightMoments(core, solarSystem, settingInt("wut_time_interval", 0));
	if (!moments.isEmpty())
		kWhen = asteriumFormatSimTime(moments.first(), "yyyy-MM-dd HH:mm");

	QSet<QString> seen;

	const auto keep = [&](const StelObjectP& object, const QString& name, const QString& designation,
	                      float magnitude, double angularSize)
	{
		if (seen.contains(designation))
			return;
		seen.insert(designation);
		const Vec4d rts = object->getRTSTime(core, altitudeLimitMin);
		const double top = culminationElevation(object, core);
		kRows.append(row(core, name, designation, magnitude, rts, top, angularSize,
		                 core->getIAUConstellation(object->getEquinoxEquatorialPos(core)),
		                 object->getObjectTypeI18n(), decimalDegrees));
	};

	const auto keepStar = [&](const StelObjectP& object, double angularSize)
	{
		QString designation = object->getEnglishName();
		if (designation.isEmpty())
			designation = object->getID();
		QString name = object->getNameI18n();
		if (name.isEmpty())
			name = designation;
		keep(object, name, designation, object->getVMagnitude(core), angularSize);
	};

	const auto keepDso = [&](const NebulaP& object, float magnitude)
	{
		if (angularSizeLimit
		    && !StelUtils::isWithin(object->getAngularRadius(core), angularSizeLimitMin, angularSizeLimitMax))
			return;
		QString d = object->getDSODesignation();
		if (d.isEmpty())
			d = object->getDSODesignationWIC();
		const QString n = object->getNameI18n();
		if (d.isEmpty() && n.isEmpty())
			return;
		const QString key = QString("%1:%2").arg(d, n);
		if (seen.contains(key))
			return;
		seen.insert(key);

		const StelObjectP asObject = qSharedPointerCast<StelObject>(object);
		const Vec4d rts = asObject->getRTSTime(core, altitudeLimitMin);
		const double top = culminationElevation(asObject, core);
		QString name = QString("%1 (%2)").arg(d, n), designation = d;
		if (d.isEmpty())
			name = designation = n;
		else if (n.isEmpty())
			name = designation = d;
		kRows.append(row(core, name, designation, magnitude, rts, top,
		                 object->getAngularRadius(core),
		                 core->getIAUConstellation(asObject->getEquinoxEquatorialPos(core)),
		                 asObject->getObjectTypeI18n(), decimalDegrees));
	};

	for (const double moment : moments)
	{
		core->setJD(moment);
		core->update(0);

		switch (category)
		{
			case EWBrightStars:
			case EWCarbonStars:
			case EWBariumStars:
			{
				QList<StelObjectP> stars = starMgr->getHipparcosCarbonStars();
				if (category == EWBrightStars)
					stars = starMgr->getHipparcosStars();
				else if (category == EWBariumStars)
					stars = starMgr->getHipparcosBariumStars();
				for (const StelObjectP& object : std::as_const(stars))
				{
					if (object->getVMagnitude(core) <= magLimit && object->isAboveRealHorizon(core))
						keepStar(object, 0.);
				}
				break;
			}
			case EWBrightNebulae:
			case EWDarkNebulae:
			case EWGalaxies:
			case EWOpenStarClusters:
			case EWPlanetaryNebulae:
			case EWSymbioticStars:
			case EWEmissionLineStars:
			case EWSupernovaeCandidates:
			case EWSupernovaeRemnantCandidates:
			case EWSupernovaeRemnants:
			case EWClustersOfGalaxies:
			case EWGlobularStarClusters:
			case EWRegionsOfTheSky:
			case EWInteractingGalaxies:
			case EWDeepSkyObjects:
			{
				for (const NebulaP& object : dsoMgr->getAllDeepSkyObjects())
				{
					bool wanted = false;
					float mag = object->getVMagnitude(core);
					const Nebula::NebulaType ntype = object->getDSOType();

					const bool visible = (mag <= magLimit) || (mag > 90.f && magLimit >= 19.f);
					switch (category)
					{
						case EWBrightNebulae:
							wanted = static_cast<bool>(tflags & Nebula::TypeBrightNebulae)
							         && (ntype == Nebula::NebN || ntype == Nebula::NebBn
							             || ntype == Nebula::NebEn || ntype == Nebula::NebRn
							             || ntype == Nebula::NebHII || ntype == Nebula::NebISM
							             || ntype == Nebula::NebCn || ntype == Nebula::NebSNR)
							         && mag <= magLimit;
							break;
						case EWDarkNebulae:
							wanted = static_cast<bool>(tflags & Nebula::TypeDarkNebulae)
							         && (ntype == Nebula::NebDn || ntype == Nebula::NebMolCld
							             || ntype == Nebula::NebYSO);
							break;
						case EWGalaxies:
							wanted = static_cast<bool>(tflags & Nebula::TypeGalaxies)
							         && ntype == Nebula::NebGx && mag <= magLimit;
							break;
						case EWOpenStarClusters:
							wanted = static_cast<bool>(tflags & Nebula::TypeOpenStarClusters)
							         && (ntype == Nebula::NebCl || ntype == Nebula::NebOc
							             || ntype == Nebula::NebSA || ntype == Nebula::NebSC
							             || ntype == Nebula::NebCn)
							         && mag <= magLimit;
							break;
						case EWPlanetaryNebulae:
							wanted = static_cast<bool>(tflags & Nebula::TypePlanetaryNebulae)
							         && (ntype == Nebula::NebPn || ntype == Nebula::NebPossPN
							             || ntype == Nebula::NebPPN)
							         && mag <= magLimit;
							break;
						case EWSymbioticStars:
							wanted = static_cast<bool>(tflags & Nebula::TypeOther)
							         && ntype == Nebula::NebSymbioticStar && mag <= magLimit;
							break;
						case EWEmissionLineStars:
							wanted = static_cast<bool>(tflags & Nebula::TypeOther)
							         && ntype == Nebula::NebEmissionLineStar && mag <= magLimit;
							break;
						case EWSupernovaeCandidates:
							wanted = static_cast<bool>(tflags & Nebula::TypeSupernovaRemnants)
							         && ntype == Nebula::NebSNC && visible;
							break;
						case EWSupernovaeRemnantCandidates:
							wanted = static_cast<bool>(tflags & Nebula::TypeSupernovaRemnants)
							         && ntype == Nebula::NebSNRC && visible;
							break;
						case EWSupernovaeRemnants:
							wanted = static_cast<bool>(tflags & Nebula::TypeSupernovaRemnants)
							         && ntype == Nebula::NebSNR && visible;
							break;
						case EWClustersOfGalaxies:
							wanted = static_cast<bool>(tflags & Nebula::TypeGalaxyClusters)
							         && ntype == Nebula::NebGxCl && mag <= magLimit;
							break;
						case EWGlobularStarClusters:
							wanted = static_cast<bool>(tflags & Nebula::TypeGlobularStarClusters)
							         && ntype == Nebula::NebGc && mag <= magLimit;
							break;
						case EWRegionsOfTheSky:
							wanted = static_cast<bool>(tflags & Nebula::TypeOther)
							         && ntype == Nebula::NebRegion;
							break;
						case EWInteractingGalaxies:
							wanted = static_cast<bool>(tflags & Nebula::TypeInteractingGalaxies)
							         && ntype == Nebula::NebIGx && mag <= magLimit;
							break;
						default:
							wanted = (mag <= magLimit);
							if (ntype == Nebula::NebDn)
								mag = 99.f;
							break;
					}
					if (wanted && object->isAboveRealHorizon(core))
						keepDso(object, mag);
				}
				break;
			}
			case EWPlanets:
			case EWAsteroids:
			case EWComets:
			case EWPlutinos:
			case EWDwarfPlanets:
			case EWCubewanos:
			case EWScatteredDiscObjects:
			case EWOortCloudObjects:
			case EWSednoids:
			case EWInterstellarObjects:
			{
				static const QMap<int, Planet::PlanetType> map = {
					{ EWPlanets,              Planet::isPlanet },
					{ EWAsteroids,            Planet::isAsteroid },
					{ EWComets,               Planet::isComet },
					{ EWPlutinos,             Planet::isPlutino },
					{ EWDwarfPlanets,         Planet::isDwarfPlanet },
					{ EWCubewanos,            Planet::isCubewano },
					{ EWScatteredDiscObjects, Planet::isSDO },
					{ EWOortCloudObjects,     Planet::isOCO },
					{ EWSednoids,             Planet::isSednoid },
					{ EWInterstellarObjects,  Planet::isInterstellar },
				};
				const Planet::PlanetType wantedType = map.value(category, Planet::isInterstellar);
				for (const PlanetP& object : solarSystem->getAllPlanets())
				{
					if (object->getPlanetType() != wantedType
					    || object->getVMagnitude(core) > magLimit
					    || !object->isAboveRealHorizon(core))
						continue;
					if (angularSizeLimit
					    && !StelUtils::isWithin(object->getAngularRadius(core),
					                            angularSizeLimitMin, angularSizeLimitMax))
						continue;

					keep(qSharedPointerCast<StelObject>(object), object->getNameI18n(),
					     object->getEnglishName(), object->getVMagnitude(core),
					     2. * object->getAngularRadius(core));
				}
				break;
			}
			case EWBrightDoubleStars:
			{
				for (const StelACStarData& pair : starMgr->getHipparcosDoubleStars())
				{
					const StelObjectP object = pair.first;
					if (object->getVMagnitude(core) > magLimit || !object->isAboveRealHorizon(core))
						continue;

					if (angularSizeLimit
					    && !StelUtils::isWithin(static_cast<double>(pair.second) / 3600.,
					                            angularSizeLimitMin, angularSizeLimitMax))
						continue;
					keepStar(object, static_cast<double>(pair.second) / 3600.);
				}
				break;
			}
			case EWAlgolTypeVariableStars:
			case EWClassicalCepheidsTypeVariableStars:
			case EWBrightVariableStars:
			{
				QList<StelACStarData> stars = starMgr->getHipparcosVariableStars();
				if (category == EWAlgolTypeVariableStars)
					stars = starMgr->getHipparcosAlgolTypeStars();
				else if (category == EWClassicalCepheidsTypeVariableStars)
					stars = starMgr->getHipparcosClassicalCepheidsTypeStars();
				for (const StelACStarData& pair : std::as_const(stars))
				{
					if (pair.first->getVMagnitude(core) <= magLimit
					    && pair.first->isAboveRealHorizon(core))
						keepStar(pair.first, 0.);
				}
				break;
			}
			case EWBrightStarsWithHighProperMotion:
			{
				for (const StelACStarData& pair : starMgr->getHipparcosHighPMStars())
				{
					if (pair.first->getVMagnitude(core) <= magLimit
					    && pair.first->isAboveRealHorizon(core))
						keepStar(pair.first, 0.);
				}
				break;
			}
			case EWActiveGalaxies:
			{
				for (const NebulaP& object : dsoMgr->getAllDeepSkyObjects())
				{
					const float mag = object->getVMagnitude(core);
					const Nebula::NebulaType ntype = object->getDSOType();
					if (static_cast<bool>(tflags & Nebula::TypeActiveGalaxies)
					    && (ntype == Nebula::NebQSO || ntype == Nebula::NebPossQSO
					        || ntype == Nebula::NebAGx || ntype == Nebula::NebRGx
					        || ntype == Nebula::NebBLA || ntype == Nebula::NebBLL)
					    && mag <= magLimit && object->isAboveRealHorizon(core))
						keepDso(object, mag);
				}
#ifdef USE_STATIC_PLUGIN_QUASARS
				if (app.getModuleMgr().isPluginLoaded("Quasars")
				    && app.getStelPropertyManager()->getStelPropertyValue("Quasars.quasarsVisible").toBool())
				{
					for (const auto& object : GETSTELMODULE(Quasars)->getAllQuasars())
					{
						if (object->getVMagnitude(core) > magLimit
						    || !object->isAboveRealHorizon(core)
						    || object->getEnglishName().isEmpty())
							continue;
						keep(qSharedPointerCast<StelObject>(object), object->getNameI18n(),
						     object->getEnglishName(), object->getVMagnitude(core), 0.);
					}
				}
#endif
				break;
			}
			case EWPulsars:
			{
#ifdef USE_STATIC_PLUGIN_PULSARS
				for (const auto& object : GETSTELMODULE(Pulsars)->getAllPulsars())
				{
					if (!object->isAboveRealHorizon(core))
						continue;
					QString designation = object->getEnglishName();
					if (designation.isEmpty())
						designation = object->getDesignation();
					if (designation.isEmpty())
						continue;
					QString name = object->getNameI18n();
					if (name.isEmpty())
						name = designation;

					keep(qSharedPointerCast<StelObject>(object), name, designation, 99.f, 0.);
				}
#endif
				break;
			}
			case EWExoplanetarySystems:
			{
#ifdef USE_STATIC_PLUGIN_EXOPLANETS
				for (const auto& object : GETSTELMODULE(Exoplanets)->getAllExoplanetarySystems())
				{
					if (object->getVMagnitude(core) > magLimit || !object->isVMagnitudeDefined()
					    || !object->isAboveRealHorizon(core) || object->getEnglishName().isEmpty())
						continue;
					keep(qSharedPointerCast<StelObject>(object), object->getNameI18n().trimmed(),
					     object->getEnglishName(), object->getVMagnitude(core), 0.);
				}
#endif
				break;
			}
			case EWBrightNovaStars:
			{
#ifdef USE_STATIC_PLUGIN_NOVAE
				for (const auto& object : GETSTELMODULE(Novae)->getAllBrightNovae())
				{
					if (object->getVMagnitude(core) <= magLimit && object->isAboveRealHorizon(core))
						keep(qSharedPointerCast<StelObject>(object), object->getNameI18n(),
						     object->getEnglishName(), object->getVMagnitude(core), 0.);
				}
#endif
				break;
			}
			case EWBrightSupernovaStars:
			{
#ifdef USE_STATIC_PLUGIN_SUPERNOVAE
				for (const auto& object : GETSTELMODULE(Supernovae)->getAllBrightSupernovae())
				{
					if (object->getVMagnitude(core) <= magLimit && object->isAboveRealHorizon(core))
						keep(qSharedPointerCast<StelObject>(object), object->getNameI18n(),
						     object->getEnglishName(), object->getVMagnitude(core), 0.);
				}
#endif
				break;
			}
			case EWMessierObjects:
			case EWNGCICObjects:
			case EWCaldwellObjects:
			case EWHerschel400Objects:
			{
				QList<NebulaP> catalogue;
				if (category == EWMessierObjects)
					catalogue = dsoMgr->getDeepSkyObjectsByType("100");
				else if (category == EWNGCICObjects)
				{
					catalogue = dsoMgr->getDeepSkyObjectsByType("108");
					catalogue.append(dsoMgr->getDeepSkyObjectsByType("109"));
				}
				else if (category == EWCaldwellObjects)
					catalogue = dsoMgr->getDeepSkyObjectsByType("101");
				else
					catalogue = dsoMgr->getDeepSkyObjectsByType("151");

				for (const NebulaP& object : std::as_const(catalogue))
				{
					if (object->getVMagnitude(core) <= magLimit && object->isAboveRealHorizon(core))
						keepDso(object, object->getVMagnitude(core));
				}
				break;
			}
			default:
				break;
		}
	}

	core->setJD(currentJD);
	core->update(0);
}

void select(const QString& designation)
{
	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	StelObjectMgr& objects = app.getStelObjectMgr();
	const QString type = objectTypeFor(kCategory);
	if (!objects.findAndSelect(designation, type) && !objects.findAndSelectI18n(designation, type))
		return;

	const QList<StelObjectP> found = objects.getSelectedObject();
	StelMovementMgr* movement = core->getMovementMgr();
	if (found.isEmpty() || !movement)
		return;

	if (found.first()->getEnglishName() == core->getCurrentLocation().planetName)
	{
		objects.unSelect();
		return;
	}
	asteriumMoveToSelected(movement, found.first(), movement->getAutoMoveDuration());
}

void state(QJsonObject& out)
{
	QJsonArray items;
	for (const auto& item : categories())
	{
		QJsonObject entry;
		entry["id"] = QString::number(item.second);
		entry["name"] = item.first;
		items.append(entry);
	}
	out["categories"] = items;
	out["category"] = QString::number(kCategory);

	const char* const intervals[] = { "In the Evening", "In the Morning", "Around Midnight",
	                                  "In Any Time of the Night" };
	QJsonArray whens;
	for (int i = 0; i < 4; ++i)
	{
		QJsonObject entry;
		entry["id"] = QString::number(i);
		entry["name"] = qc_(QString::fromUtf8(intervals[i]), "Celestial object is observed...");
		whens.append(entry);
	}
	out["intervals"] = whens;
	out["interval"] = QString::number(settingInt("wut_time_interval", 0));

	out["mag"] = settingDouble("wut_magnitude_limit", 10.0);
	out["angularLimit"] = settingBool("wut_angular_limit_flag", false);
	out["angularMin"] = settingDouble("wut_angular_limit_min", 10.0);
	out["angularMax"] = settingDouble("wut_angular_limit_max", 600.0);
	out["altitude"] = settingDouble("wut_altitude_min", 0.0);

	const Columns columns = columnsFor(kCategory);

	out["magLabel"] = columns.opacity ? ct_("Opac.") : ct_("Mag.");

	out["sizeLabel"] = columns.separation ? ct_("Sep.") : ct_("Ang. Size");
	out["limitLabel"] = columns.separation ? ct_("Limit angular separation")
	                                       : ct_("Limit angular size");
	out["showMag"] = columns.magnitude;
	out["showSize"] = columns.size;
	out["angularLimits"] = columns.angularLimits;
	out["constLabel"] = qc_("Const.", "IAU Constellation");
	out["riseLabel"] = qc_("Rise", "celestial event");
	out["transitLabel"] = qc_("Transit", "celestial event; passage across a meridian");
	out["setLabel"] = qc_("Set", "celestial event");
	out["elevLabel"] = ct_("Elev.");

	out["when"] = kWhen;
	out["rows"] = kRows;
}
}

bool answer(const QString& verb, const QString&, QJsonObject& out)
{
	if (verb != QLatin1String("wut") && verb != QLatin1String("wut.generate"))
		return false;
	if (verb == QLatin1String("wut.generate"))
		calculate();
	state(out);
	return true;
}

bool perform(const QString& verb, const QString& arg)
{
	if (verb == QLatin1String("wut.set"))
	{
		const QString key = arg.section('=', 0, 0);
		const QString value = arg.section('=', 1);
		if (key == QLatin1String("category"))
			kCategory = value.toInt();
		else
		{
			static const QSet<QString> twoPlaces = {
				QStringLiteral("wut_magnitude_limit"), QStringLiteral("wut_angular_limit_min"),
				QStringLiteral("wut_angular_limit_max"), QStringLiteral("wut_altitude_min"),
			};
			writeSetting(key, twoPlaces.contains(key)
			                  ? QString::number(value.toDouble(), 'f', 2) : value);
		}
		return true;
	}
	if (verb == QLatin1String("wut.select"))
	{
		select(arg);
		return true;
	}
	if (verb == QLatin1String("wut.clear"))
	{
		kRows = QJsonArray();
		kWhen.clear();
		return true;
	}
	return false;
}
}
}
