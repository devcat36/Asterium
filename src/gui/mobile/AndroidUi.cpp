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

#include "AndroidUi.hpp"

#include "StelApp.hpp"
#include "StelCore.hpp"
#include "StelUtils.hpp"
#include "StelTranslator.hpp"
#include "StelActionMgr.hpp"
#include "StelPropertyMgr.hpp"
#include "StelObjectMgr.hpp"
#include "StelObject.hpp"
#include "StelModuleMgr.hpp"
#include "StelMovementMgr.hpp"
#include "StelLocationMgr.hpp"
#include "StelLocaleMgr.hpp"
#include "StelLocation.hpp"
#include "StelFileMgr.hpp"
#include "StelSkyDrawer.hpp"
#include "StelGui.hpp"
#include "StelMainView.hpp"
#include "StelLogger.hpp"
#include "LandscapeMgr.hpp"
#include "StelSkyCultureMgr.hpp"
#include "AsterismMgr.hpp"
#include "ConstellationMgr.hpp"
#include "Nebula.hpp"
#include "NebulaMgr.hpp"
#include "StarMgr.hpp"
#include "SolarSystem.hpp"
#include "Planet.hpp"
#include "MinorPlanet.hpp"
#include "AstroCalcDialog.hpp"
#include "AstroCalcPages.hpp"
#include "OcularsPanel.hpp"
#include "HighlightMgr.hpp"
#include "LabelMgr.hpp"
#include "SimbadSearcher.hpp"
#include "CustomObjectMgr.hpp"
#include "SpecialMarkersMgr.hpp"
#include "SearchDialog.hpp"
#include "StarMgr.hpp"
#ifdef ENABLE_SCRIPTING
#include "StelScriptMgr.hpp"
#endif

#include <QColor>
#include <QDir>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QMetaObject>
#include <QRegularExpression>
#include <QSet>
#include <QSettings>
#include <QTime>
#include <QTimeZone>
#include <QTimer>
#include <QUuid>
#include <QVariant>

#include <algorithm>
#include <cmath>
#include <memory>
#include <utility>

namespace
{
const StelTranslator& forkTranslator()
{
	static QString locale;
	static std::unique_ptr<StelTranslator> fork;
	const QString now = StelApp::getInstance().getLocaleMgr().getAppLanguage();
	if (!fork || locale != now)
	{
		locale = now;
		fork.reset(new StelTranslator("asterium", now));
	}
	return *fork;
}
}

QString asteriumTranslate(const QString& text)
{
	const QString upstream = StelTranslator::globalTranslator->qtranslate(text);
	return upstream != text ? upstream : forkTranslator().qtranslate(text);
}

QString asteriumShortLabel(const QString& text)
{
	const QString own = forkTranslator().qtranslate(text);
	return own != text ? own : StelTranslator::globalTranslator->qtranslate(text);
}

QString asteriumFormatSimTime(double jd, const QString& format)
{
	if (const StelCore* core = StelApp::getInstance().getCore())
		jd += core->getUTCOffset(jd) / 24.;

	int y = 0, m = 0, d = 0, h = 0, mi = 0, s = 0;
	StelUtils::getDateTimeFromJulianDay(jd, &y, &m, &d, &h, &mi, &s);
	if (y > 0)
		return StelUtils::jdToQDateTime(jd, Qt::LocalTime).toString(format);

	const QString date = QString("%1-%2-%3")
	                     .arg(y, 4, 10, QChar('0')).arg(m, 2, 10, QChar('0')).arg(d, 2, 10, QChar('0'));
	const QString time = QString("%1:%2:%3")
	                     .arg(h, 2, 10, QChar('0')).arg(mi, 2, 10, QChar('0')).arg(s, 2, 10, QChar('0'));
	const bool wantsDate = format.contains('y') || format.contains('M') || format.contains('d');
	const bool wantsTime = format.contains('H') || format.contains('m') || format.contains('s');
	if (wantsDate && wantsTime)
		return date + QStringLiteral(" ") + time;
	return wantsTime ? time : date;
}

static bool asteriumViewHeld = false;
static int asteriumFindNudge = 0;

void asteriumMoveToSelected(StelMovementMgr* movement, const StelObjectP& object, float duration)
{
	if (asteriumViewHeld)
	{
		++asteriumFindNudge;
		return;
	}
	movement->moveToObject(object, duration);
	movement->setFlagTracking(true);
}

#if defined(Q_OS_ANDROID)

#include <QJniEnvironment>
#include <QJniObject>
#include <jni.h>

namespace
{
const char* const kBridgeClass = "org/asterium/asterium/NativeBridge";

AndroidUi* uiInstance = nullptr;

struct ToolbarEntry { const char* action; const char* icon; const char* caption; const char* flag; };
const char kFullScreen[] = "actionSet_Full_Screen_Global";
bool fullScreenOn = true;
const ToolbarEntry kToolbar[] = {
	{ "actionShow_Constellation_Lines",      "ConstellationLines",  N_("Lines"),      ""                                             },
	{ "actionShow_Constellation_Labels",     "ConstellationLabels", N_("Names"),      ""                                             },
	{ "actionShow_Constellation_Art",        "ConstellationArt",    N_("Art"),        "StelGui.flagShowConstellationArtsButton"       },
	{ "actionShow_Constellation_Boundaries", "ConstellationBoundaries", N_("Bounds"), "StelGui.flagShowConstellationBoundariesButton" },
	{ "actionShow_Asterism_Lines",           "AsterismLines",       N_("Ast. lines"), "StelGui.flagShowAsterismLinesButton"           },
	{ "actionShow_Asterism_Labels",          "AsterismLabels",      N_("Ast. names"), "StelGui.flagShowAsterismLabelsButton"          },
	{ "actionShow_Equatorial_Grid",          "EquatorialGrid",      N_("Equ. grid"),  ""                                             },
	{ "actionShow_Azimuthal_Grid",           "AzimuthalGrid",       N_("Azi. grid"),  ""                                             },
	{ "actionShow_Ecliptic_Grid",            "EclipticGrid",        N_("Ecl. grid"),  "StelGui.flagShowEclipticGridButton"            },
	{ "actionShow_Equatorial_J2000_Grid",    "EquatorialJ2000Grid", N_("ICRS grid"),  "StelGui.flagShowICRSGridButton"                },
	{ "actionShow_Galactic_Grid",            "GalacticGrid",        N_("Gal. grid"),  "StelGui.flagShowGalacticGridButton"            },
	{ "actionShow_Ground",                   "Ground",              N_("Ground"),     ""                                             },
	{ "actionShow_Atmosphere",               "Atmosphere",          N_("Atmosph."),   ""                                             },
	{ "actionShow_Nebulas",                  "Nebula",              N_("Nebulae"),    ""                                             },
	{ "actionShow_DSO_Textures",             "NebulaeBackground",   N_("Neb. bg"),    "StelGui.flagShowNebulaBackgroundButton"        },
	{ "actionShow_Planets_Labels",           "Planets",             N_("Planets"),    ""                                             },
	{ "actionShow_Cardinal_Points",          "CardinalPoints",      N_("Cardinals"),  "StelGui.flagShowCardinalButton"                },
	{ "actionShow_Compass_Marks",            "Compass",             N_("Compass"),    "StelGui.flagShowCompassButton"                 },
	{ "actionSwitch_Equatorial_Mount",       "EquatorialMount",     N_("Equ. mount"), ""                                             },
	{ "actionVertical_Flip",                 "FlipVertical",        N_("Flip vert."), "StelGui.flagShowFlipButtons"                   },
	{ "actionHorizontal_Flip",               "FlipHorizontal",      N_("Flip horiz."),"StelGui.flagShowFlipButtons"                   },
	{ "actionShow_Night_Mode",               "NightView",           N_("Night"),      "StelGui.flagShowNightmodeButton"               },
	{ "actionShow_ObsList_Highlight",        "ObsList",             N_("Highlight"),  "StelGui.flagShowObsListButton"                 },
	{ kFullScreen,                           "FullScreen",          N_("Full screen"), "StelGui.flagShowFullscreenButton"         },
	{ "actionQuit_Global",                   "Quit",                N_("Quit"),       "StelGui.flagShowQuitButton"                    },
};

const double kMaxTimeRate = 10000.;

void clampTimeRate(StelCore* core)
{
	const double factor = core->getTimeRate() / StelCore::JD_SECOND;
	if (std::abs(factor) > kMaxTimeRate)
		core->setTimeRate(std::copysign(kMaxTimeRate, factor) * StelCore::JD_SECOND);
}

bool toolbarState(const char* action, const StelAction* a)
{
	return qstrcmp(action, kFullScreen) == 0 ? fullScreenOn : a->isChecked();
}

struct DeltaTEntry { const char* key; const char* name; };
const DeltaTEntry kDeltaT[] = {
	{ "WithoutCorrection",              "Without correction"                          },
	{ "Schoch",                         "Schoch (1931)"                               },
	{ "Clemence",                       "Clemence (1948)"                             },
	{ "IAU",                            "IAU (1952)"                                  },
	{ "AstronomicalEphemeris",          "Astronomical Ephemeris (1960)"               },
	{ "TuckermanGoldstine",             "Tuckerman (1962, 1964) & Goldstine (1973)"   },
	{ "MullerStephenson",               "Muller & Stephenson (1975)"                  },
	{ "Stephenson1978",                 "Stephenson (1978)"                           },
	{ "SchmadelZech1979",               "Schmadel & Zech (1979)"                      },
	{ "MorrisonStephenson1982",         "Morrison & Stephenson (1982)"                },
	{ "StephensonMorrison1984",         "Stephenson & Morrison (1984)"                },
	{ "StephensonHoulden",              "Stephenson & Houlden (1986)"                 },
	{ "Espenak",                        "Espenak (1987, 1989)"                        },
	{ "Borkowski",                      "Borkowski (1988)"                            },
	{ "SchmadelZech1988",               "Schmadel & Zech (1988)"                      },
	{ "ChaprontTouze",                  "Chapront-Touze & Chapront (1991)"            },
	{ "StephensonMorrison1995",         "Stephenson & Morrison (1995)"                },
	{ "Stephenson1997",                 "Stephenson (1997)"                           },
	{ "ChaprontMeeus",                  "Meeus (1998)"                                },
	{ "JPLHorizons",                    "JPL Horizons"                                },
	{ "MeeusSimons",                    "Meeus & Simons (2000)"                       },
	{ "MontenbruckPfleger",             "Montenbruck & Pfleger (2000)"                },
	{ "ReingoldDershowitz",             "Reingold & Dershowitz (2002, 2007, 2018)"    },
	{ "MorrisonStephenson2004",         "Morrison & Stephenson (2004, 2005)"          },
	{ "EspenakMeeus",                   "Espenak & Meeus (2006, 2014)"                },
	{ "EspenakMeeusModified",           "Espenak & Meeus, modified (2006, 2014, 2023)" },
	{ "Reijs",                          "Reijs (2006)"                                },
	{ "Banjevic",                       "Banjevic (2006)"                             },
	{ "IslamSadiqQureshi",              "Islam, Sadiq & Qureshi (2008, 2013)"         },
	{ "KhalidSultanaZaidi",             "Khalid, Sultana & Zaidi (2014)"              },
	{ "StephensonMorrisonHohenkerk2016", "Stephenson, Morrison & Hohenkerk (2016, 2021)" },
	{ "Henriksson2017",                 "Henriksson (2017)"                           },
	{ "Custom",                         "Custom equation of ΔT"                       },
};

const char* const kSkyCultureRegions[] = {
	"World", "Antarctica",
	"Eastern Europe", "Northern Europe", "Southern Europe", "Western Europe",
	"Eastern Africa", "Central Africa", "Northern Africa", "Southern Africa", "Western Africa",
	"Caribbean", "Central America", "Northern America", "South America",
	"Northern Asia", "Central Asia", "Eastern Asia", "South-eastern Asia", "Southern Asia",
	"Western Asia",
	"Australasia", "Melanesia", "Micronesia", "Polynesia",
	"Other",
};

struct CatalogEntry { const char* label; const char* name; int bit; };
const CatalogEntry kCatalogs[] = {
	{ "M",           "Messier Catalogue",                                          Nebula::CatM      },
	{ "C",           "Caldwell Catalogue",                                         Nebula::CatC      },
	{ "NGC",         "New General Catalogue of Nebulae and Clusters of Stars",     Nebula::CatNGC    },
	{ "IC",          "Index Catalogue of Nebulae and Clusters of Stars",           Nebula::CatIC     },
	{ "B",           "Barnard's Catalogue of 349 Dark Objects in the Sky",         Nebula::CatB      },
	{ "SH 2",        "Catalogue of HII Regions (Sharpless, 1959)",                 Nebula::CatSh2    },
	{ "vdB",         "Catalogue of Reflection Nebulae (van den Bergh, 1966)",      Nebula::CatVdB    },
	{ "RCW",         "Hα-emission regions in the southern Milky Way (Rodgers+)",   Nebula::CatRCW    },
	{ "LBN",         "Lynds' Catalogue of Bright Nebulae (Lynds, 1965)",           Nebula::CatLBN    },
	{ "LDN",         "Lynds' Catalogue of Dark Nebulae (Lynds, 1962)",             Nebula::CatLDN    },
	{ "Cr",          "Catalog of Open Galactic Clusters (Collinder, 1931)",        Nebula::CatCr     },
	{ "Mel",         "Star Clusters on Franklin-Adams Chart Plates (Melotte)",     Nebula::CatMel    },
	{ "PGC",         "Principal Galaxy Catalog",                                   Nebula::CatPGC    },
	{ "UGC",         "The Uppsala General Catalogue of Galaxies",                  Nebula::CatUGC    },
	{ "Ced",         "Bright diffuse Galactic nebulae (Cederblad, 1946)",          Nebula::CatCed    },
	{ "Arp",         "Atlas of Peculiar Galaxies (Arp, 1966)",                     Nebula::CatArp    },
	{ "VV",          "Interacting Galaxies (Vorontsov-Velyaminov+, 2001)",         Nebula::CatVV     },
	{ "PK",          "The Catalogue of Galactic Planetary Nebulae (Kohoutek)",     Nebula::CatPK     },
	{ "PN G",        "Strasbourg-ESO Galactic Planetary Nebulae (Acker+, 1992)",   Nebula::CatPNG    },
	{ "SNR G",       "A catalogue of Galactic supernova remnants (Green, 2014)",   Nebula::CatSNRG   },
	{ "Abell (ACO)", "A Catalog of Rich Clusters of Galaxies (Abell+, 1989)",      Nebula::CatACO    },
	{ "HCG",         "Hickson Compact Group (Hickson+, 1982)",                     Nebula::CatHCG    },
	{ "vdBH",        "Southern stars embedded in nebulosity (van den Bergh+)",     Nebula::CatVdBH   },
	{ "ESO",         "ESO/Uppsala Survey of the ESO(B) Atlas (Lauberts, 1982)",    Nebula::CatESO    },
	{ "DWB",         "Optically visible H II regions (Dickel+, 1969)",             Nebula::CatDWB    },
	{ "Tr",          "Trumpler Catalogue",                                         Nebula::CatTr     },
	{ "St",          "Stock Catalogue",                                            Nebula::CatSt     },
	{ "Ru",          "Ruprecht Catalogue",                                         Nebula::CatRu     },
	{ "vdB-Ha",      "van den Bergh-Hagen Catalogue",                              Nebula::CatVdBHa  },
	{ "Other",       "Objects with no catalogue number",                           Nebula::CatOther  },
};

struct TypeEntry { const char* label; int bit; const char* ini; };
const TypeEntry kDsoTypes[] = {
	{ "Galaxies",               Nebula::TypeGalaxies,             "dso_type_filters/flag_show_galaxies"             },
	{ "Bright nebulae",         Nebula::TypeBrightNebulae,        "dso_type_filters/flag_show_bright_nebulae"       },
	{ "Active galaxies",        Nebula::TypeActiveGalaxies,       "dso_type_filters/flag_show_active_galaxies"      },
	{ "Dark nebulae",           Nebula::TypeDarkNebulae,          "dso_type_filters/flag_show_dark_nebulae"         },
	{ "Interacting galaxies",   Nebula::TypeInteractingGalaxies,  "dso_type_filters/flag_show_interacting_galaxies" },
	{ "Planetary nebulae",      Nebula::TypePlanetaryNebulae,     "dso_type_filters/flag_show_planetary_nebulae"    },
	{ "Clusters of galaxies",   Nebula::TypeGalaxyClusters,       "dso_type_filters/flag_show_galaxy_clusters"      },
	{ "Supernova remnants",     Nebula::TypeSupernovaRemnants,    "dso_type_filters/flag_show_supernova_remnants"   },
	{ "Open star clusters",     Nebula::TypeOpenStarClusters,     "dso_type_filters/flag_show_open_clusters"        },
	{ "Hydrogen regions",       Nebula::TypeHydrogenRegions,      "dso_type_filters/flag_show_hydrogen_regions"     },
	{ "Globular star clusters", Nebula::TypeGlobularStarClusters, "dso_type_filters/flag_show_globular_clusters"    },
	{ "Other",                  Nebula::TypeOther,                "dso_type_filters/flag_show_other"                },
};

struct DsoColorEntry { const char* label; const char* prop; };
const DsoColorEntry kDsoColors[] = {
	{ "Labels of DSOs",               "NebulaMgr.labelsColor" },
	{ "Markers of DSOs",              "NebulaMgr.circlesColor" },
	{ "Galaxies",                     "NebulaMgr.galaxiesColor" },
	{ "Active galaxies",              "NebulaMgr.activeGalaxiesColor" },
	{ "Radio galaxies",               "NebulaMgr.radioGalaxiesColor" },
	{ "Interacting galaxies",         "NebulaMgr.interactingGalaxiesColor" },
	{ "Quasars",                      "NebulaMgr.quasarsColor" },
	{ "Possible quasars",             "NebulaMgr.possibleQuasarsColor" },
	{ "BL Lac objects",               "NebulaMgr.blLacObjectsColor" },
	{ "Blazars",                      "NebulaMgr.blazarsColor" },
	{ "Star clusters",                "NebulaMgr.clustersColor" },
	{ "Open star clusters",           "NebulaMgr.openClustersColor" },
	{ "Globular star clusters",       "NebulaMgr.globularClustersColor" },
	{ "Stellar associations",         "NebulaMgr.stellarAssociationsColor" },
	{ "Star clouds",                  "NebulaMgr.starCloudsColor" },
	{ "Stars",                        "NebulaMgr.starsColor" },
	{ "Regions of the sky",           "NebulaMgr.regionsColor" },
	{ "Nebulae",                      "NebulaMgr.nebulaeColor" },
	{ "Planetary nebulae",            "NebulaMgr.planetaryNebulaeColor" },
	{ "Possible pl. nebulae",         "NebulaMgr.possiblePlanetaryNebulaeColor" },
	{ "Protoplanetary nebulae",       "NebulaMgr.protoplanetaryNebulaeColor" },
	{ "Dark nebulae",                 "NebulaMgr.darkNebulaeColor" },
	{ "Reflection nebulae",           "NebulaMgr.reflectionNebulaeColor" },
	{ "Bipolar nebulae",              "NebulaMgr.bipolarNebulaeColor" },
	{ "Emission nebulae",             "NebulaMgr.emissionNebulaeColor" },
	{ "Hydrogen regions",             "NebulaMgr.hydrogenRegionsColor" },
	{ "Molecular clouds",             "NebulaMgr.molecularCloudsColor" },
	{ "Interstellar matter",          "NebulaMgr.interstellarMatterColor" },
	{ "Emission objects",             "NebulaMgr.emissionObjectsColor" },
	{ "Young stellar objects",        "NebulaMgr.youngStellarObjectsColor" },
	{ "Supernova remnants",           "NebulaMgr.supernovaRemnantsColor" },
	{ "Symbiotic stars",              "NebulaMgr.symbioticStarsColor" },
	{ "Emission-line stars",          "NebulaMgr.emissionLineStarsColor" },
	{ "Supernova candidates",         "NebulaMgr.supernovaCandidatesColor" },
	{ "Supernova remnant candidates", "NebulaMgr.supernovaRemnantCandidatesColor" },
	{ "Clusters of galaxies",         "NebulaMgr.galaxyClustersColor" },
};

struct ColorKey { const char* prop; const char* ini; const char* module; };
const ColorKey kColorKeys[] = {
	{ "SolarSystem.ephemerisGenericMarkerColor",             "color/ephemeris_generic_marker_color",                      "" },
	{ "SolarSystem.ephemerisSecondaryMarkerColor",           "color/ephemeris_secondary_marker_color",                    "" },
	{ "SolarSystem.ephemerisSelectedMarkerColor",            "color/ephemeris_selected_marker_color",                     "" },
	{ "SolarSystem.ephemerisMercuryMarkerColor",             "color/ephemeris_mercury_marker_color",                      "" },
	{ "SolarSystem.ephemerisVenusMarkerColor",               "color/ephemeris_venus_marker_color",                        "" },
	{ "SolarSystem.ephemerisMarsMarkerColor",                "color/ephemeris_mars_marker_color",                         "" },
	{ "SolarSystem.ephemerisJupiterMarkerColor",             "color/ephemeris_jupiter_marker_color",                      "" },
	{ "SolarSystem.ephemerisSaturnMarkerColor",              "color/ephemeris_saturn_marker_color",                       "" },
	{ "StelApp.overwriteInfoColor",                          "color/info_text_color",                                     "" },
	{ "StelApp.daylightInfoColor",                           "color/daylight_text_color",                                 "" },
	{ "NebulaMgr.labelsColor",                               "color/dso_label_color",                                     "" },
	{ "NebulaMgr.circlesColor",                              "color/dso_circle_color",                                    "" },
	{ "NebulaMgr.galaxiesColor",                             "color/dso_galaxy_color",                                    "" },
	{ "NebulaMgr.activeGalaxiesColor",                       "color/dso_active_galaxy_color",                             "" },
	{ "NebulaMgr.radioGalaxiesColor",                        "color/dso_radio_galaxy_color",                              "" },
	{ "NebulaMgr.interactingGalaxiesColor",                  "color/dso_interacting_galaxy_color",                        "" },
	{ "NebulaMgr.quasarsColor",                              "color/dso_quasar_color",                                    "" },
	{ "NebulaMgr.possibleQuasarsColor",                      "color/dso_possible_quasar_color",                           "" },
	{ "NebulaMgr.clustersColor",                             "color/dso_cluster_color",                                   "" },
	{ "NebulaMgr.openClustersColor",                         "color/dso_open_cluster_color",                              "" },
	{ "NebulaMgr.globularClustersColor",                     "color/dso_globular_cluster_color",                          "" },
	{ "NebulaMgr.stellarAssociationsColor",                  "color/dso_stellar_association_color",                       "" },
	{ "NebulaMgr.starCloudsColor",                           "color/dso_star_cloud_color",                                "" },
	{ "NebulaMgr.starsColor",                                "color/dso_star_color",                                      "" },
	{ "NebulaMgr.symbioticStarsColor",                       "color/dso_symbiotic_star_color",                            "" },
	{ "NebulaMgr.emissionLineStarsColor",                    "color/dso_emission_star_color",                             "" },
	{ "NebulaMgr.nebulaeColor",                              "color/dso_nebula_color",                                    "" },
	{ "NebulaMgr.planetaryNebulaeColor",                     "color/dso_planetary_nebula_color",                          "" },
	{ "NebulaMgr.darkNebulaeColor",                          "color/dso_dark_nebula_color",                               "" },
	{ "NebulaMgr.reflectionNebulaeColor",                    "color/dso_reflection_nebula_color",                         "" },
	{ "NebulaMgr.bipolarNebulaeColor",                       "color/dso_bipolar_nebula_color",                            "" },
	{ "NebulaMgr.emissionNebulaeColor",                      "color/dso_emission_nebula_color",                           "" },
	{ "NebulaMgr.possiblePlanetaryNebulaeColor",             "color/dso_possible_planetary_nebula_color",                 "" },
	{ "NebulaMgr.protoplanetaryNebulaeColor",                "color/dso_protoplanetary_nebula_color",                     "" },
	{ "NebulaMgr.hydrogenRegionsColor",                      "color/dso_hydrogen_region_color",                           "" },
	{ "NebulaMgr.interstellarMatterColor",                   "color/dso_interstellar_matter_color",                       "" },
	{ "NebulaMgr.emissionObjectsColor",                      "color/dso_emission_object_color",                           "" },
	{ "NebulaMgr.molecularCloudsColor",                      "color/dso_molecular_cloud_color",                           "" },
	{ "NebulaMgr.blLacObjectsColor",                         "color/dso_bl_lac_color",                                    "" },
	{ "NebulaMgr.blazarsColor",                              "color/dso_blazar_color",                                    "" },
	{ "NebulaMgr.youngStellarObjectsColor",                  "color/dso_young_stellar_object_color",                      "" },
	{ "NebulaMgr.supernovaRemnantsColor",                    "color/dso_supernova_remnant_color",                         "" },
	{ "NebulaMgr.supernovaCandidatesColor",                  "color/dso_supernova_candidate_color",                       "" },
	{ "NebulaMgr.supernovaRemnantCandidatesColor",           "color/dso_supernova_remnant_cand_color",                    "" },
	{ "NebulaMgr.galaxyClustersColor",                       "color/dso_galaxy_cluster_color",                            "" },
	{ "NebulaMgr.regionsColor",                              "color/dso_regions_color",                                   "" },
	{ "SolarSystem.orbitsColor",                             "color/sso_orbits_color",                                    "" },
	{ "SolarSystem.majorPlanetsOrbitsColor",                 "color/major_planet_orbits_color",                           "" },
	{ "SolarSystem.minorPlanetsOrbitsColor",                 "color/minor_planet_orbits_color",                           "" },
	{ "SolarSystem.dwarfPlanetsOrbitsColor",                 "color/dwarf_planet_orbits_color",                           "" },
	{ "SolarSystem.moonsOrbitsColor",                        "color/moon_orbits_color",                                   "" },
	{ "SolarSystem.cubewanosOrbitsColor",                    "color/cubewano_orbits_color",                               "" },
	{ "SolarSystem.plutinosOrbitsColor",                     "color/plutino_orbits_color",                                "" },
	{ "SolarSystem.scatteredDiskObjectsOrbitsColor",         "color/sdo_orbits_color",                                    "" },
	{ "SolarSystem.oortCloudObjectsOrbitsColor",             "color/oco_orbits_color",                                    "" },
	{ "SolarSystem.cometsOrbitsColor",                       "color/comet_orbits_color",                                  "" },
	{ "SolarSystem.sednoidsOrbitsColor",                     "color/sednoid_orbits_color",                                "" },
	{ "SolarSystem.interstellarOrbitsColor",                 "color/interstellar_orbits_color",                           "" },
	{ "SolarSystem.mercuryOrbitColor",                       "color/mercury_orbit_color",                                 "" },
	{ "SolarSystem.venusOrbitColor",                         "color/venus_orbit_color",                                   "" },
	{ "SolarSystem.earthOrbitColor",                         "color/earth_orbit_color",                                   "" },
	{ "SolarSystem.marsOrbitColor",                          "color/mars_orbit_color",                                    "" },
	{ "SolarSystem.jupiterOrbitColor",                       "color/jupiter_orbit_color",                                 "" },
	{ "SolarSystem.saturnOrbitColor",                        "color/saturn_orbit_color",                                  "" },
	{ "SolarSystem.uranusOrbitColor",                        "color/uranus_orbit_color",                                  "" },
	{ "SolarSystem.neptuneOrbitColor",                       "color/neptune_orbit_color",                                 "" },
	{ "NomenclatureMgr.nomenclatureColor",                   "color/planet_nomenclature_color",                           "" },
	{ "SolarSystem.labelsColor",                             "color/planet_names_color",                                  "" },
	{ "SolarSystem.trailsColor",                             "color/object_trails_color",                                 "" },
	{ "LandscapeMgr.labelColor",                             "landscape/label_color",                                     "" },
	{ "LandscapeMgr.polyLineColor",                          "landscape/polyline_color",                                  "" },
	{ "GridLinesMgr.eclipticJ2000GridColor",                 "color/ecliptical_J2000_color",                              "" },
	{ "GridLinesMgr.eclipticGridColor",                      "color/ecliptical_color",                                    "" },
	{ "GridLinesMgr.equatorJ2000GridColor",                  "color/equatorial_J2000_color",                              "" },
	{ "GridLinesMgr.equatorGridColor",                       "color/equatorial_color",                                    "" },
	{ "GridLinesMgr.fixedEquatorGridColor",                  "color/fixed_equatorial_color",                              "" },
	{ "GridLinesMgr.galacticGridColor",                      "color/galactic_color",                                      "" },
	{ "GridLinesMgr.supergalacticGridColor",                 "color/supergalactic_color",                                 "" },
	{ "GridLinesMgr.azimuthalGridColor",                     "color/azimuthal_color",                                     "" },
	{ "GridLinesMgr.eclipticJ2000LineColor",                 "color/ecliptic_J2000_color",                                "" },
	{ "GridLinesMgr.eclipticLineColor",                      "color/ecliptic_color",                                      "" },
	{ "GridLinesMgr.invariablePlaneLineColor",               "color/invariable_plane_color",                              "" },
	{ "GridLinesMgr.solarEquatorLineColor",                  "color/solar_equator_color",                                 "" },
	{ "GridLinesMgr.equatorJ2000LineColor",                  "color/equator_J2000_color",                                 "" },
	{ "GridLinesMgr.equatorLineColor",                       "color/equator_color",                                       "" },
	{ "GridLinesMgr.fixedEquatorLineColor",                  "color/fixed_equator_color",                                 "" },
	{ "GridLinesMgr.galacticEquatorLineColor",               "color/galactic_equator_color",                              "" },
	{ "GridLinesMgr.supergalacticEquatorLineColor",          "color/supergalactic_equator_color",                         "" },
	{ "GridLinesMgr.horizonLineColor",                       "color/horizon_color",                                       "" },
	{ "GridLinesMgr.longitudeLineColor",                     "color/oc_longitude_color",                                  "" },
	{ "GridLinesMgr.quadratureLineColor",                    "color/quadrature_color",                                    "" },
	{ "GridLinesMgr.colureLinesColor",                       "color/colures_color",                                       "" },
	{ "GridLinesMgr.circumpolarCirclesColor",                "color/circumpolar_circles_color",                           "" },
	{ "GridLinesMgr.umbraCircleColor",                       "color/umbra_circle_color",                                  "" },
	{ "GridLinesMgr.penumbraCircleColor",                    "color/penumbra_circle_color",                               "" },
	{ "GridLinesMgr.precessionCirclesColor",                 "color/precession_circles_color",                            "" },
	{ "GridLinesMgr.primeVerticalLineColor",                 "color/prime_vertical_color",                                "" },
	{ "GridLinesMgr.currentVerticalLineColor",               "color/current_vertical_color",                              "" },
	{ "GridLinesMgr.meridianLineColor",                      "color/meridian_color",                                      "" },
	{ "GridLinesMgr.celestialJ2000PolesColor",               "color/celestial_J2000_poles_color",                         "" },
	{ "GridLinesMgr.celestialPolesColor",                    "color/celestial_poles_color",                               "" },
	{ "GridLinesMgr.zenithNadirColor",                       "color/zenith_nadir_color",                                  "" },
	{ "GridLinesMgr.eclipticJ2000PolesColor",                "color/ecliptic_J2000_poles_color",                          "" },
	{ "GridLinesMgr.eclipticPolesColor",                     "color/ecliptic_poles_color",                                "" },
	{ "GridLinesMgr.galacticPolesColor",                     "color/galactic_poles_color",                                "" },
	{ "GridLinesMgr.galacticCenterColor",                    "color/galactic_center_color",                               "" },
	{ "GridLinesMgr.supergalacticPolesColor",                "color/supergalactic_poles_color",                           "" },
	{ "GridLinesMgr.equinoxJ2000PointsColor",                "color/equinox_J2000_points_color",                          "" },
	{ "GridLinesMgr.equinoxPointsColor",                     "color/equinox_points_color",                                "" },
	{ "GridLinesMgr.solsticeJ2000PointsColor",               "color/solstice_J2000_points_color",                         "" },
	{ "GridLinesMgr.solsticePointsColor",                    "color/solstice_points_color",                               "" },
	{ "GridLinesMgr.antisolarPointColor",                    "color/antisolar_point_color",                               "" },
	{ "GridLinesMgr.apexPointsColor",                        "color/apex_points_color",                                   "" },
	{ "SpecialMarkersMgr.fovCenterMarkerColor",              "color/fov_center_marker_color",                             "" },
	{ "SpecialMarkersMgr.fovCircularMarkerColor",            "color/fov_circular_marker_color",                           "" },
	{ "SpecialMarkersMgr.fovRectangularMarkerColor",         "color/fov_rectangular_marker_color",                        "" },
	{ "LandscapeMgr.cardinalPointsColor",                    "color/cardinal_color",                                      "" },
	{ "SpecialMarkersMgr.compassMarksColor",                 "color/compass_marks_color",                                 "" },
	{ "ConstellationMgr.boundariesColor",                    "color/const_boundary_color",                                "" },
	{ "ConstellationMgr.namesColor",                         "color/const_names_color",                                   "" },
	{ "ConstellationMgr.linesColor",                         "color/const_lines_color",                                   "" },
	{ "AsterismMgr.namesColor",                              "color/asterism_names_color",                                "" },
	{ "AsterismMgr.linesColor",                              "color/asterism_lines_color",                                "" },
	{ "AsterismMgr.rayHelpersColor",                         "color/rayhelper_lines_color",                               "" },
	{ "ConstellationMgr.hullsColor",                         "color/const_hulls_color",                                   "" },
	{ "ConstellationMgr.zodiacColor",                        "color/skyculture_zodiac_color",                             "" },
	{ "ConstellationMgr.lunarSystemColor",                   "color/skyculture_lunarsystem_color",                        "" },
	{ "AngleMeasure.equatorialLineColor",                    "AngleMeasure/line_color",                                   "" },
	{ "AngleMeasure.equatorialTextColor",                    "AngleMeasure/text_color",                                   "" },
	{ "AngleMeasure.horizontalLineColor",                    "AngleMeasure/line_color_horizontal",                        "" },
	{ "AngleMeasure.horizontalTextColor",                    "AngleMeasure/text_color_horizontal",                        "" },
	{ "ArchaeoLines.equinoxColor",                           "ArchaeoLines/color_equinox",                                "" },
	{ "ArchaeoLines.solsticesColor",                         "ArchaeoLines/color_solstices",                              "" },
	{ "ArchaeoLines.crossquartersColor",                     "ArchaeoLines/color_crossquarters",                          "" },
	{ "ArchaeoLines.majorStandstillColor",                   "ArchaeoLines/color_major_standstill",                       "" },
	{ "ArchaeoLines.minorStandstillColor",                   "ArchaeoLines/color_minor_standstill",                       "" },
	{ "ArchaeoLines.polarCirclesColor",                      "ArchaeoLines/color_polar_circles",                          "" },
	{ "ArchaeoLines.zenithPassageColor",                     "ArchaeoLines/color_zenith_passage",                         "" },
	{ "ArchaeoLines.nadirPassageColor",                      "ArchaeoLines/color_nadir_passage",                          "" },
	{ "ArchaeoLines.selectedObjectColor",                    "ArchaeoLines/color_selected_object",                        "" },
	{ "ArchaeoLines.selectedObjectAzimuthColor",             "ArchaeoLines/color_selected_object_azimuth",                "" },
	{ "ArchaeoLines.selectedObjectHourAngleColor",           "ArchaeoLines/color_selected_object_hour_angle",             "" },
	{ "ArchaeoLines.currentSunColor",                        "ArchaeoLines/color_current_sun",                            "" },
	{ "ArchaeoLines.currentMoonColor",                       "ArchaeoLines/color_current_moon",                           "" },
	{ "ArchaeoLines.currentPlanetColor",                     "ArchaeoLines/color_current_planet",                         "" },
	{ "ArchaeoLines.geographicLocation1Color",               "ArchaeoLines/color_geographic_location_1",                  "" },
	{ "ArchaeoLines.geographicLocation2Color",               "ArchaeoLines/color_geographic_location_2",                  "" },
	{ "ArchaeoLines.customAzimuth1Color",                    "ArchaeoLines/color_custom_azimuth_1",                       "" },
	{ "ArchaeoLines.customAzimuth2Color",                    "ArchaeoLines/color_custom_azimuth_2",                       "" },
	{ "ArchaeoLines.customAltitude1Color",                   "ArchaeoLines/color_custom_altitude_1",                      "" },
	{ "ArchaeoLines.customAltitude2Color",                   "ArchaeoLines/color_custom_altitude_2",                      "" },
	{ "ArchaeoLines.customDeclination1Color",                "ArchaeoLines/color_custom_declination_1",                   "" },
	{ "ArchaeoLines.customDeclination2Color",                "ArchaeoLines/color_custom_declination_2",                   "" },
	{ "Calendars.textColor",                                 "Calendars/text_color",                                      "" },
	{ "EquationOfTime.textColor",                            "EquationOfTime/text_color",                                 "" },
	{ "Exoplanets.markerColor",                              "Exoplanets/exoplanet_marker_color",                         "" },
	{ "Exoplanets.habitableColor",                           "Exoplanets/habitable_exoplanet_marker_color",               "" },
	{ "LensDistortionEstimator.imageAxesColor",              "LensDistortionEstimator/image_axes_color",                  "" },
	{ "LensDistortionEstimator.pointMarkerColor",            "LensDistortionEstimator/point_marker_color",                "" },
	{ "LensDistortionEstimator.selectedPointMarkerColor",    "LensDistortionEstimator/selected_point_marker_color",       "" },
	{ "LensDistortionEstimator.projectionCenterMarkerColor", "LensDistortionEstimator/center_of_projection_marker_color", "" },
	{ "MeteorShowers.colorARG",                              "MeteorShowers/colorARG",                                    "" },
	{ "MeteorShowers.colorARC",                              "MeteorShowers/colorARC",                                    "" },
	{ "MeteorShowers.colorIR",                               "MeteorShowers/colorIR",                                     "" },
	{ "Oculars.textColor",                                   "text_color",                                                "Oculars" },
	{ "Oculars.lineColor",                                   "line_color",                                                "Oculars" },
	{ "Oculars.reticleColor",                                "reticle_color",                                             "Oculars" },
	{ "Oculars.focuserColor",                                "focuser_color",                                             "Oculars" },
	{ "PointerCoordinates.fontColor",                        "PointerCoordinates/text_color",                             "" },
	{ "Pulsars.markerColor",                                 "Pulsars/marker_color",                                      "" },
	{ "Pulsars.glitchColor",                                 "Pulsars/glitch_color",                                      "" },
	{ "Quasars.quasarsColor",                                "Quasars/marker_color",                                      "" },
	{ "Satellites.invisibleSatelliteColor",                  "Satellites/invisible_satellite_color",                      "" },
	{ "Satellites.transitSatelliteColor",                    "Satellites/transit_satellite_color",                        "" },
	{ "Satellites.umbraColor",                               "Satellites/umbra_color",                                    "" },
	{ "Satellites.penumbraColor",                            "Satellites/penumbra_color",                                 "" },
	{ "TelescopeControl.reticleColor",                       "TelescopeControl/color_telescope_reticles",                 "" },
	{ "TelescopeControl.labelColor",                         "TelescopeControl/color_telescope_labels",                   "" },
	{ "TelescopeControl.circleColor",                        "TelescopeControl/color_telescope_circles",                  "" },
};

QString formatLatLon(double degrees, QChar positive, QChar negative)
{
	const QChar hemisphere = (degrees < 0.) ? negative : positive;
	double v = std::fabs(degrees);
	const int d = static_cast<int>(v);
	v = (v - d) * 60.;
	const int m = static_cast<int>(v);
	const int s = static_cast<int>((v - m) * 60. + 0.5);
	return QString("%1°%2'%3\"%4")
	       .arg(d).arg(m, 2, 10, QChar('0')).arg(s, 2, 10, QChar('0')).arg(hemisphere);
}

QString mapAssetFor(const QString& planetName)
{
	if (planetName == QStringLiteral("Earth"))
		return QStringLiteral("gui/miscWorldMap.jpg");
	if (SolarSystem* ssystem = GETSTELMODULE(SolarSystem))
	{
		const PlanetP planet = ssystem->searchByEnglishName(planetName);
		if (planet && !planet->getTextMapName().isEmpty())
			return QStringLiteral("textures/") + planet->getTextMapName();
	}
	return QString();
}

bool isObserverPlanet(const QString& planetName)
{
	if (SolarSystem* ssystem = GETSTELMODULE(SolarSystem))
	{
		const PlanetP planet = ssystem->searchByEnglishName(planetName);
		return planet && planet->getPlanetType() == Planet::isObserver;
	}
	return false;
}

QString plainText(QString html)
{
	const auto ci = QRegularExpression::CaseInsensitiveOption;
	html.replace(QRegularExpression("</t[dh]>\\s*<t[dh][^>]*>", ci), "\t");
	html.replace(QRegularExpression("<br\\s*/?>", ci), "\n");
	html.replace(QRegularExpression("</(tr|table|p|div|h[1-6]|li)>", ci), "\n");
	html.replace(QRegularExpression("</t[dh]>", ci), "\n");
	html.remove(QRegularExpression("<[^>]*>"));
	html.replace("&deg;", "°").replace("&nbsp;", " ").replace("&amp;", "&");
	html.replace(QChar(0x00A0), QChar(' '));
	html.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"");
	html.replace(QRegularExpression("[ \\t]*\\n[ \\t]*\\n+"), "\n");
	return html.trimmed();
}

QJsonObject infoRow(const QString& line)
{
	QJsonObject row;
	const int tab = line.indexOf('\t');
	const int split = tab > 0 ? tab : line.indexOf(':');
	if (split > 0)
	{
		row["k"] = line.left(split).trimmed();
		row["v"] = line.mid(split + 1).trimmed();
	}
	else
	{
		row["k"] = QString();
		row["v"] = line.trimmed();
	}
	return row;
}

QString colorToHex(const Vec3f& color)
{
	const auto byte = [](float c) { return static_cast<int>(std::lround(qBound(0.f, c, 1.f) * 255.f)); };
	return QString("#%1%2%3")
	       .arg(byte(color[0]), 2, 16, QChar('0'))
	       .arg(byte(color[1]), 2, 16, QChar('0'))
	       .arg(byte(color[2]), 2, 16, QChar('0'));
}

Vec3f hexToColor(const QString& hex)
{
	const QColor parsed(hex.trimmed());
	if (!parsed.isValid())
		return Vec3f(1.f, 1.f, 1.f);
	return Vec3f(static_cast<float>(parsed.redF()),
	             static_cast<float>(parsed.greenF()),
	             static_cast<float>(parsed.blueF()));
}

QString badgeFor(const QString& type)
{
	if (type.contains("Planet", Qt::CaseInsensitive))    return "Pl";
	if (type.contains("Star", Qt::CaseInsensitive))      return "St";
	if (type.contains("Galaxy", Qt::CaseInsensitive))    return "Gx";
	if (type.contains("Nebula", Qt::CaseInsensitive))    return "Neb";
	if (type.contains("Cluster", Qt::CaseInsensitive))   return "Cl";
	if (type.contains("Satellite", Qt::CaseInsensitive)) return "Sat";
	return type.left(2);
}

QString designationOf(const StelObjectP& obj)
{
	if (obj.isNull())
		return QString();
	const QString id = obj->getID().trimmed();
	if (!id.isEmpty())
		return id;
	if (const Nebula* nebula = dynamic_cast<const Nebula*>(obj.data()))
		return nebula->getDSODesignationWIC().trimmed();
	return QString();
}

QString magnitudeText(const StelObjectP& obj, const StelCore* core)
{
	if (obj.isNull())
		return QString();
	const float mag = obj->getVMagnitude(core);
	if (mag > 90.f || std::isnan(mag))
		return QString();
	return QString::number(static_cast<double>(mag), 'f', 2);
}

using AsteriumAstroCalc::kDash;
using AsteriumAstroCalc::culminationElevation;
using AsteriumAstroCalc::transitStrings;

QPair<QString, QString> positionCoordinates(const Vec3d& coord, bool horizontal, bool southAzimuth,
                                            bool decimalDegrees, bool polarDistance)
{
	return AsteriumAstroCalc::coordinates(coord, horizontal, southAzimuth, decimalDegrees, polarDistance);
}

QString elongationString(double radians, bool decimalDegrees)
{
	return AsteriumAstroCalc::angleString(radians, decimalDegrees);
}

QJsonArray positionCategories(StelObjectMgr& objects)
{
	QList<QPair<QString, QString>> items;
	const QMap<QString, QString> modules = objects.objectModulesMap();
	for (auto it = modules.constBegin(); it != modules.constEnd(); ++it)
	{
		QString key = it.key();
		if (key.startsWith("NebulaMgr") && key.contains(":"))
			items.append(qMakePair(ct_(it.value()), key.remove("NebulaMgr:")));
		if (key.startsWith("StarMgr") && key.contains(":"))
		{
			const int kn = key.remove("StarMgr:").toInt();
			if (kn > 1 && kn <= 6)
				items.append(qMakePair(ct_(it.value()), QString::number(kn + 168)));
		}
	}
	items.append(qMakePair(ct_("Deep-sky objects"), QStringLiteral("169")));
	items.append(qMakePair(ct_("Solar system objects"), QStringLiteral("200")));
	items.append(qMakePair(ct_("Solar system objects: comets"), QStringLiteral("201")));
	items.append(qMakePair(ct_("Solar system objects: minor bodies"), QStringLiteral("202")));
	items.append(qMakePair(ct_("Solar system objects: planets"), QStringLiteral("203")));
	items.append(qMakePair(ct_("Almanac: Sun, Moon and naked-eye planets"), QStringLiteral("204")));
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

void positionColumnLabels(int celTypeId, bool horizontal, QJsonObject& out)
{
	out["lngLabel"] = horizontal ? ct_("Azimuth") : ct_("RA (J2000)");
	out["latLabel"] = horizontal
	                  ? ct_("Altitude")
	                  : (StelApp::getInstance().getFlagPolarDistanceUsage() ? ct_("PD (J2000)") : ct_("Dec (J2000)"));

	const bool opacity = (celTypeId == 12 || celTypeId == 102 || celTypeId == 111);
	out["magLabel"] = opacity ? ct_("Opac.") : ct_("Mag.");

	if (celTypeId >= 170 && celTypeId < 200)
		out["sizeLabel"] = QString();
	else if (celTypeId >= 200)
		out["sizeLabel"] = QString("%1, \"").arg(ct_("A.S."));
	else
		out["sizeLabel"] = QString("%1, '").arg(ct_("A.S."));

	if (celTypeId == 170)
		out["extraLabel"] = QString("%1, \"").arg(ct_("Sep."));
	else if (celTypeId == 171 || celTypeId == 173 || celTypeId == 174)
		out["extraLabel"] = QString("%1, %2").arg(ct_("Per."), qc_("d", "days"));
	else if (celTypeId == 172)
		out["extraLabel"] = QString("%1, %2").arg(ct_("P.M."), qc_("\"/yr", "arc-second per year"));
	else if (celTypeId >= 200)
		out["extraLabel"] = QString("%1, %2").arg(ct_("Dist."), qc_("AU", "distance, astronomical unit"));
	else
		out["extraLabel"] = ct_("S.B.");
}

QJsonArray kEphemerisRows;

struct EphemerisStep { const char* label; int id; };
const EphemerisStep kEphemerisSteps[] = {
	{ "1 minute", 38 },        { "10 minutes", 1 },        { "30 minutes", 2 },
	{ "1 hour", 3 },           { "6 hours", 4 },           { "12 hours", 5 },
	{ "1 solar day", 6 },      { "3 solar days", 40 },     { "5 solar days", 7 },
	{ "10 solar days", 8 },    { "15 solar days", 9 },     { "30 solar days", 10 },
	{ "60 solar days", 11 },   { "100 solar days", 24 },   { "500 solar days", 37 },
	{ "1 sidereal day", 18 },  { "3 sidereal days", 39 },  { "5 sidereal days", 19 },
	{ "10 sidereal days", 20 },{ "15 sidereal days", 21 }, { "30 sidereal days", 22 },
	{ "60 sidereal days", 23 },{ "100 sidereal days", 25 },{ "500 sidereal days", 36 },
	{ "1 sidereal year", 27 }, { "1 Julian day", 12 },     { "5 Julian days", 13 },
	{ "10 Julian days", 14 },  { "15 Julian days", 15 },   { "30 Julian days", 16 },
	{ "60 Julian days", 17 },  { "100 Julian days", 26 },  { "1 Julian year", 28 },
	{ "1 Gaussian year", 29 }, { "1 synodic month", 30 },  { "1 draconic month", 31 },
	{ "1 mean tropical month", 32 }, { "1 anomalistic month", 33 },
	{ "1 anomalistic year", 34 },    { "1 tritos", 43 },   { "1 saros", 35 },
	{ "1 Metonic cycle", 44 }, { "1 inex", 45 },           { "custom interval", 0 },
	{ "Sun at preset altitude", AstroCalcDialog::EphemerisTimeStepSunAtAltitude },
	{ "Opposition of planet", AstroCalcDialog::EphemerisTimeStepOpposition },
};

struct EphemerisUnit { const char* label; const char* context; int id; };
const EphemerisUnit kEphemerisUnits[] = {
	{ "minutes", "time unit measurement", 1 }, { "hours",  "time unit measurement", 2 },
	{ "days",    "time unit measurement", 3 }, { "weeks",  "time unit measurement", 4 },
	{ "months",  "time unit measurement", 5 }, { "years",  "time unit measurement", 6 },
};

double ephemerisStepDays(int id, const PlanetP& observer, QSettings* conf)
{
	double solarDay = 1., siderealDay = 1., siderealYear = 365.256363004;
	if (!observer.isNull() && observer->getPlanetType() != Planet::isObserver)
	{
		solarDay = (observer->getEnglishName() == QLatin1String("Earth")) ? 1. : observer->getMeanSolarDay();
		siderealDay = observer->getSiderealDay();
		siderealYear = observer->getSiderealPeriod();
	}

	const QMap<int, double> customUnits = {
		{ 1, StelCore::JD_MINUTE },        { 2, StelCore::JD_HOUR },
		{ 3, solarDay },                   { 4, siderealDay },
		{ 5, StelCore::JD_DAY },           { 6, 29.530588853 * solarDay },
		{ 7, 27.212220817 * solarDay },    { 8, 27.321582241 * solarDay },
		{ 9, 27.554549878 * solarDay },    { 10, siderealYear },
		{ 11, 365.25 * solarDay },         { 12, 365.2568983 * solarDay },
		{ 13, 365.259636 * solarDay },     { 14, 6585.321314219 * solarDay },
		{ 15, 10571.950809374 * solarDay },{ 16, 3986.629495155 * solarDay },
		{ 17, 6939.688380455 * solarDay },
	};
	const double custom = conf->value("astrocalc/custom_time_step", 1.0).toDouble()
	                      * customUnits.value(conf->value("astrocalc/custom_time_step_unit", 3).toInt(), solarDay);

	const QMap<int, double> steps = {
		{ 0, custom },
		{ 1, 10. * StelCore::JD_MINUTE },  { 2, 30. * StelCore::JD_MINUTE },
		{ 3, StelCore::JD_HOUR },          { 4, 6. * StelCore::JD_HOUR },
		{ 5, 12. * StelCore::JD_HOUR },    { 6, solarDay },
		{ 7, 5. * solarDay },              { 8, 10. * solarDay },
		{ 9, 15. * solarDay },             { 10, 30. * solarDay },
		{ 11, 60. * solarDay },            { 12, StelCore::JD_DAY },
		{ 13, 5. * StelCore::JD_DAY },     { 14, 10. * StelCore::JD_DAY },
		{ 15, 15. * StelCore::JD_DAY },    { 16, 30. * StelCore::JD_DAY },
		{ 17, 60. * StelCore::JD_DAY },    { 18, siderealDay },
		{ 19, 5. * siderealDay },          { 20, 10. * siderealDay },
		{ 21, 15. * siderealDay },         { 22, 30. * siderealDay },
		{ 23, 60. * siderealDay },         { 24, 100. * solarDay },
		{ 25, 100. * siderealDay },        { 26, 100. * StelCore::JD_DAY },
		{ 27, siderealYear * solarDay },   { 28, 365.25 * solarDay },
		{ 29, 365.2568983 * solarDay },    { 30, 29.530588853 * solarDay },
		{ 31, 27.212220817 * solarDay },   { 32, 27.321582241 * solarDay },
		{ 33, 27.554549878 * solarDay },   { 34, 365.259636 * solarDay },
		{ 35, 6585.321314219 * solarDay }, { 36, 500. * siderealDay },
		{ 37, 500. * solarDay },           { 38, StelCore::JD_MINUTE },
		{ 39, 3. * siderealDay },          { 40, 3. * solarDay },
		{ 43, 3986.629495155 * solarDay }, { 44, 6939.688380455 * solarDay },
		{ 45, 10571.950809374 * solarDay },
	};
	return steps.value(id, solarDay);
}

double ephemerisUnitDays(int id)
{
	const QMap<int, double> units = {
		{ 1, StelCore::JD_MINUTE }, { 2, StelCore::JD_HOUR }, { 3, StelCore::JD_DAY },
		{ 4, 7. }, { 5, 30.4375 },
		{ 6, 365.25 },
	};
	return units.value(id, 30.4375);
}

using AsteriumAstroCalc::parseLocalTime;

bool findSunAtAltitude(StelCore* core, const PlanetP& sun, double dayJD, double targetAltDeg,
                       bool evening, double& resultJD)
{
	const auto sunSinAltitude = [&](double jd)
	{
		core->setJD(jd);
		core->update(0);
		Vec3d altAz = sun->getAltAzPosAuto(core);
		altAz.normalize();
		return altAz[2];
	};
	const double target = std::sin(targetAltDeg * M_PI_180);

	const double longitude = static_cast<double>(core->getCurrentLocation().getLongitude());
	const double localNoon = dayJD + 0.5 - longitude / 360.;
	const double first = evening ? localNoon : localNoon - 0.5;
	const double last = evening ? localNoon + 0.5 : localNoon;

	double atFirst = sunSinAltitude(first) - target;
	const double atLast = sunSinAltitude(last) - target;

	if (atFirst * atLast > 0.)
		return false;

	double low = first, high = last;
	for (int i = 0; i < 30; ++i)
	{
		const double middle = 0.5 * (low + high);
		const double atMiddle = sunSinAltitude(middle) - target;
		if (atMiddle * atFirst <= 0.)
			high = middle;
		else
		{
			low = middle;
			atFirst = atMiddle;
		}
	}
	resultJD = 0.5 * (low + high);
	return true;
}

bool findNextOpposition(StelCore* core, const PlanetP& sun, const PlanetP& planet,
                        double startJD, double endJD, double& resultJD)
{
	const auto metric = [&](double jd)
	{
		core->setJD(jd);
		core->update(0);
		return M_PI - planet->getJ2000EquatorialPos(core).angle(sun->getJ2000EquatorialPos(core));
	};

	const double step = 1.;
	double jd = startJD - step * 0.5;
	double previous = metric(jd);
	int previousSign = 0;
	jd += step;

	while (jd <= endJD)
	{
		const double value = metric(jd);
		const int sign = (value > previous) ? 1 : ((value < previous) ? -1 : 0);
		if (sign != previousSign && previousSign == -1)
		{
			double refined = jd, refineStep = -step / 2.;
			int refineSign = -sign;
			double refinePrevious = value;
			for (int i = 0; i < 30; ++i)
			{
				refined += refineStep;
				const double refinedValue = metric(refined);
				const int refinedSign = (refinedValue > refinePrevious)
				                        ? 1 : ((refinedValue < refinePrevious) ? -1 : 0);
				if (refinedSign != refineSign)
				{
					refineStep = -refineStep / 2.;
					refineSign = -refinedSign;
				}
				refinePrevious = refinedValue;
				if (std::fabs(refineStep) < 1. / 1440.)
					break;
			}

			if (metric(refined) < 10. * M_PI_180)
			{
				resultJD = refined;
				return true;
			}
		}
		previous = value;
		previousSign = sign;
		jd += step;
	}
	return false;
}

using AsteriumAstroCalc::bodies;

void generateEphemeris(StelApp& app, StelCore* core, const QString& startText)
{
	SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
	QSettings* conf = app.getSettings();
	kEphemerisRows = QJsonArray();
	AstroCalcDialog::EphemerisList.clear();
	AstroCalcDialog::DisplayedPositionIndex = -1;
	if (!solarSystem)
		return;

	const QString primary = conf->value("astrocalc/ephemeris_celestial_body", "Moon").toString();
	const QString secondary = conf->value("astrocalc/ephemeris_second_celestial_body", "none").toString();

	const bool horizon = conf->value("astrocalc/flag_ephemeris_horizontal_coordinates", false).toBool();
	const bool ignoreDateTest = conf->value("astrocalc/flag_ephemeris_ignore_date_test", true).toBool();
	const bool southAzimuth = app.getFlagSouthAzimuthUsage();
	const bool decimalDegrees = app.getFlagShowDecimalDegrees();
	const bool polarDistance = app.getFlagPolarDistanceUsage();

	const PlanetP here = core->getCurrentPlanet();
	const PlanetP sun = solarSystem->getSun();
	const double currentJD = core->getJD();
	const double firstJD = parseLocalTime(startText, core);
	const int stepId = conf->value("astrocalc/ephemeris_time_step", 6).toInt();

	const bool sunAltitudeMode = (stepId == AstroCalcDialog::EphemerisTimeStepSunAtAltitude);
	const bool oppositionMode = (stepId == AstroCalcDialog::EphemerisTimeStepOpposition);
	const double stepDays = (sunAltitudeMode || oppositionMode)
	                        ? 1. : ephemerisStepDays(stepId, here, conf);
	const double spanDays = conf->value("astrocalc/ephemeris_time_duration", 1).toInt()
	                        * ephemerisUnitDays(conf->value("astrocalc/ephemeris_time_unit", 5).toInt());
	const int elements = stepDays > 0. ? static_cast<int>(spanDays / stepDays) : 0;
	const double lastJD = firstJD + spanDays;

	const double sunAltitude = conf->value("astrocalc/ephemeris_sun_altitude", -10.0).toDouble();

	const bool sunAltEvening = conf->value("astrocalc/ephemeris_sun_altitude_evening", 0).toInt() == 0;

	QVector<double> oppositions;
	if (oppositionMode)
	{
		const PlanetP target = solarSystem->searchByEnglishName(
				conf->value("astrocalc/ephemeris_opposition_planet", "Mars").toString());
		double search = firstJD;
		while (!target.isNull() && search < lastJD)
		{
			double found = 0.;
			if (!findNextOpposition(core, sun, target, search, lastJD, found))
				break;
			oppositions.append(found);

			search = found + 30.;
		}
	}

	QVector<PlanetP> bodies;
	QVector<int> colours;
	const bool nakedEye = conf->value("astrocalc/ephemeris_nakedeye_planets", false).toBool()
	                      && here == solarSystem->getEarth();
	if (nakedEye)
	{
		const QList<QPair<QString, int>> nakedEyePlanets = {
			{ "Mercury", 2 }, { "Venus", 3 }, { "Mars", 4 }, { "Jupiter", 5 }, { "Saturn", 6 }
		};
		for (const auto& entry : nakedEyePlanets)
		{
			bodies.append(solarSystem->searchByEnglishName(entry.first));
			colours.append(entry.second);
		}
	}
	else
	{
		bodies.append(solarSystem->searchByEnglishName(primary));
		colours.append(0);
		if (secondary != QLatin1String("none"))
		{
			bodies.append(solarSystem->searchByEnglishName(secondary));
			colours.append(1);
		}
	}

	AstroCalcDialog::EphemerisList.reserve(static_cast<qsizetype>(elements + 1) * bodies.size());
	app.getStelPropertyManager()->setStelPropertyValue("SolarSystem.ephemerisDataLimit", bodies.size());

	for (int b = 0; b < bodies.size(); ++b)
	{
		const PlanetP obj = bodies.at(b);
		if (obj.isNull())
			continue;
		const QString englishName = obj->getEnglishName();
		const QString name = obj->getNameI18n();
		const bool isSun = (obj == sun);

		const int count = oppositionMode ? oppositions.size() : (elements + 1);
		for (int i = 0; i < count; ++i)
		{
			double jd = firstJD + i * stepDays;
			if (oppositionMode)
				jd = oppositions.at(i);
			else if (sunAltitudeMode)
			{
				int year = 0, month = 0, day = 0;
				StelUtils::getDateFromJulianDay(firstJD + i, &year, &month, &day);
				double dayJD = 0.;
				StelUtils::getJDFromDate(&dayJD, year, month, day, 0, 0, 0.f);
				if (!findSunAtAltitude(core, sun, dayJD, sunAltitude, sunAltEvening, jd))
					continue;
			}
			core->setJD(jd);

			for (const PlanetP& p : solarSystem->getAllPlanets())
			{
				QSharedPointer<MinorPlanet> mp = p.dynamicCast<MinorPlanet>();
				if (mp && mp->hasEpochElements())
					mp->updateEpochOrbit(jd, true);
			}
			core->update(0);

			if (!ignoreDateTest && !obj->hasValidPositionalData(jd, Planet::PositionQuality::OrbitPlotting))
				continue;

			const Vec3d pos = horizon ? obj->getAltAzPosAuto(core) : obj->getJ2000EquatorialPos(core);
			const Vec3d sunPos = horizon ? sun->getAltAzPosAuto(core) : sun->getJ2000EquatorialPos(core);

			Ephemeris item;
			item.coord = pos;
			item.sunCoord = sunPos;
			item.colorIndex = colours.at(b);
			item.objDate = jd;
			item.magnitude = obj->getVMagnitudeWithExtinction(core);
			item.isComet = obj->getPlanetType() == Planet::isComet;
			item.sso = obj;
			AstroCalcDialog::EphemerisList.append(item);

			const QPair<QString, QString> coords = positionCoordinates(
					pos, horizon, southAzimuth, decimalDegrees, polarDistance);
			const Vec3d observerPos = core->getObserverHeliocentricEclipticPos();

			QJsonObject row;

			row["index"] = AstroCalcDialog::EphemerisList.size() - 1;
			row["name"] = name;
			row["select"] = englishName;
			row["jd"] = jd;
			row["when"] = asteriumFormatSimTime(jd, "yyyy-MM-dd HH:mm:ss");
			row["lng"] = coords.first;
			row["lat"] = coords.second;
			row["mag"] = QString::number(obj->getVMagnitudeWithExtinction(core), 'f', 2);
			row["phase"] = isSun ? kDash
			                     : QString("%1%").arg(QString::number(obj->getPhase(observerPos) * 100., 'f', 2));
			row["dist"] = QString::number(obj->getJ2000EquatorialPos(core).norm(), 'f', 6);
			row["elong"] = isSun ? kDash
			                     : elongationString(obj->getElongation(observerPos), decimalDegrees);
			kEphemerisRows.append(row);
		}
	}

	core->setJD(currentJD);
	core->update(0);

	QList<QJsonValue> sorted;
	for (const QJsonValue& row : std::as_const(kEphemerisRows))
		sorted.append(row);
	std::sort(sorted.begin(), sorted.end(), [](const QJsonValue& a, const QJsonValue& b)
	          { return a.toObject().value("jd").toDouble() < b.toObject().value("jd").toDouble(); });
	kEphemerisRows = QJsonArray();
	for (const QJsonValue& row : std::as_const(sorted))
		kEphemerisRows.append(row);

	QMetaObject::invokeMethod(solarSystem, "requestEphemerisVisualization", Qt::DirectConnection);
}

struct SearchFlag { const char* name; const char* key; bool fallback; };
const SearchFlag kSearchFlags[] = {
	{ "simbad",       "flag_search_online",      true  },
	{ "startOfWords", "flag_start_words",        false },
	{ "lengthSort",   "flag_sorting_length",     false },
	{ "lockPosition", "flag_lock_position",      true  },
	{ "fovMarker",    "flag_fov_center_marker",  true  },
	{ "autoClose",    "flag_auto_closing",       true  },
	{ "ids",          "simbad_query_IDs",        true  },
	{ "types",        "simbad_query_types",      false },
	{ "spectrum",     "simbad_query_spec",       false },
	{ "morphology",   "simbad_query_morpho",     false },
	{ "dimensions",   "simbad_query_dimensions", false },
	{ "english",      "list_names_english",      false },
};

struct SearchNumber { const char* name; const char* key; int fallback; };
const SearchNumber kSearchNumbers[] = {
	{ "radius",     "simbad_query_dist",  30 },
	{ "results",    "simbad_query_count",  3 },
	{ "recentSize", "recentSearchSize",   20 },
};

struct SearchText { const char* name; const char* key; const char* fallback; };
const SearchText kSearchTexts[] = {
	{ "system", "coordinate_system",  "equatorialJ2000" },
	{ "server", "simbad_server_url",  SearchDialog::DEF_SIMBAD_URL },
};

QSettings* config()
{
	return StelApp::getInstance().getSettings();
}

bool searchFlag(const char* key, bool fallback)
{
	return config()->value(QString("search/%1").arg(key), fallback).toBool();
}

int searchNumber(const char* key, int fallback)
{
	return config()->value(QString("search/%1").arg(key), fallback).toInt();
}

QString searchText(const char* key, const char* fallback)
{
	return config()->value(QString("search/%1").arg(key), QString(fallback)).toString();
}

QStringList recentSearches()
{
	return config()->value("search/recent_objects").toStringList();
}

void rememberSearch(const QString& name)
{
	if (name.trimmed().isEmpty())
		return;
	QStringList recent = recentSearches();
	recent.removeAll(name);
	recent.prepend(name);
	const int limit = qMax(1, searchNumber("recentSearchSize", 20));
	while (recent.size() > limit)
		recent.removeLast();
	config()->setValue("search/recent_objects", recent);
}

void moveToSelection()
{
	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	const QList<StelObjectP> selected = app.getStelObjectMgr().getSelectedObject();
	StelMovementMgr* movement = core ? core->getMovementMgr() : nullptr;
	if (selected.isEmpty() || !movement)
		return;
	if (selected.first()->getEnglishName() == core->getCurrentLocation().planetName)
	{
		app.getStelObjectMgr().unSelect();
		return;
	}
	asteriumMoveToSelected(movement, selected.first(), movement->getAutoMoveDuration());
}

QJsonObject viewCentre(const QString& system)
{
	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	const auto projector = core->getProjection(StelCore::FrameJ2000, StelCore::RefractionOff);
	const Vector2<qreal> middle = projector->getViewportCenter();
	Vec3d centre;
	projector->unProject(middle[0], middle[1], centre);

	double longitude = 0., latitude = 0.;
	bool hours = false;
	if (system == QLatin1String("horizontal"))
	{
		StelUtils::rectToSphe(&longitude, &latitude, core->j2000ToAltAz(centre, StelCore::RefractionAuto));
		longitude = StelUtils::fmodpos(3. * M_PI - longitude
		                               + (app.getFlagSouthAzimuthUsage() ? M_PI : 0.), 2. * M_PI);
	}
	else if (system == QLatin1String("galactic"))
		StelUtils::rectToSphe(&longitude, &latitude, core->j2000ToGalactic(centre));
	else if (system == QLatin1String("supergalactic"))
		StelUtils::rectToSphe(&longitude, &latitude, core->j2000ToSupergalactic(centre));
	else if (system == QLatin1String("ecliptic") || system == QLatin1String("eclipticJ2000"))
	{
		const bool ofDate = system == QLatin1String("ecliptic");
		double ra = 0., dec = 0.;
		StelUtils::rectToSphe(&ra, &dec, ofDate ? core->j2000ToEquinoxEqu(centre, StelCore::RefractionOff)
		                                        : centre);
		StelUtils::equToEcl(ra, dec, GETSTELMODULE(SolarSystem)->getEarth()->getRotObliquity(
		                            ofDate ? core->getJDE() : 2451545.0), &longitude, &latitude);
		longitude = StelUtils::fmodpos(longitude, 2. * M_PI);
	}
	else
	{
		StelUtils::rectToSphe(&longitude, &latitude,
		                      system == QLatin1String("equatorial")
		                      ? core->j2000ToEquinoxEqu(centre, StelCore::RefractionOff) : centre);
		if (app.getFlagPolarDistanceUsage())
			latitude = M_PI_2 - latitude;
		hours = true;
	}

	const auto write = [&app](double radians, bool asHours)
	{
		if (app.getFlagShowDecimalDegrees())
			return QString::number(radians * 180. / M_PI, 'f', 5);
		return asHours ? StelUtils::radToHmsStr(radians, true).trimmed()
		               : StelUtils::radToDmsStr(radians, true, true).trimmed();
	};
	QJsonObject out;
	out["x"] = write(longitude, hours);
	out["y"] = write(latitude, false);
	return out;
}

void gotoPosition(const QString& request)
{
	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	StelMovementMgr* movement = core ? core->getMovementMgr() : nullptr;
	const QStringList parts = request.split('|');
	if (!movement || parts.size() < 3)
		return;

	const QString system = parts.at(0);
	const double longitude = StelUtils::getDecAngle(parts.at(1));
	double latitude = StelUtils::getDecAngle(parts.at(2));
	if (app.getFlagPolarDistanceUsage() && system.startsWith(QLatin1String("equatorial")))
		latitude = M_PI_2 - latitude;

	double aimLongitude = longitude;
	Vec3d position;
	if (system == QLatin1String("horizontal"))
	{
		aimLongitude = 3. * M_PI - longitude - (app.getFlagSouthAzimuthUsage() ? M_PI : 0.);
		StelUtils::spheToRect(aimLongitude, latitude, position);
		position = core->altAzToJ2000(position, StelCore::RefractionOff);
		core->setTimeRate(0.);
	}
	else if (system == QLatin1String("galactic"))
	{
		StelUtils::spheToRect(longitude, latitude, position);
		position = core->galacticToJ2000(position);
	}
	else if (system == QLatin1String("supergalactic"))
	{
		StelUtils::spheToRect(longitude, latitude, position);
		position = core->supergalacticToJ2000(position);
	}
	else if (system == QLatin1String("ecliptic") || system == QLatin1String("eclipticJ2000"))
	{
		const bool ofDate = system == QLatin1String("ecliptic");
		double ra = 0., dec = 0.;
		StelUtils::eclToEqu(longitude, latitude,
		                    GETSTELMODULE(SolarSystem)->getEarth()->getRotObliquity(
		                            ofDate ? core->getJDE() : 2451545.0), &ra, &dec);
		StelUtils::spheToRect(ra, dec, position);
		if (ofDate)
			position = core->equinoxEquToJ2000(position, StelCore::RefractionOff);
	}
	else
	{
		StelUtils::spheToRect(longitude, latitude, position);
		if (system == QLatin1String("equatorial"))
			position = core->equinoxEquToJ2000(position, StelCore::RefractionOff);
	}

	const StelMovementMgr::MountMode mount = movement->getMountMode();
	const bool mountFrame =
	        (system.startsWith(QLatin1String("equatorial")) && mount == StelMovementMgr::MountEquinoxEquatorial)
	        || (system == QLatin1String("horizontal") && mount == StelMovementMgr::MountAltAzimuthal)
	        || (system == QLatin1String("galactic") && mount == StelMovementMgr::MountGalactic)
	        || (system == QLatin1String("supergalactic") && mount == StelMovementMgr::MountSupergalactic);
	movement->setViewUpVector(Vec3d(0., 0., 1.));
	if (mountFrame && std::fabs(latitude) > 0.9 * M_PI_2)
	{
		movement->setViewUpVector(Vec3d(-std::cos(aimLongitude), -std::sin(aimLongitude), 0.)
		                          * (latitude > 0. ? 1. : -1.));
	}

	movement->setFlagTracking(false);
	movement->moveToJ2000(position, movement->getViewUpVectorJ2000(), movement->getAutoMoveDuration());
	movement->setFlagLockEquPos(searchFlag("flag_lock_position", true));
}

QJsonObject searchHit(const QString& name, const StelObjectP& obj, const StelCore* core)
{
	QJsonObject hit;
	hit["name"] = name;
	hit["badge"] = obj.isNull() ? QStringLiteral("?") : badgeFor(obj->getType());
	hit["mag"] = magnitudeText(obj, core);
	const QString english = obj.isNull() ? QString() : obj->getEnglishName();
	const QString type = obj.isNull() ? QString() : obj->getObjectType();
	hit["sub"] = english.isEmpty() || english == name ? type : QString("%1 · %2").arg(english, type);
	return hit;
}

void rememberColor(const QString& property, const Vec3f& colour)
{
	for (const ColorKey& entry : kColorKeys)
	{
		if (property != QLatin1String(entry.prop))
			continue;
		const QString ini = QString::fromUtf8(entry.ini);
		const QString text = Vec3d(colour[0], colour[1], colour[2]).toStr();
		if (*entry.module == '\0')
		{
			StelApp::getInstance().getSettings()->setValue(ini, text);
			return;
		}
		if (StelModule* module = StelApp::getInstance().getModuleMgr().getModule(entry.module, true))
		{
			if (QSettings* settings = module->getSettings())
				settings->setValue(ini, text);
		}
		return;
	}
}

const char* const kObsLists   = "observingLists";
const char* const kObsDefault = "defaultListOlud";
const char* const kObsObjects = "objects";
const char* const kObsName    = "name";
const char* const kObsEdited  = "last edit";

struct ObservingLists
{
	QJsonObject doc;
	bool loaded = false;
	QString olud;
	QSet<QString> members;
	QList<int> labels;
	bool marked = false;
	bool readOnly = false;
};
ObservingLists obs;

QString obsListPath()
{
	const QString configured = config()->value("main/observinglists_dir", "data").toString();
	QString dir = StelFileMgr::findFile(configured, static_cast<StelFileMgr::Flags>(
	                                    StelFileMgr::Directory | StelFileMgr::Writable));
	if (dir.isEmpty())
	{
		dir = QDir::isAbsolutePath(configured) ? configured
		                                       : StelFileMgr::getUserDir() + "/" + configured;
		QDir().mkpath(dir);
	}
	return dir + "/observingList.json";
}

QString obsTimestamp()
{
	const double jd = StelUtils::getJDFromSystem();
	const StelCore* core = StelApp::getInstance().getCore();
	return StelUtils::julianDayToISO8601String(jd + (core ? core->getUTCOffset(jd) : 0.) / 24.)
	       .replace('T', ' ');
}

QJsonObject obsNewList(const QString& name)
{
	QJsonObject list;
	list[kObsName] = name;
	list["description"] = QString();
	list["sorting"] = QString();
	list["creation date"] = obsTimestamp();
	list[kObsEdited] = obsTimestamp();
	list[kObsObjects] = QJsonArray();
	return list;
}

QJsonObject obsAllLists()
{
	return obs.doc.value(QLatin1String(kObsLists)).toObject();
}

QJsonObject obsCurrentList()
{
	return obsAllLists().value(obs.olud).toObject();
}

QString obsPositionKey(const StelObjectP& obj)
{
	StelCore* core = StelApp::getInstance().getCore();
	if (!core)
		return QString();
	double ra = 0., dec = 0.;
	StelUtils::rectToSphe(&ra, &dec, obj->getJ2000EquatorialPos(core));
	const int raMinutes = static_cast<int>(StelUtils::fmodpos(ra, 2. * M_PI) * (720. / M_PI)) % 1440;
	const double decDegrees = dec * (180. / M_PI);
	const int decMinutes = static_cast<int>(qAbs(decDegrees) * 60.);
	return QString("%1h%2m %3%4°%5'")
	       .arg(raMinutes / 60, 2, 10, QChar('0'))
	       .arg(raMinutes % 60, 2, 10, QChar('0'))
	       .arg(decDegrees < 0. ? QStringLiteral("-") : QStringLiteral("+"))
	       .arg(decMinutes / 60, 2, 10, QChar('0'))
	       .arg(decMinutes % 60, 2, 10, QChar('0'));
}

QString obsKey(const StelObjectP& obj)
{
	if (obj.isNull())
		return QString();
	QString key;
	if (const Nebula* nebula = dynamic_cast<const Nebula*>(obj.data()))
		key = nebula->getDSODesignationWIC().trimmed();
	if (key.isEmpty())
		key = designationOf(obj);
	if (key.isEmpty())
		key = obj->getEnglishName();
	if (key.isEmpty())
		key = obsPositionKey(obj);
	return key.isEmpty() ? QStringLiteral("Unnamed object") : key;
}

void obsRefreshMembers()
{
	obs.members.clear();
	const QJsonArray objects = obsCurrentList().value(QLatin1String(kObsObjects)).toArray();
	for (const QJsonValue& entry : objects)
		obs.members.insert(entry.toObject().value("designation").toString());
}

void obsLoad()
{
	if (obs.loaded)
		return;
	obs.loaded = true;
	QFile file(obsListPath());
	if (file.exists())
	{
		QJsonParseError problem{};
		if (file.open(QIODevice::ReadOnly))
			obs.doc = QJsonDocument::fromJson(file.readAll(), &problem).object();
		obs.readOnly = !file.isOpen() || problem.error != QJsonParseError::NoError;
		if (obs.readOnly)
			qWarning() << "Asterium: leaving" << file.fileName()
			           << "alone, it cannot be read:" << problem.errorString();
	}

	QJsonObject lists = obsAllLists();
	if (lists.isEmpty())
	{
		const QString olud = QUuid::createUuid().toString();
		lists.insert(olud, obsNewList(ct_("Favorites")));
		obs.doc.insert(QLatin1String(kObsLists), lists);
		obs.doc.insert(QLatin1String(kObsDefault), olud);
	}
	obs.olud = obs.doc.value(QLatin1String(kObsDefault)).toString();
	if (!lists.contains(obs.olud))
		obs.olud = lists.keys().first();
	obsRefreshMembers();
}

void obsSave()
{
	if (obs.readOnly)
		return;
	obs.doc.insert(QLatin1String(kObsDefault), obs.olud);
	obs.doc.insert("shortName", QStringLiteral("Observing list for Stellarium"));
	obs.doc.insert("version", QStringLiteral("2.1"));
	QFile file(obsListPath());
	if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate))
	{
		qWarning() << "Asterium: cannot write observing list to" << file.fileName();
		return;
	}
	file.write(QJsonDocument(obs.doc).toJson(QJsonDocument::Indented));
}

void obsHighlight(bool on);

void obsRedrawMarks()
{
	if (obs.marked)
		obsHighlight(true);
}

void obsPutCurrent(QJsonObject list)
{
	list[kObsEdited] = obsTimestamp();
	QJsonObject lists = obsAllLists();
	lists.insert(obs.olud, list);
	obs.doc.insert(QLatin1String(kObsLists), lists);
	obsSave();
	obsRefreshMembers();
	obsRedrawMarks();
}

void obsSelectList(const QString& olud)
{
	obsLoad();
	if (!obsAllLists().contains(olud))
		return;
	obs.olud = olud;
	obsSave();
	obsRefreshMembers();
	obsRedrawMarks();
}

bool obsAdd(const StelObjectP& obj)
{
	obsLoad();
	StelCore* core = StelApp::getInstance().getCore();
	if (obj.isNull() || !core)
		return false;
	const QString designation = obsKey(obj);
	if (obs.members.contains(designation))
		return false;

	QJsonObject item;
	item["designation"] = designation;
	item[kObsName] = obj->getEnglishName();
	item["nameI18n"] = obj->getNameI18n();
	item["type"] = obj->getType();
	item["objtype"] = obj->getObjectTypeI18n();
	item["magnitude"] = magnitudeText(obj, core);
	item["constellation"] = core->getIAUConstellation(obj->getEquinoxEquatorialPos(core));
	if (obj->getType() != QLatin1String("Planet"))
	{
		double ra = 0., dec = 0.;
		StelUtils::rectToSphe(&ra, &dec, obj->getJ2000EquatorialPos(core));
		item["ra"] = StelUtils::radToHmsStr(ra, false).trimmed();
		item["dec"] = StelUtils::radToDmsStr(dec, false).trimmed();
	}
	item["jd"] = 0;
	item["fov"] = 0;
	item["location"] = QString();
	item["landscapeID"] = QString();
	item["isVisibleMarker"] = !designation.contains("marker", Qt::CaseInsensitive);

	QJsonObject list = obsCurrentList();
	QJsonArray objects = list.value(QLatin1String(kObsObjects)).toArray();
	objects.append(item);
	list[kObsObjects] = objects;
	obsPutCurrent(list);
	return true;
}

bool obsAddSelected()
{
	const QList<StelObjectP> selected =
	        StelApp::getInstance().getStelObjectMgr().getSelectedObject();
	return obsAdd(selected.isEmpty() ? StelObjectP() : selected.first());
}

void obsRemoveAt(int index)
{
	obsLoad();
	QJsonObject list = obsCurrentList();
	QJsonArray objects = list.value(QLatin1String(kObsObjects)).toArray();
	if (index < 0 || index >= objects.size())
		return;
	objects.removeAt(index);
	list[kObsObjects] = objects;
	obsPutCurrent(list);
}

int obsIndexOf(const QString& designation)
{
	const QJsonArray objects = obsCurrentList().value(QLatin1String(kObsObjects)).toArray();
	for (int i = 0; i < objects.size(); ++i)
	{
		if (objects.at(i).toObject().value("designation").toString() == designation)
			return i;
	}
	return -1;
}

int obsIndexOfObject(const StelObjectP& obj)
{
	if (obj.isNull())
		return -1;
	const int at = obsIndexOf(obsKey(obj));
	if (at >= 0 || obj->getEnglishName().isEmpty())
		return at;
	return obsIndexOf(obj->getEnglishName());
}

bool obsHolds(const StelObjectP& obj)
{
	if (obj.isNull())
		return false;
	return obs.members.contains(obsKey(obj))
	       || (!obj->getEnglishName().isEmpty() && obs.members.contains(obj->getEnglishName()));
}

void obsGoTo(int index)
{
	obsLoad();
	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	StelMovementMgr* movement = core ? core->getMovementMgr() : nullptr;
	const QJsonArray objects = obsCurrentList().value(QLatin1String(kObsObjects)).toArray();
	if (!movement || index < 0 || index >= objects.size())
		return;
	const QJsonObject item = objects.at(index).toObject();

	StelObjectMgr& mgr = app.getStelObjectMgr();
	const QString designation = item.value("designation").toString();
	if (!designation.isEmpty() && mgr.findAndSelect(designation))
	{
		const QList<StelObjectP> found = mgr.getSelectedObject();
		if (!found.isEmpty())
		{
			asteriumMoveToSelected(movement, found.first(), movement->getAutoMoveDuration());
			return;
		}
	}

	const QString ra = item.value("ra").toString();
	const QString dec = item.value("dec").toString();
	if (ra.isEmpty() || dec.isEmpty())
		return;
	Vec3d position;
	StelUtils::spheToRect(StelUtils::getDecAngle(ra), StelUtils::getDecAngle(dec), position);
	movement->setFlagTracking(false);
	movement->moveToJ2000(position, movement->mountFrameToJ2000(Vec3d(0., 0., 1.)),
	                      movement->getAutoMoveDuration());
}

void obsHighlight(bool on)
{
	obsLoad();
	HighlightMgr* markers = GETSTELMODULE(HighlightMgr);
	LabelMgr* labels = GETSTELMODULE(LabelMgr);
	StelCore* core = StelApp::getInstance().getCore();
	if (!markers || !labels || !core)
		return;

	obs.marked = on;
	markers->cleanHighlightList();
	for (int id : std::as_const(obs.labels))
		labels->deleteLabel(id);
	obs.labels.clear();
	if (!on)
		return;

	const int fontSize = StelApp::getInstance().getScreenFontSize();
	const QString colour = markers->getColor().toHtmlColor();
	const float distance = markers->getMarkersSize();
	QList<Vec3d> positions;
	const QJsonArray objects = obsCurrentList().value(QLatin1String(kObsObjects)).toArray();
	for (const QJsonValue& entry : objects)
	{
		const QJsonObject item = entry.toObject();
		const QString designation = item.value("designation").toString();
		const QString ra = item.value("ra").toString();
		const QString dec = item.value("dec").toString();
		if (!ra.isEmpty() && !dec.isEmpty())
		{
			Vec3d position;
			StelUtils::spheToRect(StelUtils::getDecAngle(ra), StelUtils::getDecAngle(dec), position);
			positions.append(position);
			obs.labels.append(labels->labelEquatorial(designation, ra, dec, true, fontSize,
			                                          colour, "NE", distance));
		}
		else if (const StelObjectP obj = StelApp::getInstance().getStelObjectMgr()
		                                 .searchByName(designation))
		{
			positions.append(obj->getJ2000EquatorialPos(core));
			obs.labels.append(labels->labelObject(designation, designation, true, fontSize,
			                                      colour, "NE", distance));
		}
	}
	markers->fillHighlightList(positions);
}
}

extern "C" JNIEXPORT void JNICALL
Java_org_asterium_asterium_NativeBridge_nativeSend(JNIEnv* env, jclass, jint token,
                                                     jstring verb, jstring arg)
{
	if (!uiInstance)
		return;
	const QString qVerb = QJniObject(verb).toString();
	const QString qArg  = arg ? QJniObject(arg).toString() : QString();
	Q_UNUSED(env)
	QMetaObject::invokeMethod(uiInstance, "dispatch", Qt::QueuedConnection,
	                          Q_ARG(int, token), Q_ARG(QString, qVerb), Q_ARG(QString, qArg));
}

AndroidUi::AndroidUi(QObject* parent)
	: QObject(parent)
{
	fpsShown = config()->value("gui/flag_show_fps", false).toBool();
	sinceSnapshot.start();
}

bool AndroidUi::flagShowFps() const
{
	return fpsShown;
}

void AndroidUi::setFlagShowFps(bool on)
{
	if (fpsShown == on)
		return;
	fpsShown = on;
	StelApp::immediateSave("gui/flag_show_fps", on);
	emit flagShowFpsChanged(on);
}

bool AndroidUi::obsListHighlight() const
{
	return obs.marked;
}

void AndroidUi::setObsListHighlight(bool on)
{
	if (obs.marked == on)
		return;
	obsHighlight(on);
	config()->setValue("gui/obslist_highlight", obs.marked);
	emit obsListHighlightChanged(obs.marked);
}

void AndroidUi::install()
{
	if (uiInstance)
		return;
	uiInstance = new AndroidUi(&StelApp::getInstance());

	fullScreenOn = config()->value("video/fullscreen", true).toBool();

	if (StelActionMgr* actions = StelApp::getInstance().getStelActionManager())
	{
		actions->addAction("actionShow_ObsList_Highlight", N_("Miscellaneous"),
		                   N_("Highlight the objects of the observing list"),
		                   uiInstance, "obsListHighlight");
	}
	StelApp::getInstance().getStelPropertyManager()->registerProperty(
			"AndroidUi.flagShowFps", uiInstance, "flagShowFps");

	connect(StelApp::getInstance().getCore(), &StelCore::locationChanged, uiInstance,
	        [](const StelLocation& loc)
	{
		if (loc.planetName != QLatin1String("Earth"))
			return;
		config()->setValue("init_location/last_location",
		                   QStringLiteral("%1, %2, %3")
		                           .arg(static_cast<double>(loc.getLatitude()))
		                           .arg(static_cast<double>(loc.getLongitude()))
		                           .arg(loc.altitude));
	});

	const JNINativeMethod methods[] = {
		{ "nativeSend", "(ILjava/lang/String;Ljava/lang/String;)V",
		  reinterpret_cast<void*>(Java_org_asterium_asterium_NativeBridge_nativeSend) },
	};
	QJniEnvironment env;
	if (!env.registerNativeMethods(kBridgeClass, methods, 1))
	{
		qWarning() << "Asterium: could not register the native UI bridge; the"
		           << "interface will start but no control will do anything";
		return;
	}
	qInfo() << "Asterium: native UI bridge ready";
}

bool AndroidUi::ready()
{
	return uiInstance != nullptr;
}

void AndroidUi::update()
{
	if (!uiInstance)
		return;
	if (!uiInstance->announced)
	{
		uiInstance->announced = true;
		AsteriumOculars::install();
		QJniObject::callStaticMethod<void>(kBridgeClass, "onEngineReady", "()V");
		uiInstance->setObsListHighlight(
				config()->value("gui/obslist_highlight", true).toBool());
	}
	if (uiInstance->sinceSnapshot.elapsed() < 250)
		return;
	uiInstance->sinceSnapshot.restart();
	const QString json = uiInstance->snapshot();
	if (json == uiInstance->lastSnapshot)
		return;
	uiInstance->lastSnapshot = json;
	callJava("onSnapshot", "(Ljava/lang/String;)V", 0, json);
}

void AndroidUi::callJava(const char* method, const char* signature, int token, const QString& payload)
{
	QJniObject str = QJniObject::fromString(payload);
	if (token)
		QJniObject::callStaticMethod<void>(kBridgeClass, method, signature,
		                                   static_cast<jint>(token), str.object<jstring>());
	else
		QJniObject::callStaticMethod<void>(kBridgeClass, method, signature, str.object<jstring>());
}

void AndroidUi::lookupSimbad(int token, const QString& arg, bool byCoordinates)
{
	StelApp& app = StelApp::getInstance();
	const auto fail = [token](const QString& why)
	{
		QJsonObject out;
		out["error"] = why;
		callJava("onReply", "(ILjava/lang/String;)V", token,
		         QString::fromUtf8(QJsonDocument(out).toJson(QJsonDocument::Compact)));
	};
	if (!searchFlag("flag_search_online", true))
	{
		fail(ct_("SIMBAD is switched off in the search options."));
		return;
	}
	if (!simbad)
		simbad = new SimbadSearcher(this);

	const QString server = searchText("simbad_server_url", SearchDialog::DEF_SIMBAD_URL);
	SimbadLookupReply* reply = nullptr;
	if (byCoordinates)
	{
		const QList<StelObjectP> selected = app.getStelObjectMgr().getSelectedObject();
		if (selected.isEmpty())
		{
			fail(ct_("Select an object first: this asks SIMBAD what it holds around it."));
			return;
		}
		reply = simbad->lookupCoords(server, selected.first()->getJ2000EquatorialPos(app.getCore()),
		                             searchNumber("simbad_query_count", 3), 100,
		                             searchNumber("simbad_query_dist", 30),
		                             searchFlag("simbad_query_IDs", true),
		                             searchFlag("simbad_query_types", false),
		                             searchFlag("simbad_query_spec", false),
		                             searchFlag("simbad_query_morpho", false),
		                             searchFlag("simbad_query_dimensions", false));
	}
	else
	{
		if (arg.trimmed().isEmpty())
		{
			fail(QString());
			return;
		}
		reply = simbad->lookup(server, arg.trimmed(), 6, 100);
	}

	QTimer* patience = new QTimer(reply);
	patience->setSingleShot(true);
	connect(patience, &QTimer::timeout, this, [this, reply, token]()
	{
		QJsonObject out;
		out["error"] = ct_("SIMBAD did not answer.");
		callJava("onReply", "(ILjava/lang/String;)V", token,
		         QString::fromUtf8(QJsonDocument(out).toJson(QJsonDocument::Compact)));
		reply->disconnect(this);
		reply->deleteLater();
	});
	patience->start(20000);

	connect(reply, &SimbadLookupReply::statusChanged, this, [this, reply, token]()
	{
		const SimbadLookupReply::SimbadLookupStatus status = reply->getCurrentStatus();
		if (status == SimbadLookupReply::SimbadLookupQuerying)
			return;
		QJsonObject out;
		if (status == SimbadLookupReply::SimbadLookupErrorOccured)
			out["error"] = reply->getErrorString();
		else if (status == SimbadLookupReply::SimbadCoordinateLookupFinished)
			out["text"] = reply->getResult();
		else
		{
			QJsonArray found;
			for (const SimbadSearcher::Result& result : reply->getResults())
			{
				QJsonObject hit;
				hit["name"] = result.name;
				hit["sub"] = result.type;
				hit["pos"] = QString("%1|%2|%3").arg(result.position[0])
				             .arg(result.position[1]).arg(result.position[2]);
				found.append(hit);
			}
			out["results"] = found;
		}
		callJava("onReply", "(ILjava/lang/String;)V", token,
		         QString::fromUtf8(QJsonDocument(out).toJson(QJsonDocument::Compact)));
		reply->disconnect(this);
		reply->deleteLater();
	});
}

void AndroidUi::dispatch(int token, const QString& verb, const QString& arg)
{
	if (!StelApp::isInitialized())
		return;
	if (token)
	{
		if (verb == "simbad.names" || verb == "simbad.here")
		{
			lookupSimbad(token, arg, verb == "simbad.here");
			return;
		}
		callJava("onReply", "(ILjava/lang/String;)V", token, answer(verb, arg));
		return;
	}
	perform(verb, arg);
	if (verb == "view.point")
		return;
	sinceSnapshot.restart();
	const QString json = snapshot();
	if (json != lastSnapshot)
	{
		lastSnapshot = json;
		callJava("onSnapshot", "(Ljava/lang/String;)V", 0, json);
	}
}

QString AndroidUi::snapshot() const
{
	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	if (!core)
		return QString();

	QJsonObject out;

	const double jd = core->getJD();
	out["clock"] = asteriumFormatSimTime(jd, "yyyy-MM-dd HH:mm:ss");
	out["jd"] = jd;

	const double offset = core->getUTCOffset(jd);
	const int offsetMinutes = static_cast<int>(std::lround(offset * 60.));
	out["tz"] = QString("UTC%1%2%3")
	            .arg(offsetMinutes < 0 ? '-' : '+')
	            .arg(std::abs(offsetMinutes) / 60)
	            .arg(std::abs(offsetMinutes) % 60 ? QString(":%1").arg(std::abs(offsetMinutes) % 60, 2, 10, QChar('0'))
	                                              : QString());
	out["sidereal"] = StelUtils::radToHmsStr(core->getLocalSiderealTime(), true).trimmed();

	int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0;
	StelUtils::getDateTimeFromJulianDay(jd + offset / 24., &year, &month, &day,
	                                    &hour, &minute, &second);
	QJsonObject wheels;
	wheels["year"] = year;
	wheels["month"] = month;
	wheels["day"] = day;
	wheels["hour"] = hour;
	wheels["minute"] = minute;
	wheels["second"] = second;
	out["wheels"] = wheels;

	const double rate = core->getTimeRate();
	out["rate"] = rate;
	const double factor = rate / StelCore::JD_SECOND;
	out["realtimeRate"] = core->getRealTimeSpeed();
	out["isNow"] = core->getIsTimeNow();
	if (qFuzzyIsNull(rate))
		out["rateText"] = QString("paused");
	else if (qFuzzyCompare(rate, StelCore::JD_SECOND))
		out["rateText"] = QString("×1");
	else
		out["rateText"] = QString("×%1").arg(factor, 0, 'f',
		                                     factor == std::round(factor) ? 0 : 3);

	const StelLocation& loc = core->getCurrentLocation();
	out["place"] = loc.region.isEmpty() ? loc.name : QString("%1, %2").arg(loc.name, loc.region);
	out["coords"] = QString("%1 %2 · %3 m")
	                .arg(formatLatLon(loc.getLatitude(), 'N', 'S'),
	                     formatLatLon(loc.getLongitude(), 'E', 'W'))
	                .arg(loc.altitude);
	out["planet"] = loc.planetName;
	out["lat"] = loc.getLatitude();
	out["lon"] = loc.getLongitude();
	out["alt"] = loc.altitude;
	out["planetMap"] = mapAssetFor(loc.planetName);
	out["customTz"] = core->getUseCustomTimeZone();
	out["observer"] = loc.role == QChar('o') || isObserverPlanet(loc.planetName);

	if (StelMovementMgr* mv = core->getMovementMgr())
	{
		out["fov"] = QString::number(mv->getCurrentFov(), 'f', 2);
		out["tracking"] = mv->getFlagTracking();
	}
	out["find"] = asteriumFindNudge;
	if (fpsShown)
		out["fps"] = QString::number(static_cast<double>(app.getFps()), 'f', 1);

#ifdef ENABLE_SCRIPTING
	out["script"] = app.getScriptMgr().runningScriptId();
#endif

	const QList<StelObjectP> selected = app.getStelObjectMgr().getSelectedObject();
	if (!selected.isEmpty())
	{
		const StelObjectP obj = selected.first();
		QJsonObject sel;
		const QString designation = designationOf(obj);
		QString name = obj->getNameI18n().isEmpty() ? obj->getEnglishName() : obj->getNameI18n();
		if (name.isEmpty())
			name = designation;
		const bool namedByType = name.isEmpty();
		if (namedByType)
			name = obj->getObjectTypeI18n();
		sel["name"] = name;
		sel["type"] = obj->getObjectType();
		sel["mag"] = magnitudeText(obj, core);
		QString sub = namedByType ? QString() : obj->getObjectTypeI18n();
		if (!designation.isEmpty() && designation != name)
			sub += QStringLiteral(" · ") + designation;
		sel["sub"] = sub;
		obsLoad();
		sel["saved"] = obsHolds(obj);
		Vec3d apparent = obj->getAltAzPosAuto(core);
		apparent.normalize();
		QJsonArray enu;
		enu.append(apparent[1]);
		enu.append(-apparent[0]);
		enu.append(apparent[2]);
		sel["enu"] = enu;
		out["sel"] = sel;
	}
	else
	{
		out["sel"] = QJsonValue::Null;
	}

	StelActionMgr* actions = app.getStelActionManager();
	QJsonObject toggles;
	if (actions)
	{
		for (const ToolbarEntry& entry : kToolbar)
		{
			if (StelAction* a = actions->findAction(entry.action))
				toggles[entry.action] = toolbarState(entry.action, a);
		}
	}
	out["toggles"] = toggles;

	AsteriumOculars::addSnapshot(out);

	return QString::fromUtf8(QJsonDocument(out).toJson(QJsonDocument::Compact));
}

void AndroidUi::perform(const QString& verb, const QString& arg)
{
	StelMainView::getInstance().thereWasAnEvent();

	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	StelActionMgr* actions = app.getStelActionManager();

	const QString key = arg.section('=', 0, 0);
	const QString value = arg.section('=', 1);

	if (AsteriumAstroCalc::perform(verb, arg) || AsteriumOculars::perform(verb, arg))
		return;

	if (verb == "action")
	{
		if (arg == QLatin1String(kFullScreen))
		{
			fullScreenOn = !fullScreenOn;
			config()->setValue("video/fullscreen", fullScreenOn);
		}
		else if (StelAction* a = actions ? actions->findAction(arg) : nullptr)
		{
			a->trigger();
			clampTimeRate(core);
		}
	}
	else if (verb == "prop.set")
	{
		StelProperty* prop = app.getStelPropertyManager()->getProperty(key, true);
		if (prop && prop->getType() == static_cast<QMetaType::Type>(qMetaTypeId<Vec3f>()))
		{
			const Vec3f colour = hexToColor(value);
			app.getStelPropertyManager()->setStelPropertyValue(key, QVariant::fromValue(colour));
			rememberColor(key, colour);
		}
		else
		{
			app.getStelPropertyManager()->setStelPropertyValue(key, QVariant(value));
		}
	}
	else if (verb == "time.rate")
	{
		core->setTimeRate(arg.toDouble() * StelCore::JD_SECOND);
	}
	else if (verb == "time.now")
	{
		core->setTimeNow();
	}
	else if (verb == "time.step")
	{
		core->setJD(core->getJD() + arg.toDouble() / 86400.);
	}
	else if (verb == "time.jd")
	{
		bool ok = false;
		const double newJd = arg.toDouble(&ok);
		if (ok)
			core->setJD(newJd);
	}
	else if (verb == "time.set")
	{
		const QStringList parts = arg.split(',');
		if (parts.size() >= 6)
		{
			double newJd = 0.;
			if (StelUtils::getJDFromDate(&newJd, parts[0].toInt(), parts[1].toInt(), parts[2].toInt(),
			                             parts[3].toInt(), parts[4].toInt(), parts[5].toFloat()))
			{
				core->setJD(newJd - core->getUTCOffset(newJd) / 24.);
			}
		}
	}
	else if (verb == "select")
	{
		StelObjectMgr& mgr = app.getStelObjectMgr();
		if (mgr.findAndSelectI18n(arg) || mgr.findAndSelect(arg))
		{
			const QList<StelObjectP> found = mgr.getSelectedObject();
			if (!found.isEmpty() && core->getMovementMgr())
				asteriumMoveToSelected(core->getMovementMgr(), found.first(), 1.f);
			rememberSearch(arg);
		}
	}
	else if (verb == "search.set")
	{
		const QString key = arg.section('=', 0, 0);
		const QString value = arg.section('=', 1);
		for (const SearchFlag& flag : kSearchFlags)
		{
			if (key == QLatin1String(flag.name))
				config()->setValue(QString("search/%1").arg(flag.key), value == QLatin1String("true"));
		}
		for (const SearchNumber& number : kSearchNumbers)
		{
			if (key == QLatin1String(number.name))
				config()->setValue(QString("search/%1").arg(number.key), value.toInt());
		}
		for (const SearchText& text : kSearchTexts)
		{
			if (key == QLatin1String(text.name))
				config()->setValue(QString("search/%1").arg(text.key), value);
		}
	}
	else if (verb == "search.forget")
	{
		config()->remove("search/recent_objects");
		if (CustomObjectMgr* custom = GETSTELMODULE(CustomObjectMgr))
			custom->removePersistentObjects();
	}
	else if (verb == "search.marker")
	{
		SpecialMarkersMgr* markers = GETSTELMODULE(SpecialMarkersMgr);
		static bool ours = false;
		static bool wasShown = false;
		const bool wanted = arg.toInt() != 0 && searchFlag("flag_fov_center_marker", true);
		if (markers && wanted && !ours)
		{
			ours = true;
			wasShown = markers->getFlagFOVCenterMarker();
			markers->setFlagFOVCenterMarker(true);
		}
		else if (markers && !wanted && ours)
		{
			ours = false;
			markers->setFlagFOVCenterMarker(wasShown);
		}
	}
	else if (verb == "search.goto")
	{
		gotoPosition(arg);
	}
	else if (verb == "simbad.goto")
	{
		const QStringList parts = arg.split('|');
		const QString name = parts.value(0);
		StelObjectMgr& mgr = app.getStelObjectMgr();
		if (!mgr.findAndSelectI18n(name) && !mgr.findAndSelect(name) && parts.size() >= 4)
		{
			Vec3d position(parts.at(1).toDouble(), parts.at(2).toDouble(), parts.at(3).toDouble());
			if (CustomObjectMgr* custom = GETSTELMODULE(CustomObjectMgr))
				custom->addPersistentObject(name, position, QStringLiteral("SIMBAD"));
			mgr.findAndSelect(name);
		}
		moveToSelection();
		rememberSearch(name);
	}
	else if (verb == "unselect")
	{
		app.getStelObjectMgr().unSelect();
	}
	else if (verb == "center")
	{
		const QList<StelObjectP> selected = app.getStelObjectMgr().getSelectedObject();
		if (!selected.isEmpty() && core->getMovementMgr())
			asteriumMoveToSelected(core->getMovementMgr(), selected.first(), 1.f);
	}
	else if (verb == "track")
	{
		if (core->getMovementMgr())
			core->getMovementMgr()->setFlagTracking(arg.toInt() != 0);
	}
	else if (verb == "view.hold")
	{
		asteriumViewHeld = arg.toInt() != 0;
	}
	else if (verb == "view.point")
	{
		StelMovementMgr* mv = core->getMovementMgr();
		const QStringList n = arg.split(',');
		if (!mv || n.size() < 6)
			return;
		const auto toAltAz = [&n](int i) {
			return Vec3d(-n.at(i + 1).toDouble(), n.at(i).toDouble(), n.at(i + 2).toDouble());
		};
		mv->setFlagTracking(false);
		mv->setViewUpVectorJ2000(core->altAzToJ2000(toAltAz(3), StelCore::RefractionOff));
		mv->setViewDirectionJ2000(core->altAzToJ2000(toAltAz(0), StelCore::RefractionOff));
	}
	else if (verb == "city.set")
	{
		const StelLocation target = app.getLocationMgr().locationForString(arg);
		if (target.isValid())
			core->moveObserverTo(target, 0., 0.);
	}
	else if (verb == "loc.set")
	{
		const QStringList parts = arg.split(',');
		if (parts.size() < 2)
			return;
		bool okLat = false, okLon = false;
		const double lat = parts.at(0).toDouble(&okLat);
		const double lon = parts.at(1).toDouble(&okLon);
		if (!okLat || !okLon)
			return;

		const StelLocation& previous = core->getCurrentLocation();
		StelLocation loc;
		loc.planetName = parts.value(3).isEmpty() ? previous.planetName : parts.value(3);
		loc.setLatitude(static_cast<float>(qBound(-90., lat, 90.)));
		loc.setLongitude(static_cast<float>(StelUtils::fmodpos(lon + 180., 360.) - 180.));
		loc.altitude = parts.value(2).isEmpty() ? previous.altitude : parts.value(2).toInt();
		loc.role = 'X';

		const LocationMap nearby = app.getLocationMgr().pickLocationsNearby(
				loc.planetName, static_cast<float>(lon), static_cast<float>(lat), 3.f);
		const StelLocation* best = nullptr;
		float bestDistance = 0.f;
		for (auto it = nearby.constBegin(); it != nearby.constEnd(); ++it)
		{
			const float distance = it.value().distanceDegrees(static_cast<float>(lon),
			                                                  static_cast<float>(lat));
			if (!best || distance < bestDistance)
			{
				best = &it.value();
				bestDistance = distance;
			}
		}
		if (best)
		{
			loc.ianaTimeZone = best->ianaTimeZone;
			if (bestDistance < 0.5f)
			{
				loc.name = best->name;
				loc.region = best->region;
			}
		}
		if (loc.planetName != QStringLiteral("Earth"))
			loc.ianaTimeZone = QStringLiteral("LMST");
		if (isObserverPlanet(loc.planetName))
		{
			loc.role = 'o';
			loc.name = loc.planetName;
			loc.region.clear();
			if (SolarSystem* ssystem = GETSTELMODULE(SolarSystem))
			{
				const PlanetP planet = ssystem->searchByEnglishName(loc.planetName);
				if (planet && planet->getParent())
				{
					app.getStelObjectMgr().findAndSelect(planet->getParent()->getEnglishName());
					if (core->getMovementMgr())
						core->getMovementMgr()->setFlagTracking(true);
				}
			}
		}
		if (loc.ianaTimeZone.isEmpty())
			loc.ianaTimeZone = QStringLiteral("LMST");
		if (loc.name.isEmpty())
			loc.name = QString("%1 %2").arg(formatLatLon(loc.getLatitude(), 'N', 'S'),
			                                formatLatLon(loc.getLongitude(), 'E', 'W'));
		if (loc.planetName != previous.planetName)
			core->moveObserverTo(loc, 0., 1., loc.planetName);
		else
			core->moveObserverTo(loc, 0., 0.);
	}
	else if (verb == "tz.set")
	{
		core->setCurrentTimeZone(arg);
		if (core->getUseCustomTimeZone())
			app.getSettings()->setValue("localization/time_zone", arg);
	}
	else if (verb == "skyculture.set")
	{
		app.getSkyCultureMgr().setCurrentSkyCultureID(arg);
	}
	else if (verb == "skyculture.default")
	{
		StelSkyCultureMgr& scMgr = app.getSkyCultureMgr();
		scMgr.setDefaultSkyCultureID(scMgr.getCurrentSkyCultureID());
	}
	else if (verb == "skyculture.style")
	{
		StelSkyCultureMgr& scMgr = app.getSkyCultureMgr();
		if (key == QStringLiteral("screen"))
			scMgr.setScreenLabelStyle(value);
		else if (key == QStringLiteral("info"))
			scMgr.setInfoLabelStyle(value);
		else if (key == QStringLiteral("zodiac"))
			scMgr.setZodiacLabelStyle(value);
		else if (key == QStringLiteral("lunar"))
			scMgr.setLunarSystemLabelStyle(value);
	}

	else if (verb == "dso.filter")
	{
		NebulaMgr* nebulae = GETSTELMODULE(NebulaMgr);
		if (!nebulae)
			return;
		const int flags = value.toInt();
		if (key == QLatin1String("catalogs"))
		{
			nebulae->setCatalogFilters(flags);
		}
		else if (key == QLatin1String("types"))
		{
			nebulae->setTypeFilters(flags);
			for (const TypeEntry& entry : kDsoTypes)
				StelApp::immediateSave(QString::fromUtf8(entry.ini), (flags & entry.bit) != 0);
		}
	}
	else if (verb == "dso.preset")
	{
		NebulaMgr* nebulae = GETSTELMODULE(NebulaMgr);
		if (!nebulae)
			return;
		if (arg == QLatin1String("all"))            nebulae->selectAllCatalogs();
		else if (arg == QLatin1String("standard"))  nebulae->selectStandardCatalogs();
		else if (arg == QLatin1String("none"))      nebulae->selectNoneCatalogs();
		else if (arg == QLatin1String("load"))      nebulae->loadCatalogFilters();
		else if (arg == QLatin1String("store"))     nebulae->storeCatalogFilters();
	}
	else if (verb == "positions.set")
	{
		AsteriumAstroCalc::writeSetting(key,
				key.endsWith(QLatin1String("magnitude_limit"))
				? QString::number(value.toDouble(), 'f', 2) : value);
	}
	else if (verb == "ephemeris.set")
	{
		AsteriumAstroCalc::writeSetting(key, value);
	}
	else if (verb == "ephemeris.clear")
	{
		AstroCalcDialog::EphemerisList.clear();
		AstroCalcDialog::DisplayedPositionIndex = -1;
		kEphemerisRows = QJsonArray();
		if (SolarSystem* solarSystem = GETSTELMODULE(SolarSystem))
			QMetaObject::invokeMethod(solarSystem, "requestEphemerisVisualization", Qt::DirectConnection);
	}
	else if (verb == "ephemeris.goto")
	{
		const int index = arg.toInt();
		if (index < 0 || index >= AstroCalcDialog::EphemerisList.size())
			return;
		const Ephemeris& item = AstroCalcDialog::EphemerisList.at(index);
		if (item.sso.isNull())
			return;
		AstroCalcDialog::DisplayedPositionIndex = index;

		StelObjectMgr& objects = app.getStelObjectMgr();
		const QString name = item.sso->getEnglishName();
		if (!objects.findAndSelectI18n(name, "Planet") && !objects.findAndSelect(name, "Planet"))
			return;
		core->setJD(item.objDate);
		core->update(0.);

		if (!objects.findAndSelectI18n(name, "Planet"))
			objects.findAndSelect(name, "Planet");
		const QList<StelObjectP> selected = objects.getSelectedObject("Planet");
		StelMovementMgr* movement = core->getMovementMgr();
		if (selected.isEmpty() || !movement)
			return;

		if (selected.first()->getEnglishName() == core->getCurrentLocation().planetName)
			objects.unSelect();
		else
			asteriumMoveToSelected(movement, selected.first(), movement->getAutoMoveDuration());
	}
	else if (verb == "obslist.save")
	{
		obsLoad();
		const QList<StelObjectP> selected = app.getStelObjectMgr().getSelectedObject();
		const int at = selected.isEmpty() ? -1 : obsIndexOfObject(selected.first());
		if (at >= 0)
			obsRemoveAt(at);
		else
			obsAddSelected();
	}
	else if (verb == "obslist.toggle")
	{
		obsLoad();
		StelObjectMgr& objects = app.getStelObjectMgr();
		StelObjectP obj = objects.searchByNameI18n(arg);
		if (obj.isNull())
			obj = objects.searchByName(arg);
		const int at = obsIndexOfObject(obj);
		if (at >= 0)
			obsRemoveAt(at);
		else
			obsAdd(obj);
	}
	else if (verb == "obslist.remove")
	{
		obsLoad();
		obsRemoveAt(obsIndexOf(arg));
	}
	else if (verb == "obslist.goto")
	{
		obsLoad();
		obsGoTo(obsIndexOf(arg));
	}
	else if (verb == "obslist.select")
	{
		obsSelectList(arg);
	}
	else if (verb == "obslist.new")
	{
		obsLoad();
		const QString olud = QUuid::createUuid().toString();
		QJsonObject lists = obsAllLists();
		lists.insert(olud, obsNewList(arg.trimmed().isEmpty() ? ct_("New list") : arg.trimmed()));
		obs.doc.insert(QLatin1String(kObsLists), lists);
		obsSelectList(olud);
	}
	else if (verb == "obslist.rename")
	{
		obsLoad();
		if (!arg.trimmed().isEmpty())
		{
			QJsonObject list = obsCurrentList();
			list[kObsName] = arg.trimmed();
			obsPutCurrent(list);
		}
	}
	else if (verb == "obslist.delete")
	{
		obsLoad();
		QJsonObject lists = obsAllLists();
		if (lists.size() > 1)
		{
			setObsListHighlight(false);
			lists.remove(obs.olud);
			obs.doc.insert(QLatin1String(kObsLists), lists);
			obs.olud = lists.keys().first();
			obsSave();
			obsRefreshMembers();
		}
	}
	else if (verb == "obslist.highlight")
	{
		setObsListHighlight(arg.toInt() != 0);
	}
	else if (verb == "lang.set")
	{
		app.getLocaleMgr().setAppLanguage(arg);
		app.getSettings()->setValue("localization/app_locale", arg);
	}
	else if (verb == "view.save")
	{
		if (StelMovementMgr* mv = core->getMovementMgr())
		{
			mv->setInitFov(mv->getCurrentFov());
			mv->setInitViewDirectionToCurrent();
		}
	}
	else if (verb == "time.startupMode")
	{
		core->setStartupTimeMode(arg.toLower());
	}
	else if (verb == "time.todayTime")
	{
		core->setInitTodayTime(QTime::fromString(arg, "HH:mm"));
	}
	else if (verb == "time.presetNow")
	{
		const double jd = core->getJD();
		core->setPresetSkyTime(jd + core->getUTCOffset(jd) / 24.);
	}
	else if (verb == "fmt.date")
	{
		app.getLocaleMgr().setDateFormatStr(arg);
	}
	else if (verb == "fmt.time")
	{
		app.getLocaleMgr().setTimeFormatStr(arg);
	}
	else if (verb == "deltat.set")
	{
		core->setCurrentDeltaTAlgorithmKey(arg);
	}
#ifdef ENABLE_SCRIPTING
	else if (verb == "script.run")
	{
		QMetaObject::invokeMethod(&app.getScriptMgr(), "runScript", Qt::QueuedConnection,
		                          Q_ARG(QString, arg), Q_ARG(QString, QString()));
	}
	else if (verb == "script.stop")
	{
		app.getScriptMgr().stopScript();
	}
#endif
	else if (verb == "config.save")
	{
		if (StelGui* gui = dynamic_cast<StelGui*>(app.getGui()))
			gui->saveSettingsNow();
		app.getSettings()->sync();
	}
}

QString AndroidUi::answer(const QString& verb, const QString& arg) const
{
	StelApp& app = StelApp::getInstance();
	StelCore* core = app.getCore();
	QJsonObject out;

	if (AsteriumAstroCalc::answer(verb, arg, out)
	    || AsteriumOculars::answer(verb, arg, out))
		return QString::fromUtf8(QJsonDocument(out).toJson(QJsonDocument::Compact));

	if (verb == "config.restore")
	{
		app.getSettings()->setValue("main/restore_defaults", true);
		app.getSettings()->sync();
		out["ok"] = true;
	}
	else if (verb == "search")
	{
		StelObjectMgr& objects = app.getStelObjectMgr();
		const QString typed = arg.trimmed();
		obsLoad();
		QJsonArray hits;
		if (typed.isEmpty())
		{
			for (const QString& name : recentSearches())
			{
				StelObjectP obj = objects.searchByNameI18n(name);
				if (obj.isNull())
					obj = objects.searchByName(name);
				QJsonObject hit = searchHit(name, obj, core);
				hit["saved"] = obsHolds(obj);
				hits.append(hit);
			}
			out["recent"] = true;
		}
		else
		{
			const bool fromStart = searchFlag("flag_start_words", false);
			auto matches = objects.listMatchingObjects(typed, 30, fromStart);
			const QString greek = StelUtils::substituteGreek(typed);
			if (greek != typed)
				matches += objects.listMatchingObjects(greek, 12, fromStart);

			QSet<QString> seen;
			QVector<QPair<QString, StelObjectP>> found;
			for (const auto& match : matches)
			{
				const StelObjectP obj = match.second;
				if (obj.isNull())
					continue;
				const QString identity = obj->getEnglishName().isEmpty()
				                         ? designationOf(obj) : obj->getEnglishName();
				if (!identity.isEmpty() && seen.contains(identity))
					continue;
				seen.insert(identity);
				found.append(match);
			}
			if (searchFlag("flag_sorting_length", false))
			{
				std::stable_sort(found.begin(), found.end(),
				                 [](const QPair<QString, StelObjectP>& a,
				                    const QPair<QString, StelObjectP>& b)
				                 { return a.first.length() < b.first.length(); });
			}
			for (const auto& match : found)
			{
				QJsonObject hit = searchHit(match.first, match.second, core);
				hit["saved"] = obsHolds(match.second);
				hits.append(hit);
			}
		}
		out["results"] = hits;
	}
	else if (verb == "search.options")
	{
		for (const SearchFlag& flag : kSearchFlags)
			out[flag.name] = searchFlag(flag.key, flag.fallback);
		for (const SearchNumber& number : kSearchNumbers)
			out[number.name] = searchNumber(number.key, number.fallback);
		for (const SearchText& text : kSearchTexts)
			out[text.name] = searchText(text.key, text.fallback);
		out["polarDistance"] = app.getFlagPolarDistanceUsage();
	}
	else if (verb == "search.centre")
	{
		out = viewCentre(arg.isEmpty() ? searchText("coordinate_system", "equatorialJ2000") : arg);
	}
	else if (verb == "search.modules")
	{
		const bool english = searchFlag("list_names_english", false);
		QJsonArray modules;
		const QMap<QString, QString> named = app.getStelObjectMgr().objectModulesMap();
		for (auto it = named.constBegin(); it != named.constEnd(); ++it)
		{
			if (app.getStelObjectMgr().listAllModuleObjects(it.key(), english).isEmpty())
				continue;
			QJsonObject module;
			module["id"] = it.key();
			module["name"] = ct_(it.value());
			modules.append(module);
		}
		out["modules"] = modules;
	}
	else if (verb == "search.list")
	{
		const QStringList parts = arg.split('|');
		const QString needle = parts.value(1).trimmed();
		QStringList names;
		for (const auto& [name, obj] : app.getStelObjectMgr()
		                               .listAllModuleObjects(parts.value(0),
		                                                     searchFlag("list_names_english", false)))
		{
			Q_UNUSED(obj)
			if (needle.isEmpty() || name.contains(needle, Qt::CaseInsensitive))
				names.append(name);
		}
		std::sort(names.begin(), names.end(), StelUtils::naturalLessThan);
		out["total"] = names.size();
		names = names.mid(0, 200);
		out["names"] = QJsonArray::fromStringList(names);
	}
	else if (verb == "obslist")
	{
		obsLoad();
		QJsonArray lists;
		const QJsonObject all = obsAllLists();
		for (auto it = all.constBegin(); it != all.constEnd(); ++it)
		{
			QJsonObject entry;
			entry["olud"] = it.key();
			entry["name"] = it.value().toObject().value(QLatin1String(kObsName)).toString();
			entry["count"] = it.value().toObject().value(QLatin1String(kObsObjects)).toArray().size();
			lists.append(entry);
		}
		out["lists"] = lists;
		out["olud"] = obs.olud;
		out["name"] = obsCurrentList().value(QLatin1String(kObsName)).toString();
		out["highlighted"] = obs.marked;

		QJsonArray rows;
		const QJsonArray objects = obsCurrentList().value(QLatin1String(kObsObjects)).toArray();
		for (const QJsonValue& entry : objects)
		{
			const QJsonObject item = entry.toObject();
			const QString designation = item.value("designation").toString();
			QString name = item.value("nameI18n").toString();
			if (const StelObjectP obj = app.getStelObjectMgr().searchByName(designation))
				name = obj->getNameI18n();
			if (name.isEmpty() || name == QLatin1String("—"))
				name = designation;

			QJsonObject row;
			row["name"] = name;
			row["designation"] = designation;
			row["mag"] = item.value("magnitude").toString();
			const QString type = item.value("objtype").toString();
			const QString constellation = item.value("constellation").toString();
			QString sub = type.isEmpty() ? item.value("type").toString() : type;
			if (name != designation && !designation.isEmpty())
				sub += QStringLiteral(" · ") + designation;
			if (!constellation.isEmpty())
				sub += QStringLiteral(" · ") + constellation;
			row["sub"] = sub;
			rows.append(row);
		}
		out["objects"] = rows;
	}
	else if (verb == "info")
	{
		const QList<StelObjectP> selected = app.getStelObjectMgr().getSelectedObject();
		if (!selected.isEmpty())
		{
			const StelObjectP obj = selected.first();
			StelObject::InfoStringGroup flags = StelObject::DefaultInfo;
			if (arg == "all")         flags = StelObject::AllInfo;
			else if (arg == "short")  flags = StelObject::ShortInfo;
			else if (arg == "none")   flags = StelObject::InfoStringGroup(StelObject::Name);
			else if (arg == "custom") flags = app.getStelObjectMgr().getCustomInfoStrings();

			QString name = obj->getNameI18n().isEmpty() ? obj->getEnglishName() : obj->getNameI18n();
			if (name.isEmpty())
				name = designationOf(obj);
			const bool namedByType = name.isEmpty();
			if (namedByType)
				name = obj->getObjectTypeI18n();
			out["name"] = name;
			out["mag"] = magnitudeText(obj, core);
			out["type"] = obj->getObjectType();

			QJsonArray rows;
			const QStringList lines = plainText(obj->getInfoString(core, flags))
			                          .split('\n', Qt::SkipEmptyParts);
			for (int i = namedByType ? 0 : 1; i < lines.size(); ++i)
			{
				if (lines.at(i).trimmed().isEmpty())
					continue;
				rows.append(infoRow(lines.at(i)));
			}
			out["rows"] = rows;
			out["sub"] = (namedByType || lines.isEmpty()) ? QString() : lines.first().trimmed();

			const Vec4d rts = obj->getRTSTime(core);
			QJsonObject times;
			if (rts[3] >= 0.)
			{
				times["rise"] = asteriumFormatSimTime(rts[0], "HH:mm");
				times["transit"] = asteriumFormatSimTime(rts[1], "HH:mm");
				times["set"] = asteriumFormatSimTime(rts[2], "HH:mm");
			}
			out["rts"] = times;
		}
	}
	else if (verb == "cities")
	{
		QJsonArray cities;
		const LocationMap all = app.getLocationMgr().getAllMap();
		const QString needle = arg.trimmed();
		int budget = 120;
		for (auto it = all.constBegin(); it != all.constEnd() && budget > 0; ++it)
		{
			if (!needle.isEmpty() && !it.key().contains(needle, Qt::CaseInsensitive))
				continue;
			QJsonObject city;
			city["id"] = it.key();
			city["name"] = it.value().name;
			city["sub"] = it.value().region;
			cities.append(city);
			--budget;
		}
		out["cities"] = cities;
	}
	else if (verb == "planets")
	{
		QJsonArray planets;
		if (SolarSystem* ssystem = GETSTELMODULE(SolarSystem))
		{
			QList<PlanetP> all = ssystem->getAllPlanets();
			const auto rank = [](const PlanetP& p) {
				switch (p->getPlanetType())
				{
					case Planet::isPlanet:
					case Planet::isMoon:
					case Planet::isDwarfPlanet:
					case Planet::isObserver:
					case Planet::isStar:
						return 0;
					default:
						return 1;
				}
			};
			std::sort(all.begin(), all.end(), [&rank](const PlanetP& a, const PlanetP& b) {
				if (rank(a) != rank(b))
					return rank(a) < rank(b);
				return a->getNameI18n().localeAwareCompare(b->getNameI18n()) < 0;
			});
			for (const PlanetP& planet : all)
			{
				QJsonObject item;
				item["id"] = planet->getEnglishName();
				item["name"] = planet->getNameI18n();
				planets.append(item);
			}
		}
		out["planets"] = planets;
	}
	else if (verb == "timezones")
	{
		QStringList names;
		const QList<QByteArray> ids = QTimeZone::availableTimeZoneIds();
		for (const QByteArray& id : ids)
			names.append(QString::fromUtf8(id));
		names.sort();

		static const QRegularExpression utcRegEx("^UTC([+-])(\\d\\d):(\\d\\d)$");
		std::vector<int> utcOffsets;
		for (int n = 0; n < names.size();)
		{
			const QRegularExpressionMatch match = utcRegEx.match(names.at(n));
			if (match.lastCapturedIndex() != 3)
			{
				++n;
				continue;
			}
			names.removeAt(n);
			const int sign = match.captured(1) == QStringLiteral("-") ? -1 : 1;
			utcOffsets.push_back(sign * (100 * match.captured(2).toInt()
			                             + match.captured(3).toInt() + 1));
		}
		std::sort(utcOffsets.begin(), utcOffsets.end());
		if (!utcOffsets.empty())
		{
			names.removeIf([](const QString& name) { return name.startsWith("Etc/GMT"); });
		}
		for (const int offset : utcOffsets)
		{
			const int hm = std::abs(offset) - 1;
			names.append(QString("UTC%1%2:%3")
			             .arg(offset > 0 ? QStringLiteral("+") : QStringLiteral("-"))
			             .arg(hm / 100, 2, 10, QChar('0'))
			             .arg(hm % 100, 2, 10, QChar('0')));
		}

		QJsonArray zones;
		const auto zone = [&zones](const QString& id, const QString& label) {
			QJsonObject item;
			item["id"] = id;
			item["name"] = label;
			zones.append(item);
		};
		for (const QString& name : std::as_const(names))
			zone(name, name);
		zone("LMST", QStringLiteral("Local Mean Solar Time"));
		zone("LTST", QStringLiteral("Local True Solar Time"));
		zone("system_default", QStringLiteral("System default"));
		out["zones"] = zones;
	}
	else if (verb == "i18n")
	{
		QJsonObject strings;
		QFile file(StelFileMgr::findFile("gui/i18n.json"));
		if (file.open(QIODevice::ReadOnly))
		{
			const QJsonArray keys = QJsonDocument::fromJson(file.readAll()).array();
			for (const QJsonValue& key : keys)
			{
				const QString source = key.toString();
				const QString translated = ct_(source);
				if (translated != source)
					strings.insert(source, translated);
			}
		}
		out["strings"] = strings;
	}
	else if (verb == "toolbar")
	{
		QJsonArray items;
		StelActionMgr* actions = app.getStelActionManager();
		for (const ToolbarEntry& entry : kToolbar)
		{
			if (*entry.flag && !app.getStelPropertyManager()
			                    ->getStelPropertyValue(entry.flag).toBool())
				continue;
			StelAction* a = actions ? actions->findAction(entry.action) : nullptr;
			if (!a)
				continue;
			QJsonObject item;
			item["action"] = entry.action;
			item["icon"] = entry.icon;
			item["label"] = asteriumShortLabel(QString::fromUtf8(entry.caption));
			item["description"] = a->getText().remove('&');
			item["on"] = toolbarState(entry.action, a);
			items.append(item);
		}
		out["items"] = items;
	}
	else if (verb == "props")
	{
		QJsonArray props;
		const QList<StelProperty*> all = app.getStelPropertyManager()->getAllProperties();
		for (StelProperty* prop : all)
		{
			const QString id = prop->getId();
			if (!id.contains('.') || (!arg.isEmpty() && id.section('.', 0, 0) != arg))
				continue;
			QJsonObject item;
			item["id"] = id;
			item["label"] = id.section('.', 1);
			item["readOnly"] = prop->isReadOnly();
			const QVariant v = prop->getValue();
			switch (prop->getType())
			{
				case QMetaType::Bool:
					item["type"] = "bool";
					item["value"] = v.toBool();
					break;
				case QMetaType::Int:
				case QMetaType::UInt:
					item["type"] = "int";
					item["value"] = v.toInt();
					break;
				case QMetaType::Double:
				case QMetaType::Float:
					item["type"] = "double";
					item["value"] = v.toDouble();
					break;
				default:
					if (prop->getType() == static_cast<QMetaType::Type>(qMetaTypeId<Vec3f>()))
					{
						item["type"] = "color";
						item["value"] = colorToHex(v.value<Vec3f>());
					}
					else
					{
						item["type"] = "text";
						item["value"] = v.toString();
					}
					break;
			}
			props.append(item);
		}
		out["props"] = props;
	}
	else if (verb == "projections")
	{
		QJsonArray items;
		for (const QString& key : core->getAllProjectionTypeKeys())
		{
			QJsonObject item;
			item["id"] = key;
			item["name"] = core->projectionTypeKeyToNameI18n(key);
			items.append(item);
		}
		out["projections"] = items;
		out["current"] = core->getCurrentProjectionTypeKey();
		out["name"] = core->getCurrentProjectionNameI18n();
		out["description"] = plainText(core->getProjection(StelCore::FrameJ2000)->getHtmlSummary());
	}
	else if (verb == "skycultures")
	{
		StelSkyCultureMgr& scMgr = app.getSkyCultureMgr();
		const QMap<QString, StelSkyCulture> byId = scMgr.getDirToNameMap();
		const QMap<QString, QString> namesI18 = scMgr.getDirToI18Map();

		QMap<QString, int> regionOrder;
		for (const char* region : kSkyCultureRegions)
			regionOrder.insert(QString::fromUtf8(region), regionOrder.size());

		struct Entry { int region; QString name, id; int begin, end; };
		QList<Entry> entries;
		for (auto it = byId.constBegin(); it != byId.constEnd(); ++it)
		{
			const StelSkyCulture& culture = it.value();
			QStringList regions;
			for (const auto& value : culture.region)
			{
				const QString region = value.toString() == QStringLiteral("Southern America")
				                       ? QStringLiteral("South America") : value.toString();
				regions.append(regionOrder.contains(region) ? region : QStringLiteral("Other"));
			}
			if (regions.isEmpty())
				regions.append(QStringLiteral("Other"));
			regions.removeDuplicates();
			for (const QString& region : std::as_const(regions))
			{
				const Entry entry = { regionOrder.value(region),
				                      namesI18.value(it.key(), culture.englishName),
				                      it.key(), culture.beginTime, culture.endTime };
				entries.append(entry);
			}
		}
		std::sort(entries.begin(), entries.end(), [](const Entry& a, const Entry& b) {
			if (a.region != b.region)
				return a.region < b.region;
			return a.name.localeAwareCompare(b.name) < 0;
		});

		QJsonArray cultures;
		for (const Entry& entry : std::as_const(entries))
		{
			QJsonObject item;
			item["id"] = entry.id;
			item["name"] = entry.name;
			item["region"] = ct_(QString::fromUtf8(kSkyCultureRegions[entry.region]));
			item["begin"] = entry.begin;
			item["end"] = entry.end;
			cultures.append(item);
		}
		out["cultures"] = cultures;
		out["current"] = scMgr.getCurrentSkyCultureID();
	}
	else if (verb == "skyculture")
	{
		StelSkyCultureMgr& scMgr = app.getSkyCultureMgr();
		const QString id = scMgr.getCurrentSkyCultureID();
		const StelSkyCulture culture = scMgr.getDirToNameMap().value(id);

		QStringList regions;
		for (const auto& value : culture.region)
			regions.append(ct_(value.toString()));

		out["id"] = id;
		out["name"] = scMgr.getCurrentSkyCultureNameI18();
		out["region"] = regions.join(QStringLiteral(", "));
		out["begin"] = culture.beginTime;
		out["end"] = culture.endTime;
		out["isDefault"] = id == scMgr.getDefaultSkyCultureID();
		out["description"] = plainText(scMgr.getCurrentSkyCultureHtmlDescription());
		out["screenLabel"] = scMgr.getScreenLabelStyleString();
		out["infoLabel"] = scMgr.getInfoLabelStyleString();
		out["zodiacLabel"] = scMgr.getZodiacLabelStyleString();
		out["lunarLabel"] = scMgr.getLunarSystemLabelStyleString();

		const ConstellationMgr* cMgr = GETSTELMODULE(ConstellationMgr);
		out["hasZodiac"] = cMgr && cMgr->hasZodiac();
		out["hasLunarSystem"] = cMgr && cMgr->hasLunarSystem();
		AsterismMgr* aMgr = GETSTELMODULE(AsterismMgr);
		out["hasAsterisms"] = aMgr && aMgr->isLinesDefined();
	}

	else if (verb == "dso")
	{
		QJsonArray catalogs, types;
		for (const CatalogEntry& entry : kCatalogs)
		{
			QJsonObject item;
			item["label"] = QString::fromUtf8(entry.label);
			item["name"] = QString::fromUtf8(entry.name);
			item["bit"] = entry.bit;
			catalogs.append(item);
		}
		for (const TypeEntry& entry : kDsoTypes)
		{
			QJsonObject item;
			item["label"] = QString::fromUtf8(entry.label);
			item["bit"] = entry.bit;
			types.append(item);
		}
		const NebulaMgr* nebulae = GETSTELMODULE(NebulaMgr);
		out["catalogs"] = catalogs;
		out["types"] = types;
		out["catalogFilters"] = nebulae ? nebulae->getCatalogFilters() : 0;
		out["typeFilters"] = nebulae ? nebulae->getTypeFilters() : 0;
	}
	else if (verb == "dso.colors")
	{
		QJsonArray colors;
		StelPropertyMgr* properties = app.getStelPropertyManager();
		for (const DsoColorEntry& entry : kDsoColors)
		{
			const QString id = QString::fromUtf8(entry.prop);
			QJsonObject item;
			item["id"] = id;
			item["label"] = QString::fromUtf8(entry.label);
			item["value"] = colorToHex(properties->getStelPropertyValue(id).value<Vec3f>());
			colors.append(item);
		}
		out["colors"] = colors;
	}
	else if (verb == "config")
	{
		if (arg == QLatin1String("main"))
		{
			QStringList names = StelTranslator::globalTranslator
			                    ->getAvailableLanguagesNamesNative(StelFileMgr::getLocaleDir());
			std::sort(names.begin(), names.end(), [](const QString& a, const QString& b) {
				return a.localeAwareCompare(b) < 0;
			});
			QJsonArray languages;
			QStringList codes;
			for (const QString& name : std::as_const(names))
			{
				const QString code = StelTranslator::nativeNameToIso639_1Code(name);
				codes.append(code);
				QJsonObject item;
				item["id"] = code;
				item["name"] = name;
				languages.append(item);
			}
			out["languages"] = languages;

			QString current = app.getLocaleMgr().getAppLanguage();
			if (!codes.contains(current) && current.contains('_'))
				current = current.section('_', 0, 0);
			out["language"] = current;

			if (StelMovementMgr* mv = core->getMovementMgr())
			{
				out["startupFov"] = QString("%1°").arg(mv->getInitFov(), 0, 'f', 1);
				double az = 0., alt = 0.;
				StelUtils::rectToSphe(&az, &alt, mv->getInitViewingDirection());
				az = StelUtils::fmodpos(3. * M_PI - az, 2. * M_PI);
				out["startupView"] = QString("%1 / %2")
				                     .arg(StelUtils::radToDmsStr(az), StelUtils::radToDmsStr(alt));
			}
		}
		else if (arg == QLatin1String("time"))
		{
			out["startupMode"] = core->getStartupTimeMode();
			out["todayTime"] = core->getInitTodayTime().toString("HH:mm");

			int y = 0, m = 0, d = 0, h = 0, mi = 0, s = 0;
			StelUtils::getDateTimeFromJulianDay(core->getPresetSkyTime(), &y, &m, &d, &h, &mi, &s);
			out["preset"] = QString("%1-%2-%3 %4:%5")
			                .arg(y, 4, 10, QChar('0')).arg(m, 2, 10, QChar('0'))
			                .arg(d, 2, 10, QChar('0')).arg(h, 2, 10, QChar('0'))
			                .arg(mi, 2, 10, QChar('0'));

			out["dateFormat"] = app.getLocaleMgr().getDateFormatStr();
			out["timeFormat"] = app.getLocaleMgr().getTimeFormatStr();

			QJsonArray algorithms;
			for (const DeltaTEntry& entry : kDeltaT)
			{
				QJsonObject item;
				item["id"] = QString::fromLatin1(entry.key);
				item["name"] = QString::fromUtf8(entry.name);
				algorithms.append(item);
			}
			out["algorithms"] = algorithms;
			out["deltaT"] = core->getCurrentDeltaTAlgorithmKey();
			out["deltaTDescription"] = core->getCurrentDeltaTAlgorithmDescription();
		}
		else if (arg == QLatin1String("scripts"))
		{
#ifdef ENABLE_SCRIPTING
			StelScriptMgr& scripts = app.getScriptMgr();
			QStringList ids = scripts.getScriptList();
			ids.sort();
			QJsonArray items;
			for (const QString& id : std::as_const(ids))
			{
				QJsonObject item;
				item["id"] = id;
				const QString name = scripts.getName(id).trimmed();
				item["name"] = name.isEmpty() ? id : name;
				item["sub"] = scripts.getDescription(id).simplified();
				item["author"] = scripts.getAuthor(id).trimmed();
				item["license"] = scripts.getLicense(id).trimmed();
				item["version"] = scripts.getVersion(id).trimmed();
				items.append(item);
			}
			out["scripts"] = items;
			out["running"] = scripts.runningScriptId();
#endif
		}
	}
	else if (verb == "help")
	{
		if (arg == QLatin1String("log"))
		{
			out["path"] = StelLogger::getLogFileName();
			out["text"] = StelLogger::getLog();
		}
		else if (arg == QLatin1String("credits"))
		{
			const QString path = StelFileMgr::findFile("CREDITS.md", StelFileMgr::File);
			out["path"] = path;
			QFile file(path);
			if (file.open(QIODevice::ReadOnly))
			{
				out["text"] = QString::fromUtf8(file.readAll());
				file.close();
			}
		}
		else if (arg == QLatin1String("config"))
		{
			const QString path = StelApp::getInstance().getSettings()->fileName();
			out["path"] = path;
			QFile file(path);
			if (file.open(QIODevice::ReadOnly))
			{
				out["text"] = QString::fromUtf8(file.readAll());
				file.close();
			}
		}
		else if (arg.startsWith(QLatin1String("license:")))
		{
			static const QStringList shipped = {
				QStringLiteral("COPYING"),        QStringLiteral("COPYING.GPL3"),
				QStringLiteral("COPYING.LGPL3"),  QStringLiteral("COPYING.LGPL21"),
				QStringLiteral("COPYING.APACHE2"), QStringLiteral("data/OFL.txt") };
			const QString name = arg.mid(8);
			if (shipped.contains(name))
			{
				const QString path = StelFileMgr::findFile(name, StelFileMgr::File);
				out["path"] = path;
				QFile file(path);
				if (file.open(QIODevice::ReadOnly))
				{
					out["text"] = QString::fromUtf8(file.readAll());
					file.close();
				}
			}
		}
		else
		{
			out["version"] = QStringLiteral(ASTERIUM_VERSION);
			out["qt"] = QString(qVersion());
		}
	}
	else if (verb == "landscapes")
	{
		if (LandscapeMgr* lmgr = GETSTELMODULE(LandscapeMgr))
		{
			const QStringList names = lmgr->getAllLandscapeNames();
			const QStringList ids = lmgr->getAllLandscapeIDs();
			QJsonArray items;
			for (int i = 0; i < names.size() && i < ids.size(); ++i)
			{
				QJsonObject item;
				item["id"] = ids.at(i);
				item["name"] = ct_(names.at(i));
				items.append(item);
			}
			out["landscapes"] = items;
			out["current"] = lmgr->getCurrentLandscapeID();
			out["name"] = ct_(lmgr->getCurrentLandscapeName());
			out["isDefault"] = lmgr->getCurrentLandscapeID() == lmgr->getDefaultLandscapeID();
			out["html"] = lmgr->getCurrentLandscapeHtmlDescription();
		}
	}
	else if (verb == "positions")
	{
		QSettings* conf = app.getSettings();
		SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
		NebulaMgr* dsoMgr = GETSTELMODULE(NebulaMgr);
		StarMgr* starMgr = GETSTELMODULE(StarMgr);

		const double magLimit = conf->value("astrocalc/celestial_magnitude_limit", 6.0).toDouble();
		const bool horizon = conf->value("astrocalc/flag_horizontal_coordinates", false).toBool();
		const int celTypeId = conf->value("astrocalc/celestial_category", "200").toInt();
		const QString celType = QString::number(celTypeId);
		const bool southAzimuth = app.getFlagSouthAzimuthUsage();
		const bool decimalDegrees = app.getFlagShowDecimalDegrees();
		const bool polarDistance = app.getFlagPolarDistanceUsage();
		const double jd = core->getJD();
		const double utcShift = core->getUTCOffset(jd) / 24.;

		out["mag"] = magLimit;
		out["horizontal"] = horizon;
		out["category"] = celType;
		out["categories"] = positionCategories(app.getStelObjectMgr());
		out["when"] = asteriumFormatSimTime(jd, "yyyy-MM-dd HH:mm:ss");
		positionColumnLabels(celTypeId, horizon, out);

		QJsonArray rows;
		if (solarSystem && dsoMgr && starMgr)
		{
			const PlanetP sun = solarSystem->getSun();
			const Vec3d sunPos = sun->getJ2000EquatorialPos(core);

			const auto addRow = [&](const StelObjectP& obj, const QString& name, const QString& select,
			                        double mag, const QString& size, const QString& extra,
			                        const QString& elongation)
			{
				const QPair<QString, QString> coords = positionCoordinates(
						horizon ? obj->getAltAzPosAuto(core) : obj->getJ2000EquatorialPos(core),
						horizon, southAzimuth, decimalDegrees, polarDistance);
				QString transit, elevation;
				transitStrings(obj, core, utcShift, decimalDegrees, transit, elevation);

				QJsonObject row;
				row["name"] = name;

				row["select"] = select;
				row["lng"] = coords.first;
				row["lat"] = coords.second;
				row["mag"] = mag > 90. ? kDash : QString::number(mag, 'f', 2);
				row["size"] = size;
				row["extra"] = extra;
				row["transit"] = transit;
				row["elev"] = elevation;
				row["elong"] = elongation;
				row["type"] = obj->getObjectTypeI18n();
				rows.append(row);
			};

			if (celTypeId < 170)
			{
				const auto addDso = [&](const NebulaP& obj)
				{
					double magOp = (celTypeId == 12 || celTypeId == 102 || celTypeId == 111)
					               ? static_cast<double>(obj->getVMagnitude(core))
					               : static_cast<double>(obj->getVMagnitudeWithExtinction(core));
					bool bright;
					if (celTypeId == 35 || (celTypeId == 169 && obj->getDSOType() == Nebula::NebDn))
					{
						bright = true;
						magOp = 99.;
					}
					else
						bright = (magOp <= magLimit);

					if (!obj->objectInDisplayedCatalog() || !obj->objectInAllowedSizeRangeLimits()
					    || !bright || !obj->isAboveRealHorizon(core))
						return;

					const QString id = obj->getDSODesignation();
					const QString proper = obj->getNameI18n();
					QString name = proper;
					if (proper.isEmpty())
						name = id;
					else if (!id.isEmpty())
						name = QString("%1 (%2)").arg(id, proper);

					QString brightness = QString::number(
							static_cast<double>(obj->getSurfaceBrightnessWithExtinction(core)), 'f', 2);
					if (brightness.toFloat() > 90.f)
						brightness = kDash;

					QString size = QString::number(obj->getAngularRadius(core) * 120., 'f', 3);
					if (size.toFloat() < 0.01f)
						size = kDash;

					const StelObjectP object = qSharedPointerCast<StelObject>(obj);
					addRow(object, name, id.isEmpty() ? proper : id, magOp, size, brightness,
					       elongationString(obj->getJ2000EquatorialPos(core).angle(sunPos), decimalDegrees));
				};

				if (celTypeId == 169)
				{
					for (const NebulaP& obj : dsoMgr->getAllDeepSkyObjects())
						addDso(obj);
				}
				else
				{
					for (const NebulaP& obj : dsoMgr->getDeepSkyObjectsByType(celType))
						addDso(obj);
				}
			}
			else if (celTypeId >= 200 && celTypeId <= 204)
			{
				QList<PlanetP> planets;
				if (celTypeId == 200 || celTypeId == 203)
					planets = solarSystem->getAllPlanets();
				else if (celTypeId == 201 || celTypeId == 202)
					planets = solarSystem->getAllMinorBodies();
				else
				{
					const QStringList nakedEye = { "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn" };
					planets.append(sun);
					for (const QString& name : nakedEye)
						planets.append(solarSystem->searchByEnglishName(name));
				}

				for (const PlanetP& planet : std::as_const(planets))
				{
					if (planet.isNull())
						continue;
					const Planet::PlanetType type = planet->getPlanetType();
					bool wanted = false;
					switch (celTypeId)
					{
						case 200: wanted = (type != Planet::isUNDEFINED); break;
						case 201: wanted = (type == Planet::isComet); break;
						case 202: wanted = (type == Planet::isAsteroid || type == Planet::isCubewano
						                    || type == Planet::isDwarfPlanet || type == Planet::isOCO
						                    || type == Planet::isPlutino || type == Planet::isSDO
						                    || type == Planet::isSednoid || type == Planet::isInterstellar);
						          break;
						case 203: wanted = (type == Planet::isPlanet); break;
						default:  wanted = true; break;
					}
					if (!planet->hasValidPositionalData(jd, Planet::PositionQuality::OrbitPlotting))
						wanted = false;
					if (!wanted || planet == core->getCurrentPlanet()
					    || static_cast<double>(planet->getVMagnitudeWithExtinction(core)) > magLimit
					    || !planet->isAboveRealHorizon(core))
						continue;

					QString size = QString::number(planet->getAngularRadius(core) * 7200., 'f', 2);
					if (size.toFloat() < 1e-4f || type == Planet::isComet)
						size = kDash;

					const QString elongation = (planet == sun)
							? kDash
							: elongationString(planet->getElongation(core->getObserverHeliocentricEclipticPos()),
							                   decimalDegrees);

					addRow(qSharedPointerCast<StelObject>(planet), planet->getNameI18n(),
					       planet->getNameI18n(), planet->getVMagnitudeWithExtinction(core), size,
					       QString::number(planet->getJ2000EquatorialPos(core).norm(), 'f', 5), elongation);
				}
			}
			else
			{
				QList<StelACStarData> stars;
				switch (celTypeId)
				{
					case 170: stars = starMgr->getHipparcosDoubleStars(); break;
					case 171: stars = starMgr->getHipparcosVariableStars(); break;
					case 173: stars = starMgr->getHipparcosAlgolTypeStars(); break;
					case 174: stars = starMgr->getHipparcosClassicalCepheidsTypeStars(); break;
					default:  stars = starMgr->getHipparcosHighPMStars(); break;
				}

				for (const StelACStarData& star : std::as_const(stars))
				{
					const StelObjectP obj = star.first;
					if (obj.isNull()
					    || static_cast<double>(obj->getVMagnitudeWithExtinction(core)) > magLimit
					    || !obj->isAboveRealHorizon(core))
						continue;

					QString extra;
					if (celTypeId == 170)
						extra = QString::number(static_cast<double>(star.second), 'f', 3);
					else if (celTypeId == 171 || celTypeId == 173 || celTypeId == 174)
						extra = star.second > 0.f ? QString::number(star.second, 'f', 5) : kDash;
					else
						extra = QString::number(star.second, 'f', 5);

					QString name = obj->getNameI18n();
					if (name.isEmpty())
						name = obj->getID();

					addRow(obj, name, name, obj->getVMagnitudeWithExtinction(core), kDash, extra,
					       elongationString(obj->getJ2000EquatorialPos(core).angle(sunPos), decimalDegrees));
				}
			}
		}
		out["positions"] = rows;
	}
	else if (verb == "positions.hec")
	{
		QSettings* conf = app.getSettings();
		SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);

		const bool minorPlanets = conf->value("astrocalc/flag_hec_minor_planets", false).toBool();
		const bool brightComets = conf->value("astrocalc/flag_hec_bright_comets", false).toBool();
		const double magLimit = conf->value("astrocalc/hec_magnitude_limit", 9.0).toDouble();
		const bool decimalDegrees = app.getFlagShowDecimalDegrees();

		out["minorPlanets"] = minorPlanets;
		out["brightComets"] = brightComets;
		out["mag"] = magLimit;
		out["when"] = asteriumFormatSimTime(core->getJD(), "yyyy-MM-dd HH:mm:ss");

		QJsonArray rows;
		if (solarSystem)
		{
			QMap<QString, QChar> symbols = {
				{ "Mercury", QChar(0x263F) }, { "Venus",   QChar(0x2640) }, { "Earth",   QChar(0x2641) },
				{ "Mars",    QChar(0x2642) }, { "Jupiter", QChar(0x2643) }, { "Saturn",  QChar(0x2644) },
				{ "Uranus",  QChar(0x2645) }, { "Neptune", QChar(0x2646) }, { "Pluto",   QChar(0x2647) }
			};

			QList<PlanetP> planets;
			for (const PlanetP& planet : solarSystem->getAllPlanets())
			{
				if (planet->getPlanetType() == Planet::isPlanet)
					planets.append(planet);
				if (brightComets && planet->getPlanetType() == Planet::isComet
				    && planet->getVMagnitude(core) <= magLimit)
				{
					planets.append(planet);
					symbols.insert(planet->getEnglishName(), QChar(0x2604));
				}
			}
			if (minorPlanets)
			{
				const QStringList wanted = { "Ceres", "Pallas", "Juno", "Vesta" };
				planets.append(solarSystem->searchByEnglishName("Pluto"));
				for (const QString& name : wanted)
					planets.append(solarSystem->searchMinorPlanetByEnglishName(name));
			}

			for (const PlanetP& planet : std::as_const(planets))
			{
				if (planet.isNull())
					continue;
				const Vec3d pos = planet->getHeliocentricEclipticPos();
				double longitude = 0., latitude = 0.;
				StelUtils::rectToSphe(&longitude, &latitude, pos);
				if (longitude < 0.)
					longitude += 2. * M_PI;

				QJsonObject row;
				row["name"] = planet->getNameI18n();
				row["select"] = planet->getNameI18n();
				row["symbol"] = QString(symbols.value(planet->getEnglishName(), QChar(0x200B)));
				row["lat"] = decimalDegrees ? StelUtils::radToDecDegStr(latitude)
				                            : StelUtils::radToDmsStr(latitude, true);
				row["lng"] = decimalDegrees ? StelUtils::radToDecDegStr(longitude)
				                            : StelUtils::radToDmsStr(longitude, true);
				row["dist"] = QString("%1 %2").arg(QString::number(pos.norm(), 'f', 2),
				                                   qc_("AU", "distance, astronomical unit"));

				row["lonDeg"] = longitude * M_180_PI;
				row["distAU"] = pos.norm();
				rows.append(row);
			}
		}
		out["hec"] = rows;
	}
	else if (verb == "ephemeris" || verb == "ephemeris.generate")
	{
		QSettings* conf = app.getSettings();
		SolarSystem* solarSystem = GETSTELMODULE(SolarSystem);
		if (verb == "ephemeris.generate")
			generateEphemeris(app, core, arg);

		out["body"] = conf->value("astrocalc/ephemeris_celestial_body", "Moon").toString();
		out["secondBody"] = conf->value("astrocalc/ephemeris_second_celestial_body", "none").toString();
		out["step"] = conf->value("astrocalc/ephemeris_time_step", 6).toInt();
		out["unit"] = conf->value("astrocalc/ephemeris_time_unit", 5).toInt();
		out["duration"] = conf->value("astrocalc/ephemeris_time_duration", 1).toInt();
		out["nakedEye"] = conf->value("astrocalc/ephemeris_nakedeye_planets", false).toBool();

		out["sunAltitude"] = conf->value("astrocalc/ephemeris_sun_altitude", -10.0).toDouble();
		out["sunAltEvening"] = conf->value("astrocalc/ephemeris_sun_altitude_evening", 0).toInt() == 0;
		out["oppositionPlanet"] = conf->value("astrocalc/ephemeris_opposition_planet", "Mars").toString();
		out["horizontal"] = conf->value("astrocalc/flag_ephemeris_horizontal_coordinates", false).toBool();
		out["ignoreDateTest"] = conf->value("astrocalc/flag_ephemeris_ignore_date_test", true).toBool();

		out["nakedEyeAllowed"] = solarSystem && core->getCurrentPlanet() == solarSystem->getEarth();
		out["start"] = asteriumFormatSimTime(core->getJD(), "yyyy-MM-dd HH:mm");
		out["bodies"] = bodies(solarSystem, core);

		QJsonArray steps;
		for (const EphemerisStep& step : kEphemerisSteps)
		{
			QJsonObject entry;
			entry["id"] = QString::number(step.id);
			entry["name"] = ct_(QString::fromUtf8(step.label));
			steps.append(entry);
		}
		out["steps"] = steps;

		QJsonArray units;
		for (const EphemerisUnit& unit : kEphemerisUnits)
		{
			QJsonObject entry;
			entry["id"] = QString::number(unit.id);
			entry["name"] = qc_(QString::fromUtf8(unit.label), unit.context);
			units.append(entry);
		}
		out["units"] = units;

		const bool horizontal = out["horizontal"].toBool();
		out["lngLabel"] = horizontal ? ct_("Azimuth") : ct_("RA (J2000)");
		out["latLabel"] = horizontal
		                  ? ct_("Altitude")
		                  : (app.getFlagPolarDistanceUsage() ? ct_("PD (J2000)") : ct_("Dec (J2000)"));
		out["distLabel"] = QString("%1, %2").arg(ct_("Dist."), qc_("AU", "distance, astronomical unit"));
		out["rows"] = kEphemerisRows;

		StelPropertyMgr* properties = app.getStelPropertyManager();
		out["showLine"] = properties->getStelPropertyValue("SolarSystem.ephemerisLineDisplayed").toBool();
		out["showMarkers"] = properties->getStelPropertyValue("SolarSystem.ephemerisMarkersDisplayed").toBool();
		out["showDates"] = properties->getStelPropertyValue("SolarSystem.ephemerisDatesDisplayed").toBool();
		out["showMagnitudes"] = properties->getStelPropertyValue("SolarSystem.ephemerisMagnitudesDisplayed").toBool();
	}

	return QString::fromUtf8(QJsonDocument(out).toJson(QJsonDocument::Compact));
}

#else

AndroidUi::AndroidUi(QObject* parent) : QObject(parent) {}
void AndroidUi::install() {}
void AndroidUi::update() {}
bool AndroidUi::ready() { return false; }
void AndroidUi::dispatch(int, const QString&, const QString&) {}
void AndroidUi::lookupSimbad(int, const QString&, bool) {}
QString AndroidUi::snapshot() const { return QString(); }
QString AndroidUi::answer(const QString&, const QString&) const { return QString(); }
void AndroidUi::perform(const QString&, const QString&) {}
void AndroidUi::callJava(const char*, const char*, int, const QString&) {}
bool AndroidUi::obsListHighlight() const { return false; }
void AndroidUi::setObsListHighlight(bool) {}
bool AndroidUi::flagShowFps() const { return false; }
void AndroidUi::setFlagShowFps(bool) {}

#endif
