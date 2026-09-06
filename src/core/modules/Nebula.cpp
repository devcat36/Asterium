/*
 * Stellarium
 * Copyright (C) 2002 Fabien Chereau
 * Copyright (C) 2011 Alexander Wolf
 * Copyright (C) 2015 Georg Zotti
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

#include "Nebula.hpp"
#include "NebulaMgr.hpp"
#include "Planet.hpp"
#include "StelLocaleMgr.hpp"
#include "StelTexture.hpp"

#include "StelUtils.hpp"
#include "StelApp.hpp"
#include "StelModuleMgr.hpp"
#include "StelCore.hpp"
#include "StelPainter.hpp"
#include "RefractionExtinction.hpp"
#include "StelSkyCultureMgr.hpp"
#include "ConstellationMgr.hpp"

#include <QTextStream>
#include <QFile>
#include <QString>
#include <QRegularExpression>

#include <QDebug>
#include <QBuffer>

const QString Nebula::NEBULA_TYPE = QStringLiteral("Nebula");

QHash<unsigned int, Nebula::Extras> Nebula::extrasByDSO;
std::vector<unsigned int> Nebula::rareNumbers;
std::vector<QString> Nebula::rareStrings;
const QString Nebula::noDesignation;
const Nebula::Extras Nebula::emptyExtras;
StelTextureSP Nebula::texRegion;
StelTextureSP Nebula::texPointElement;
StelTextureSP Nebula::texPlanetaryNebula;
bool  Nebula::drawHintProportional = false;
bool  Nebula::surfaceBrightnessUsage = false;
bool  Nebula::designationUsage = false;
float Nebula::hintsBrightness = 1.f;
float Nebula::labelsBrightness = 1.f;
Vec3f Nebula::labelColor = Vec3f(0.4f,0.3f,0.5f);
QMap<Nebula::NebulaType, Vec3f>Nebula::hintColorMap;
bool Nebula::flagUseTypeFilters = false;
Nebula::CatalogGroup Nebula::catalogFilters = Nebula::CatalogGroup(Nebula::CatNone);
Nebula::TypeGroup Nebula::typeFilters = Nebula::TypeGroup(Nebula::TypeAll);
bool Nebula::flagUseArcsecSurfaceBrightness = false;
bool Nebula::flagUseShortNotationSurfaceBrightness = true;
bool Nebula::flagUseOutlines = false;
bool Nebula::flagShowAdditionalNames = true;
bool Nebula::flagShowOnlyNamedDSO = false;
bool Nebula::flagUseSizeLimits = false;
double Nebula::minSizeLimit = 1.0;
double Nebula::maxSizeLimit = 600.0;

const QMap<Nebula::NebulaType, QString> Nebula::typeEnglishStringMap = // Maps type to english name.
{
	{ NebGx     , L1S(N_("galaxy")) },
	{ NebAGx    , L1S(N_("active galaxy")) },
	{ NebRGx    , L1S(N_("radio galaxy")) },
	{ NebIGx    , L1S(N_("interacting galaxy")) },
	{ NebQSO    , L1S(N_("quasar")) },
	{ NebCl     , L1S(N_("star cluster")) },
	{ NebOc     , L1S(N_("open star cluster")) },
	{ NebGc     , L1S(N_("globular star cluster")) },
	{ NebSA     , L1S(N_("stellar association")) },
	{ NebSC     , L1S(N_("star cloud")) },
	{ NebN      , L1S(N_("nebula")) },
	{ NebPn     , L1S(N_("planetary nebula")) },
	{ NebDn     , L1S(N_("dark nebula")) },
	{ NebRn     , L1S(N_("reflection nebula")) },
	{ NebBn     , L1S(N_("bipolar nebula")) },
	{ NebEn     , L1S(N_("emission nebula")) },
	{ NebCn     , L1S(N_("cluster associated with nebulosity")) },
	{ NebHII    , L1S(N_("HII region")) },
	{ NebSNR    , L1S(N_("supernova remnant")) },
	{ NebISM    , L1S(N_("interstellar matter")) },
	{ NebEMO    , L1S(N_("emission object")) },
	{ NebBLL    , L1S(N_("BL Lac object")) },
	{ NebBLA    , L1S(N_("blazar")) },
	{ NebMolCld , L1S(N_("molecular cloud")) },
	{ NebYSO    , L1S(N_("young stellar object")) },
	{ NebPossQSO, L1S(N_("possible quasar")) },
	{ NebPossPN , L1S(N_("possible planetary nebula")) },
	{ NebPPN    , L1S(N_("protoplanetary nebula")) },
	{ NebStar   , L1S(N_("star")) },
	{ NebSymbioticStar   , L1S(N_("symbiotic star")) },
	{ NebEmissionLineStar, L1S(N_("emission-line star")) },
	{ NebSNC     , L1S(N_("supernova candidate")) },
	{ NebSNRC    , L1S(N_("supernova remnant candidate")) },
	{ NebGxCl    , L1S(N_("cluster of galaxies")) },
	{ NebPartOfGx, L1S(N_("part of a galaxy")) },
	{ NebRegion  , L1S(N_("region of the sky")) },
	{ NebUnknown , L1S(N_("object of unknown nature")) }
};

Nebula::Nebula()
	: StelObject()
	, DSO_nb(0)
	, PGC_nb(0)
	, withoutID(false)
	, nameI18()
	, mTypeString()
	, bMag(99.)
	, vMag(99.)
	, majorAxisSize(0.)
	, minorAxisSize(0.)
	, orientationAngle(0)
	, oDistance(0.)
	, oDistanceErr(0.)
	, redshift(99.)
	, redshiftErr(0.)
	, nType()
{
	outlineSegments.clear();
	designations.clear();
}

Nebula::~Nebula()
{
}

QString Nebula::getMagnitudeInfoString(const StelCore *core, const InfoStringGroup& flags, const int decimals, const float& magOffset) const
{
	QString res;
	const float mmag = qMin(vMag, bMag);
	if (mmag < 50.f && flags&Magnitude)
	{
		QString emag = "";
		QString fsys = "";
		bool bmag = false;
		float mag = getVMagnitude(core) + magOffset;
		float mage = getVMagnitudeWithExtinction(core, mag, magOffset);
		bool hasAtmosphere = core->getSkyDrawer()->getFlagHasAtmosphere();
		QString tmag = q_("Magnitude");
		if (nType == NebDn || catNum(CatB)>0) // Dark nebulae or objects from Barnard catalog
			tmag = q_("Opacity");

		if (bMag < 50.f && vMag > 50.f)
		{
			fsys = QString("(%1 B").arg(q_("photometric passband"));
			if (hasAtmosphere)
				fsys.append(";");
			else
				fsys.append(")");
			mag = getBMagnitude(core);
			mage = getBMagnitudeWithExtinction(core);
			bmag = true;
		}

		const float airmass = getAirmass(core);
		if (nType != NebDn && catNum(CatB)==0 && airmass>-1.f) // Don't show extincted magnitude much below horizon where model is meaningless.
		{
			emag = QString("%1 <b>%2</b> %3 <b>%4</b> %5)").arg(q_("reduced to"), QString::number(mage, 'f', decimals), q_("by"), QString::number(airmass, 'f', 2), q_("Airmasses"));
			if (!bmag)
				emag = QString("(%1").arg(emag);
		}
		res = QString("%1: <b>%2</b> %3 %4<br />").arg(tmag, QString::number(mag, 'f', decimals), fsys, emag);
	}
	res += getExtraInfoStrings(Magnitude).join("");
	return res;
}

QString Nebula::getScreenLabel() const
{
	static StelSkyCultureMgr *scMgr=GETSTELMODULE(StelSkyCultureMgr);
	QStringList cLabels = getCultureLabels(scMgr->getScreenLabelStyle());

	// TODO: Decide whether SCs with DSO names should only show common names of these objects, or use common names for all objects.

	QString nameI18n = scMgr->currentSkycultureUsesCommonNames() ? getNameI18n() : QString();
	return (cLabels.isEmpty() ? nameI18n : cLabels.constFirst());
}

QString Nebula::getInfoLabel() const
{
	return getCultureLabels(GETSTELMODULE(StelSkyCultureMgr)->getInfoLabelStyle()).join(" - ");
}

QStringList Nebula::getCultureLabels(StelObject::CulturalDisplayStyle style) const
{
	static StelSkyCultureMgr *scMgr=GETSTELMODULE(StelSkyCultureMgr);
	QStringList labels;
	const Extras& e = readExtras();
	if (e.culturalNames.isEmpty())
		return labels;
	for (auto &cName: e.culturalNames)
		{
			const QString modernName= nameI18.isEmpty() ? getDSODesignation() : nameI18;
			labels << scMgr->createCulturalLabel(cName, style, modernName);
		}
	labels.removeDuplicates();
	labels.removeAll(QString(""));
	labels.removeAll(QString());
	return labels;
}


QString Nebula::getInfoString(const StelCore *core, const InfoStringGroup& flags) const
{
	// rtl tracks the right-to-left status of the text in the current position.
	const bool rtl = StelApp::getInstance().getLocaleMgr().isSkyRTL();
	QString str;
	QTextStream oss(&str);
	bool withDecimalDegree = StelApp::getInstance().getFlagShowDecimalDegrees();

	if ((flags&Name) || (flags&CatalogNumber))
		oss << (rtl ? "<h2 dir=\"rtl\">" : "<h2 dir=\"ltr\">");

	if (hasCulturalNames && flags&Name)
		oss << getInfoLabel() << "<br/>";

	if (!nameI18.isEmpty() && flags&Name)
	{
		oss << getNameI18n();
		QString aliases = getI18nAliases();
		if (!aliases.isEmpty() && flagShowAdditionalNames)
			oss << " (" << aliases << ")";
	}

	if (flags&CatalogNumber)
	{	
		if (!nameI18.isEmpty() && !withoutID && flags&Name)
			oss << "<br>";

		buildDesignations();
		oss << designations.join(" - ");
	}

	if ((flags&Name) || (flags&CatalogNumber))
		oss << "</h2>";

	if (flags&Name)
	{
		QStringList extraNames=getExtraInfoStrings(Name);
		if (!extraNames.isEmpty())
			oss << q_("Additional names: ") << extraNames.join(", ") << "<br/>";
	}
	if (flags&CatalogNumber)
	{
		QStringList extraCat=getExtraInfoStrings(CatalogNumber);
		if (!extraCat.isEmpty())
			oss << q_("Additional catalog numbers: ") << extraCat.join(", ") << "<br/>";
	}

	if (flags&ObjectType)
	{
		QString mt = getMorphologicalTypeString();
		if (mt.isEmpty())
			oss << QString("%1: <b>%2</b>").arg(q_("Type"), getObjectTypeI18n()) << "<br/>";
		else
			oss << QString("%1: <b>%2</b> (%3)").arg(q_("Type"), getObjectTypeI18n(), mt) << "<br/>";
		oss << getExtraInfoStrings(ObjectType).join("");
	}

	oss << getMagnitudeInfoString(core, flags, 2);

	if (flags&Extra)
	{
		if (vMag < 50 && bMag < 50)
			oss << QString("%1: <b>%2</b>").arg(q_("Color Index (B-V)"), QString::number(bMag-vMag, 'f', 2)) << "<br/>";
	}
	float mmag = qMin(vMag,bMag);
	if (nType != NebDn && mmag < 50 && flags&Extra)
	{
		QString sb = q_("Surface brightness");
		QString ae = q_("after extinction");
		QString mu;
		if (flagUseShortNotationSurfaceBrightness)
		{
			mu = QString("<sup>m</sup>/□'");
			if (flagUseArcsecSurfaceBrightness)
				mu = QString("<sup>m</sup>/□\"");
		}
		else
		{
			mu = QString("%1/%2<sup>2</sup>").arg(qc_("mag", "magnitude"), q_("arc-min"));
			if (flagUseArcsecSurfaceBrightness)
				mu = QString("%1/%2<sup>2</sup>").arg(qc_("mag", "magnitude"), q_("arc-sec"));
		}

		if (getSurfaceBrightness(core)<99.f)
		{
			if (getAirmass(core)>-1.f && getSurfaceBrightnessWithExtinction(core)<99.f) // Don't show extincted surface brightness much below horizon where model is meaningless.
			{
				oss << QString("%1: <b>%2</b> %5 (%3: <b>%4</b> %5)").arg(sb, QString::number(getSurfaceBrightness(core, flagUseArcsecSurfaceBrightness), 'f', 2),
				                                                          ae, QString::number(getSurfaceBrightnessWithExtinction(core, flagUseArcsecSurfaceBrightness), 'f', 2), mu) << "<br/>";
			}
			else
				oss << QString("%1: <b>%2</b> %3").arg(sb, QString::number(getSurfaceBrightness(core, flagUseArcsecSurfaceBrightness), 'f', 2), mu) << "<br/>";

			if (getContrastIndex(core)<99.f)
				oss << QString("%1: %2").arg(q_("Contrast index"), QString::number(getContrastIndex(core), 'f', 2)) << "<br/>";
		}
	}

	oss << getCommonInfoString(core, flags);

	if (flags&Size && majorAxisSize>0.f)
	{
		QString majorAxS, minorAxS, sizeAx = q_("Size");
		if (withDecimalDegree)
		{
			majorAxS = StelUtils::radToDecDegStr(static_cast<double>(majorAxisSize)*M_PI/180., 5, false, true);
			minorAxS = StelUtils::radToDecDegStr(static_cast<double>(minorAxisSize)*M_PI/180., 5, false, true);
		}
		else
		{
			majorAxS = StelUtils::radToDmsPStr(static_cast<double>(majorAxisSize)*M_PI/180., 2);
			minorAxS = StelUtils::radToDmsPStr(static_cast<double>(minorAxisSize)*M_PI/180., 2);
		}

		if (fuzzyEquals(majorAxisSize, minorAxisSize) || minorAxisSize==0.f)
			oss << QString("%1: %2").arg(sizeAx, majorAxS) << "<br/>";
		else
		{
			oss << QString("%1: %2 x %3").arg(sizeAx, majorAxS, minorAxS) << "<br/>";
			if (orientationAngle>0)
				oss << QString("%1: %2%3").arg(q_("Orientation angle")).arg(orientationAngle).arg(QChar(0x00B0)) << "<br/>";
		}
	}
	if (flags&Size)
		oss << getExtraInfoStrings(Size).join("");

	if (flags&Distance)
	{
		const float parallax = readExtras().parallax;
		const float parallaxErr = readExtras().parallaxErr;
		if (qAbs(parallax)>0.f)
		{
			QString dx;
			// distance in light years from parallax
			float distance = 3.162e-5f/(qAbs(parallax)*4.848e-9f);
			float distanceErr = 0.f;

			if (parallaxErr>0.f)
				distanceErr = qAbs(3.162e-5f/(qAbs(parallaxErr + parallax)*4.848e-9f) - distance);

			if (distanceErr>0.f)
				dx = QString("%1%2%3").arg(QString::number(distance, 'f', 3)).arg(QChar(0x00B1)).arg(QString::number(distanceErr, 'f', 3));
			else
				dx = QString("%1").arg(QString::number(distance, 'f', 3));

			if (oDistance==0.f)
			{
				// TRANSLATORS: Unit of measure for distance - Light Years
				QString ly = qc_("ly", "distance");
				oss << QString("%1: %2 %3").arg(q_("Distance"), dx, ly) << "<br/>";
			}
		}

		if (oDistance>0.f)
		{
			QString dx, dy;
			float dc = 3262.f;
			int ms = 1;
			//TRANSLATORS: Unit of measure for distance - kiloparsecs
			QString dupc = qc_("kpc", "distance");
			//TRANSLATORS: Unit of measure for distance - Light Years
			QString duly = qc_("ly", "distance");

			float distance = oDistance;
			float distanceErr = oDistanceErr;
			float distanceLY = oDistance*dc;
			float distanceErrLY= oDistanceErr*dc;
			if (oDistance>=1000.f)
			{
				distance = oDistance/1000.f;
				distanceErr = oDistanceErr/1000.f;
				//TRANSLATORS: Unit of measure for distance - Megaparsecs
				dupc = qc_("Mpc", "distance");
			}

			if (distanceLY>=1e6f)
			{
				distanceLY /= 1e6f;
				distanceErrLY /= 1e6f;
				ms = 3;
				//TRANSLATORS: Unit of measure for distance - Millions of Light Years
				duly = qc_("M ly", "distance");
			}

			if (oDistanceErr>0.f)
			{
				dx = QString("%1%2%3").arg(QString::number(distance, 'f', 3)).arg(QChar(0x00B1)).arg(QString::number(distanceErr, 'f', 3));
				dy = QString("%1%2%3").arg(QString::number(distanceLY, 'f', ms)).arg(QChar(0x00B1)).arg(QString::number(distanceErrLY, 'f', ms));
			}
			else
			{
				dx = QString("%1").arg(QString::number(distance, 'f', 3));
				dy = QString("%1").arg(QString::number(distanceLY, 'f', ms));
			}

			oss << QString("%1: %2 %3 (%4 %5)").arg(q_("Distance"), dx, dupc, dy, duly) << "<br/>";
		}
		oss << getExtraInfoStrings(Distance).join("");
	}

	if (flags&Extra)
	{
		if (redshift<99.f)
		{
			QString z;
			if (redshiftErr>0.f)
				z = QString("%1%2%3").arg(QString::number(redshift, 'f', 6)).arg(QChar(0x00B1)).arg(QString::number(redshiftErr, 'f', 6));
			else
				z = QString("%1").arg(QString::number(redshift, 'f', 6));

			oss << QString("%1: %2").arg(q_("Redshift"), z) << "<br/>";
		}
		const float parallax2 = readExtras().parallax;
		const float parallaxErr2 = readExtras().parallaxErr;
		if (qAbs(parallax2)>0.f)
		{
			QString px;
			if (parallaxErr2>0.f)
				px = QString("%1%2%3").arg(QString::number(qAbs(parallax2), 'f', 3)).arg(QChar(0x00B1)).arg(QString::number(parallaxErr2, 'f', 3));
			else
				px = QString("%1").arg(QString::number(qAbs(parallax2), 'f', 3));

			oss << QString("%1: %2 %3").arg(q_("Parallax"), px, qc_("mas", "parallax")) << "<br/>";
		}
		if (!readExtras().discoverer.isEmpty())
			oss << QString("%1: %2 (%3)").arg(q_("Discoverer"), readExtras().discoverer, StelUtils::localeDiscoveryDateString(readExtras().discoveryYear)) << "<br/>";
		if (!getMorphologicalTypeDescription().isEmpty())
			oss << StelUtils::wrapText(QString("%1: %2.").arg(q_("Morphological description"), getMorphologicalTypeDescription())) << "<br/>";
	}

	oss << getSolarLunarInfoString(core, flags);

	postProcessInfoString(str, flags);

	return str;
}

QVariantMap Nebula::getInfoMap(const StelCore *core) const
{
	QVariantMap map = StelObject::getInfoMap(core);

	map["type"]=getObjectTypeI18n(); // replace "Nebula" type by detail. This is localized.
	map.insert("morpho", getMorphologicalTypeString());
	map.insert("surface-brightness", getSurfaceBrightness(core));
	buildDesignations();
	map.insert("designations", withoutID ? QString() : designations.join(" - "));
	map.insert("bmag", bMag);
	if (vMag < 50 && bMag < 50)
		map.insert("bV", bMag-vMag);
	if (redshift<99.f)
		map.insert("redshift", redshift);

	double axisMajor = getAngularRadius(core)*(2.*M_PI_180);
	double axisMinor = getAngularRadius(core)*(2.*M_PI_180);
	int    axisPA    = 0;
	if (minorAxisSize>0.f)
	{
		axisMajor = majorAxisSize*M_PI_180;
		axisMinor = minorAxisSize*M_PI_180;
		axisPA    = orientationAngle;
	}
	map.insert("axis-major", axisMajor);
	map.insert("axis-major-dd", axisMajor*M_180_PI);
	map.insert("axis-major-deg", StelUtils::radToDecDegStr(axisMajor, 5));
	map.insert("axis-major-dms", StelUtils::radToDmsPStr(axisMajor, 2));
	map.insert("axis-minor", axisMinor);
	map.insert("axis-minor-dd", axisMinor*M_180_PI);
	map.insert("axis-minor-deg", StelUtils::radToDecDegStr(axisMinor, 5));
	map.insert("axis-minor-dms", StelUtils::radToDmsPStr(axisMinor, 2));
	map.insert("orientation-angle", axisPA);
	map.insert("cultural-names", getCultureLabels(StelObject::CulturalDisplayStyle::Native_Pronounce_Translit_Translated_IPA));

	// TODO: more? Names? Data?
	return map;
}

QString Nebula::getEnglishAliases() const
{
	QString aliases;
	int asize = englishAliases.size();
	if (asize!=0)
	{
		if (asize>2) // Special case for many AKA
		{
			bool firstLine = true;
			for(int i=1; i<=asize; i++)
			{
				aliases.append(englishAliases.at(i-1));
				if (i<asize)
					aliases.append(" - ");

				if ((i % 2)==0 && firstLine) // 2 AKA-items on first line!
				{
					aliases.append("<br />");
					firstLine = false;
				}
				if (i>3 && ((i-2) % 4)==0 && !firstLine &&  i<asize)
					aliases.append("<br />");
			}
		}
		else
			aliases = nameI18Aliases.join(" - ");
	}
	return aliases;
}

QString Nebula::getI18nAliases() const
{
	QString aliases;
	int asize = nameI18Aliases.size();
	if (asize!=0)
	{
		if (asize>2) // Special case for many AKA; NOTE: Should we add size to the config data for skyculture?
		{
			bool firstLine = true;
			for(int i=1; i<=asize; i++)
			{
				aliases.append(nameI18Aliases.at(i-1));
				if (i<asize)
					aliases.append(" - ");

				if ((i % 2)==0 && firstLine) // 2 AKA-items on first line!
				{
					aliases.append("<br />");
					firstLine = false;
				}
				if (i>3 && ((i-2) % 4)==0 && !firstLine &&  i<asize)
					aliases.append("<br />");
			}
		}
		else
			aliases = nameI18Aliases.join(" - ");
	}
	return aliases;
}

float Nebula::getVMagnitude(const StelCore* core) const
{
	Q_UNUSED(core)
	return vMag;
}

float Nebula::getBMagnitude(const StelCore* core) const
{
	Q_UNUSED(core)
	return bMag;
}

float Nebula::getBMagnitudeWithExtinction(const StelCore* core) const
{
	Vec3d altAzPos = getAltAzPosGeometric(core);
	altAzPos.normalize();
	float mag = getBMagnitude(core);
	// without the test, planets flicker stupidly in fullsky atmosphere-less view.
	if (core->getSkyDrawer()->getFlagHasAtmosphere())
		core->getSkyDrawer()->getExtinction().forward(altAzPos, &mag);
	return mag;
}

double Nebula::getAngularRadius(const StelCore *) const
{
	return static_cast<double>(0.5f*majorAxisSize);
}

float Nebula::getSelectPriority(const StelCore* core) const
{
	float selectPriority = StelObject::getSelectPriority(core);
	const NebulaMgr* nebMgr = (static_cast<NebulaMgr*>(StelApp::getInstance().getModuleMgr().getModule("NebulaMgr")));
	// minimize unwanted selection of the deep-sky objects
	if (!nebMgr->getFlagHints())
		return selectPriority+3.f;

	float mag = qMin(getVMagnitude(core), getBMagnitude(core));
	float lim = mag;
	float mLim = 15.0f;

	if (nType==NebRegion) // special case for regions
		mag = 3.f;

	if (!objectInDisplayedCatalog() || !objectInDisplayedType())
		return selectPriority+mLim;

	const StelSkyDrawer* drawer = core->getSkyDrawer();

	if (drawer->getFlagNebulaMagnitudeLimit() && (mag>static_cast<float>(drawer->getCustomNebulaMagnitudeLimit())))
		return selectPriority+mLim;

	const float maxMagHint = nebMgr->computeMaxMagHint(drawer);
	// make very easy to select if labeled
	if (surfaceBrightnessUsage)
	{
		lim = mag = getSurfaceBrightness(core);
		mLim += 1.f;
	}

	if (nType==NebDn)
		lim=mLim - mag - 2.0f*qMin(1.5f, majorAxisSize); // Note that "mag" field is used for opacity in this catalog!
	else if (nType==NebHII) // Sharpless and LBN
		lim=10.0f - 2.0f*qMin(1.5f, majorAxisSize); // Unfortunately, in Sh catalog, we always have mag=99=unknown!

	if (std::min(mLim, lim)<=maxMagHint || !outlineSegments.empty() || nType==NebRegion) // High priority for big DSO (with outlines) or regions
		selectPriority = -10.f;
	else
		selectPriority -= 5.f;

	return selectPriority;
}

Vec3f Nebula::getInfoColor(void) const
{
	return (static_cast<NebulaMgr*>(StelApp::getInstance().getModuleMgr().getModule("NebulaMgr")))->getLabelsColor();
}

double Nebula::getCloseViewFov(const StelCore*) const
{
	return majorAxisSize>0.f ? static_cast<double>(majorAxisSize) * 4. : 1.;
}

float Nebula::getSurfaceBrightness(const StelCore* core, bool arcsec) const
{
	const float sq = (arcsec ? 3600.f*3600.f : 3600.f); // arcsec^2 or arcmin^2
	const float mag = qMin(getVMagnitude(core), getBMagnitude(core));
	if (mag<99.f && majorAxisSize>0.f && nType!=NebDn)
		return mag + 2.5f*log10f(getSurfaceArea()*sq);
	else
		return 99.f;
}

float Nebula::getSurfaceBrightnessWithExtinction(const StelCore* core, bool arcsec) const
{
	const float sq = (arcsec ? 3600.f*3600.f : 3600.f); // arcsec^2 or arcmin^2
	const float mag = qMin(getVMagnitudeWithExtinction(core), getBMagnitudeWithExtinction(core));
	if (mag<99.f && majorAxisSize>0.f && nType!=NebDn)
		return mag + 2.5f*log10f(getSurfaceArea()*sq);
	else
		return 99.f;
}

float Nebula::getContrastIndex(const StelCore* core) const
{
	// Compute an extended object's contrast index: http://www.unihedron.com/projects/darksky/NELM2BCalc.html

	// Sky brightness
	const auto luminance = core->getSkyDrawer()->getLightPollutionLuminance();
	const float B_mpsas = StelCore::luminanceToMPSAS(luminance);
	// Compute an extended object's contrast index
	// Source: Clark, R.N., 1990. Appendix E in Visual Astronomy of the Deep Sky, Cambridge University Press and Sky Publishing.
	// URL: http://www.clarkvision.com/visastro/appendix-e.html
	const float emag = getSurfaceBrightnessWithExtinction(core, true);
	if (emag<99.f)
		return -0.4f * (emag - B_mpsas);
	else
		return 99.f;
}

float Nebula::getSurfaceArea(void) const
{
	if (minorAxisSize==0.f)
		return M_PIf*(majorAxisSize/2.f)*(majorAxisSize/2.f); // S = pi*R^2 = pi*(D/2)^2
	else
		return M_PIf*(majorAxisSize/2.f)*(minorAxisSize/2.f); // S = pi*a*b
}

Vec3f Nebula::getHintColor(Nebula::NebulaType nType)
{
	return hintColorMap.value(nType, hintColorMap.value(NebUnknown));
}

float Nebula::getVisibilityLevelByMagnitude(void) const
{
	StelCore* core = StelApp::getInstance().getCore();

	const float mLim = 15.0f;
	float lim = qMin(vMag, bMag);

	if (surfaceBrightnessUsage)
	{
		lim = getSurfaceBrightness(core) - 3.f;
		if (lim > 90.f) lim = mLim + 1.f;
	}
	else
	{
		if (lim > 90.f) lim = mLim;

		// Dark nebulae. Not sure how to assess visibility from opacity? --GZ
		if (nType==NebDn)
		{
			const float mag = getVMagnitude(core);
			// GZ: ad-hoc visibility formula: assuming good visibility if objects of mag9 are visible, "usual" opacity 5 and size 30', better visibility (discernability) comes with higher opacity and larger size,
			// 9-(opac-5)-2*(angularSize-0.5)
			// GZ Not good for non-Barnards. weak opacity and large surface are antagonists. (some LDN are huge, but opacity 2 is not much to discern).
			// The qMin() maximized the visibility gain for large objects.
			if (majorAxisSize>0.f && mag<90.f)
				lim = mLim - mag - 2.0f*qMin(majorAxisSize, 1.5f);
			else
				lim = (catNum(CatB)>0 ? 9.0f : 12.0f); // GZ I assume LDN objects are rather elusive.
		}
		else if (nType==NebHII) // NebHII={Sharpless, LBN, RCW} but also M42.
		{
			// artificially increase visibility of (most) Sharpless and LBN objects. No magnitude recorded:-(
			lim=qMin(lim, 10.0f);
		}
	}

	if (nType==NebRegion) // special case for regions
		lim=3.0f;

	return lim;
}

void Nebula::drawOutlines(StelPainter &sPainter, float maxMagHints) const
{
	size_t segments = outlineSegments.size();
	if (segments==0 || !flagUseOutlines)
		return;

	if (!objectInDisplayedType())
		return;

	// tune limits for outlines
	float oLim = getVisibilityLevelByMagnitude() - 3.f;
	if (oLim>maxMagHints)
		return;

	sPainter.setColor(getHintColor(nType), hintsBrightness);
	sPainter.setLineWidth(1.f * sPainter.getProjector()->getScreenScale());

	StelCore *core=StelApp::getInstance().getCore();
	const Vec3d vel = core->getAberrationVec(core->getJDE());

	// Show outlines
	if (segments>0 && flagUseOutlines && oLim<=maxMagHints)
	{
		unsigned int i, j;
		std::vector<Vec3d> *points;

		sPainter.setBlending(true);
		sPainter.setLineSmooth(true);
		const SphericalCap& viewportHalfspace = sPainter.getProjector()->getBoundingCap();

		for (i=0;i<segments;i++)
		{
			points = outlineSegments[i];

			for (j=0;j<points->size()-1;j++)
			{
				Vec3d point1=points->at(j);
				Vec3d point2=points->at(j+1);
				if (core->getUseAberration())
				{
					point1+=vel;
					point1.normalize();
					point2+=vel;
					point2.normalize();
				}
				sPainter.drawGreatCircleArc(point1, point2, &viewportHalfspace);
			}
		}
		sPainter.setLineSmooth(false);
	}
}

void Nebula::renderDarkNebulaMarker(StelPainter& sPainter, const float x, const float y,
                                    float size, const Vec3f color) const
{
	// Take into account device pixel density and global scale ratio, as we are drawing 2D stuff.
	const float scale = sPainter.getProjector()->getScreenScale();
	size *= scale;

	const float roundRadius = 0.35 * size;
	const int numPointsInArc = std::lround(std::clamp(5*size/35, 5.f, 16.f));
	static std::vector<float> vertexData;
	vertexData.clear();
	vertexData.reserve(numPointsInArc*2*4);
	const float leftOuterX = x - size;
	const float leftInnerX = leftOuterX + roundRadius;
	const float bottomOuterY = y - size;
	const float bottomInnerY = bottomOuterY + roundRadius;
	const float rightOuterX = x + size;
	const float rightInnerX = rightOuterX - roundRadius;
	const float topOuterY = y + size;
	const float topInnerY = topOuterY - roundRadius;
	const float gap = 0.15*size;
	const float*const cossin = StelUtils::ComputeCosSinRhoZone((M_PIf/2)/(numPointsInArc-1),
	                                                           numPointsInArc-1, 0);
	sPainter.setBlending(true);
	sPainter.setLineSmooth(true);
	sPainter.setLineWidth(scale * std::clamp(2*size/35, 1.f, 2.5f));
	sPainter.setColor(color, hintsBrightness);
	sPainter.enableClientStates(true);

	vertexData.clear();
	vertexData.push_back(x-gap);
	vertexData.push_back(bottomOuterY);
	for(int n = 0; n < numPointsInArc; ++n)
	{
		const auto cosa = cossin[2*n], sina = cossin[2*n+1];
		vertexData.push_back(leftInnerX   - roundRadius*sina);
		vertexData.push_back(bottomInnerY - roundRadius*cosa);
	}
	vertexData.push_back(leftOuterX);
	vertexData.push_back(y-gap);
	sPainter.setVertexPointer(2, GL_FLOAT, vertexData.data());
	sPainter.drawFromArray(StelPainter::LineStrip, vertexData.size() / 2, 0, false);

	vertexData.clear();
	vertexData.push_back(leftOuterX);
	vertexData.push_back(y+gap);
	for(int n = 0; n < numPointsInArc; ++n)
	{
		const auto cosa = cossin[2*n], sina = cossin[2*n+1];
		vertexData.push_back(leftInnerX - roundRadius*cosa);
		vertexData.push_back(topInnerY  + roundRadius*sina);
	}
	vertexData.push_back(x-gap);
	vertexData.push_back(topOuterY);
	sPainter.setVertexPointer(2, GL_FLOAT, vertexData.data());
	sPainter.drawFromArray(StelPainter::LineStrip, vertexData.size() / 2, 0, false);

	vertexData.clear();
	vertexData.push_back(x+gap);
	vertexData.push_back(topOuterY);
	for(int n = 0; n < numPointsInArc; ++n)
	{
		const auto cosa = cossin[2*n], sina = cossin[2*n+1];
		vertexData.push_back(rightInnerX + roundRadius*sina);
		vertexData.push_back(topInnerY   + roundRadius*cosa);
	}
	vertexData.push_back(rightOuterX);
	vertexData.push_back(y+gap);
	sPainter.setVertexPointer(2, GL_FLOAT, vertexData.data());
	sPainter.drawFromArray(StelPainter::LineStrip, vertexData.size() / 2, 0, false);

	vertexData.clear();
	vertexData.push_back(rightOuterX);
	vertexData.push_back(y-gap);
	for(int n = 0; n < numPointsInArc; ++n)
	{
		const auto cosa = cossin[2*n], sina = cossin[2*n+1];
		vertexData.push_back(rightInnerX  + roundRadius*cosa);
		vertexData.push_back(bottomInnerY - roundRadius*sina);
	}
	vertexData.push_back(x+gap);
	vertexData.push_back(bottomOuterY);
	sPainter.setVertexPointer(2, GL_FLOAT, vertexData.data());
	sPainter.drawFromArray(StelPainter::LineStrip, vertexData.size() / 2, 0, false);

	sPainter.enableClientStates(false);
}

void Nebula::renderMarkerRoundedRect(StelPainter& sPainter, const float x, const float y,
                                     float size, const Vec3f color) const
{
	// Take into account device pixel density and global scale ratio, as we are drawing 2D stuff.
	const float scale = sPainter.getProjector()->getScreenScale();
	size *= scale;

	const float roundRadius = 0.35 * size;
	const int numPointsInArc = std::lround(std::clamp(5*size/35, 5.f, 16.f));
	static std::vector<float> vertexData;
	vertexData.clear();
	vertexData.reserve(numPointsInArc*2*4);
	const float leftOuterX = x - size;
	const float leftInnerX = leftOuterX + roundRadius;
	const float bottomOuterY = y - size;
	const float bottomInnerY = bottomOuterY + roundRadius;
	const float rightOuterX = x + size;
	const float rightInnerX = rightOuterX - roundRadius;
	const float topOuterY = y + size;
	const float topInnerY = topOuterY - roundRadius;
	const float*const cossin = StelUtils::ComputeCosSinRhoZone((M_PIf/2)/(numPointsInArc-1),
	                                                           numPointsInArc-1, 0);
	for(int n = 0; n < numPointsInArc; ++n)
	{
		const auto cosa = cossin[2*n], sina = cossin[2*n+1];
		vertexData.push_back(leftInnerX   - roundRadius*sina);
		vertexData.push_back(bottomInnerY - roundRadius*cosa);
	}
	for(int n = 0; n < numPointsInArc; ++n)
	{
		const auto cosa = cossin[2*n], sina = cossin[2*n+1];
		vertexData.push_back(leftInnerX - roundRadius*cosa);
		vertexData.push_back(topInnerY  + roundRadius*sina);
	}
	for(int n = 0; n < numPointsInArc; ++n)
	{
		const auto cosa = cossin[2*n], sina = cossin[2*n+1];
		vertexData.push_back(rightInnerX + roundRadius*sina);
		vertexData.push_back(topInnerY   + roundRadius*cosa);
	}
	for(int n = 0; n < numPointsInArc; ++n)
	{
		const auto cosa = cossin[2*n], sina = cossin[2*n+1];
		vertexData.push_back(rightInnerX  + roundRadius*cosa);
		vertexData.push_back(bottomInnerY - roundRadius*sina);
	}
	const auto vertCount = vertexData.size() / 2;
	sPainter.setBlending(true);
	sPainter.setLineSmooth(true);
	sPainter.setLineWidth(scale * std::clamp(2*size/35, 1.f, 2.5f));
	sPainter.setColor(color, hintsBrightness);
	sPainter.enableClientStates(true);
	sPainter.setVertexPointer(2, GL_FLOAT, vertexData.data());
	sPainter.drawFromArray(StelPainter::LineLoop, vertCount, 0, false);
	sPainter.enableClientStates(false);
}

void Nebula::renderRoundMarker(StelPainter& sPainter, const float x, const float y,
                               float size, const Vec3f color, const bool crossed) const
{
	// Take into account device pixel density and global scale ratio, as we are drawing 2D stuff.
	const float scale = sPainter.getProjector()->getScreenScale();
	size *= scale;

	sPainter.setBlending(true);
	sPainter.setLineSmooth(true);
	sPainter.setLineWidth(scale * std::clamp(size/7, 1.f, 2.5f));
	sPainter.setColor(color, hintsBrightness);

	sPainter.drawCircle(x, y, size);
	if(!crossed) return;

	sPainter.enableClientStates(true);
	const float vertexData[] = {x-size, y,
	                            x+size, y,
	                            x, y-size,
	                            x, y+size};
	const auto vertCount = std::size(vertexData) / 2;
	sPainter.setVertexPointer(2, GL_FLOAT, vertexData);
	sPainter.drawFromArray(StelPainter::Lines, vertCount, 0, false);
	sPainter.enableClientStates(false);
}

void Nebula::renderEllipticMarker(StelPainter& sPainter, const float x, const float y, float size,
                                  const float aspectRatio, const float angle, const Vec3f color) const
{
	// Take into account device pixel density and global scale ratio, as we are drawing 2D stuff.
	const float scale = sPainter.getProjector()->getScreenScale();
	size *= scale;

	const float radiusY = 0.35 * size;
	const float radiusX = aspectRatio * radiusY;
	const int numPoints = std::lround(std::clamp(3.f*qMax(radiusX, radiusY), 12.f, 4096.f));
	static std::vector<float> vertexData;
	vertexData.clear();
	vertexData.reserve(numPoints*2);
	const float*const cossin = StelUtils::ComputeCosSinTheta(numPoints);
	const auto cosa = std::cos(angle);
	const auto sina = std::sin(angle);
	for(int n = 0; n < numPoints; ++n)
	{
		const auto cosb = cossin[2*n], sinb = cossin[2*n+1];
		const auto pointX = radiusX*sinb;
		const auto pointY = radiusY*cosb;
		vertexData.push_back(x + pointX*cosa - pointY*sina);
		vertexData.push_back(y + pointY*cosa + pointX*sina);
	}
	const auto vertCount = vertexData.size() / 2;
	sPainter.setBlending(true);
	sPainter.setLineSmooth(true);
	sPainter.setLineWidth(scale * std::clamp(size/40, 1.f, 2.f));
	sPainter.setColor(color, hintsBrightness);
	sPainter.enableClientStates(true);
	sPainter.setVertexPointer(2, GL_FLOAT, vertexData.data());
	sPainter.drawFromArray(StelPainter::LineLoop, vertCount, 0, false);
	sPainter.enableClientStates(false);
}

void Nebula::renderMarkerPointedCircle(StelPainter& sPainter, const float x, const float y,
                                       float size, const Vec3f color, const bool insideRect) const
{
	// Take into account device pixel density and global scale ratio, as we are drawing 2D stuff.
	const float scale = sPainter.getProjector()->getScreenScale();
	size *= scale;

	texPointElement->bind();
	sPainter.setBatchTexture(texPointElement->glName());
	sPainter.setColor(color, hintsBrightness);
	sPainter.setBlending(true, GL_SRC_ALPHA, GL_ONE);
	const auto numPoints = StelUtils::getSmallerPowerOfTwo(std::clamp(int(0.4f*size/scale), 8, 4096));
	const auto spriteSize = std::min(0.25f * 2*M_PIf*size / numPoints, 5.f * scale);
	if(insideRect)
		size -= spriteSize*2;
	const float*const cossin = StelUtils::ComputeCosSinRhoZone((2*M_PIf)/numPoints, numPoints, 0);
	static std::vector<Vec2f> points;
	points.clear();
	points.reserve(numPoints);
	for (int n = 0; n < numPoints; ++n)
	{
		const auto cosa = cossin[2 * n], sina = cossin[2 * n + 1];
		points.emplace_back(x - size * sina, y - size * cosa);
	}
	sPainter.drawSprite2dModeNoDeviceScale(points, spriteSize);
}

float Nebula::getHintSize(StelPainter& sPainter) const
{
	const float size = 6.0f;
	float scaledSize = 0.0f;
	// Should getPixelPerRadAtCenter() not adjust for HiDPI? Apparently it does not!
	if (drawHintProportional)
	{
		const float scale = sPainter.getProjector()->getScreenScale();
		const float angularRadiusAtCenter = static_cast<float>(getAngularRadius(nullptr)) * M_PI_180f;
		const float pixPerRadAtCenter = static_cast<float>(sPainter.getProjector()->getPixelPerRadAtCenter());
		scaledSize = angularRadiusAtCenter * pixPerRadAtCenter / scale;
	}
	// TODO: Is it correct that for NebRegions any catalog data for getAngularRadius() is ignored? And that NebRegions are ALWAYS drawn with larger symbol?
	if (nType==NebRegion)
		scaledSize = 12.f;

	return qMax(size, scaledSize);
}

void Nebula::drawHints(StelPainter& sPainter, const Vec3d& XY, float maxMagHints, StelCore *core) const
{
	if (!objectInDisplayedType())
		return;
	if (outlineSegments.size()>0 && flagUseOutlines)
		return;

	Vec3d win;
	// Check visibility of DSO hints
	if (!(sPainter.getProjector()->projectCheck(XYZ, win)))
		return;

	if (getVisibilityLevelByMagnitude()>maxMagHints)
		return;

	Vec3f color = getHintColor(nType);
	float finalSize = getHintSize(sPainter);

	switch (nType)
	{
		case NebGx:
		case NebIGx:
		case NebAGx:
		case NebQSO:
		case NebPossQSO:
		case NebBLL:
		case NebBLA:
		case NebRGx:
		case NebGxCl:
		{
			// The rotation angle in renderEllipticMarker() is relative to screen. Make sure to compute correct angle from 90+orientationAngle.
			// Find an on-screen direction vector from a point offset somewhat in declination from our object.
			Vec3d XYZrel(getJ2000EquatorialPos(core));
			XYZrel[2]*=0.95; XYZrel.normalize();
			Vec3d XYrel;
			sPainter.getProjector()->project(XYZrel, XYrel);
			const auto screenAngle = atan2(XYrel[1]-XY[1], XYrel[0]-XY[0]);
			renderEllipticMarker(sPainter, XY[0], XY[1], finalSize, 2, screenAngle + orientationAngle*M_PI_180f, color);
			return;
		}
		case NebOc:
		case NebSA:
		case NebSC:
		case NebCl:
		case NebPartOfGx:
			renderMarkerPointedCircle(sPainter, XY[0], XY[1], finalSize, color, false);
			return;
		case NebGc:
			renderRoundMarker(sPainter, XY[0], XY[1], finalSize, color, true);
			return;
		case NebN:
		case NebHII:
		case NebISM:
		case NebMolCld:
		case NebYSO:
		case NebRn:
		case NebSNR:
		case NebBn:
		case NebEn:
		case NebSNC:
		case NebSNRC:
			renderMarkerRoundedRect(sPainter, XY[0], XY[1], finalSize, color);
			return;
		case NebPn:
		case NebPossPN:
		case NebPPN:
			Nebula::texPlanetaryNebula->bind();
			sPainter.setBatchTexture(Nebula::texPlanetaryNebula->glName());
			break;
		case NebDn:
			renderDarkNebulaMarker(sPainter, XY[0], XY[1], finalSize, color);
			return;
		case NebCn:
		{
			renderMarkerRoundedRect(sPainter, XY[0], XY[1], finalSize, getHintColor(NebN));
			renderMarkerPointedCircle(sPainter, XY[0], XY[1], finalSize, getHintColor(NebCl), true);
			return;
		}
		case NebRegion:
			Nebula::texRegion->bind();
			sPainter.setBatchTexture(Nebula::texRegion->glName());
			break;
		case NebEMO:
		case NebStar:
		case NebSymbioticStar:
		case NebEmissionLineStar:
		case NebUnknown:
		//default: // keep this commented out to let clangd warn us of unhandled cases!
			renderRoundMarker(sPainter, XY[0], XY[1], finalSize, color, false);
			return;
	}

	sPainter.setColor(color, hintsBrightness);
	sPainter.setBlending(true, GL_SRC_ALPHA, GL_ONE);

	sPainter.drawSprite2dMode(static_cast<float>(XY[0]), static_cast<float>(XY[1]), finalSize);
}

void Nebula::drawLabel(StelPainter& sPainter, const Vec3d& XY, float maxMagLabel) const
{
	if (!objectInDisplayedType())
		return;

	Vec3d win;
	// Check visibility of DSO labels
	if (!(sPainter.getProjector()->projectCheck(XYZ, win)))
		return;

	if (getVisibilityLevelByMagnitude()>maxMagLabel)
		return;

	sPainter.setColor(labelColor, labelsBrightness);

	const float shift = 15.f + (drawHintProportional ? getHintSize(sPainter) : 0.f);

	QString str = getScreenLabel();
	if (str.isEmpty() || designationUsage)
		str = getDSODesignation();

	sPainter.drawText(static_cast<float>(XY[0]), static_cast<float>(XY[1]), str, 0, shift, shift, false);
}

QString Nebula::getDSODesignation() const
{
	QString str = "";
	// Get designation for DSO with priority as given here.
	if (catalogFilters&CatM && catNum(CatM)>0)
		str = QString("M %1").arg(catNum(CatM));
	else if (catalogFilters&CatC && catNum(CatC)>0)
		str = QString("C %1").arg(catNum(CatC));
	else if (catalogFilters&CatNGC && catNum(CatNGC)>0)
		str = QString("NGC %1").arg(catNum(CatNGC));
	else if (catalogFilters&CatIC && catNum(CatIC)>0)
		str = QString("IC %1").arg(catNum(CatIC));
	else if (catalogFilters&CatB && catNum(CatB)>0)
		str = QString("B %1").arg(catNum(CatB));
	else if (catalogFilters&CatSh2 && catNum(CatSh2)>0)
		str = QString("SH 2-%1").arg(catNum(CatSh2));
	else if (catalogFilters&CatVdB && catNum(CatVdB)>0)
		str = QString("vdB %1").arg(catNum(CatVdB));
	else if (catalogFilters&CatRCW && catNum(CatRCW)>0)
		str = QString("RCW %1").arg(catNum(CatRCW));
	else if (catalogFilters&CatLDN && catNum(CatLDN)>0)
		str = QString("LDN %1").arg(catNum(CatLDN));
	else if (catalogFilters&CatLBN && catNum(CatLBN) > 0)
		str = QString("LBN %1").arg(catNum(CatLBN));
	else if (catalogFilters&CatCr && catNum(CatCr) > 0)
		str = QString("Cr %1").arg(catNum(CatCr));
	else if (catalogFilters&CatMel && catNum(CatMel) > 0)
		str = QString("Mel %1").arg(catNum(CatMel));
	else if (catalogFilters&CatPGC && PGC_nb > 0)
		str = QString("PGC %1").arg(PGC_nb);
	else if (catalogFilters&CatUGC && catNum(CatUGC) > 0)
		str = QString("UGC %1").arg(catNum(CatUGC));
	else if (catalogFilters&CatCed && !catStr(CatCed).isEmpty())
		str = QString("Ced %1").arg(catStr(CatCed));
	else if (catalogFilters&CatArp && catNum(CatArp) > 0)
		str = QString("Arp %1").arg(catNum(CatArp));
	else if (catalogFilters&CatVV && catNum(CatVV) > 0)
		str = QString("VV %1").arg(catNum(CatVV));
	else if (catalogFilters&CatPK && !catStr(CatPK).isEmpty())
		str = QString("PK %1").arg(catStr(CatPK));
	else if (catalogFilters&CatPNG && !catStr(CatPNG).isEmpty())
		str = QString("PN G%1").arg(catStr(CatPNG));
	else if (catalogFilters&CatSNRG && !catStr(CatSNRG).isEmpty())
		str = QString("SNR G%1").arg(catStr(CatSNRG));
	else if (catalogFilters&CatACO && !catStr(CatACO).isEmpty())
		str = QString("Abell %1").arg(catStr(CatACO));
	else if (catalogFilters&CatHCG && !catStr(CatHCG).isEmpty())
		str = QString("HCG %1").arg(catStr(CatHCG));	
	else if (catalogFilters&CatESO && !catStr(CatESO).isEmpty())
		str = QString("ESO %1").arg(catStr(CatESO));
	else if (catalogFilters&CatVdBH && !catStr(CatVdBH).isEmpty())
		str = QString("vdBH %1").arg(catStr(CatVdBH));
	else if (catalogFilters&CatDWB && catNum(CatDWB) > 0)
		str = QString("DWB %1").arg(catNum(CatDWB));
	else if (catalogFilters&CatTr && catNum(CatTr) > 0)
		str = QString("Tr %1").arg(catNum(CatTr));
	else if (catalogFilters&CatSt && catNum(CatSt) > 0)
		str = QString("St %1").arg(catNum(CatSt));
	else if (catalogFilters&CatRu && catNum(CatRu) > 0)
		str = QString("Ru %1").arg(catNum(CatRu));
	else if (catalogFilters&CatVdBHa && catNum(CatVdBHa) > 0)
		str = QString("vdB-Ha %1").arg(catNum(CatVdBHa));

	return str;
}

QString Nebula::getDSODesignationWIC() const
{
	if (!withoutID)
	{
		buildDesignations();
		return designations.first();
	}
	else
		return QString();
}


void Nebula::buildDesignations() const
{
	if (designationsBuilt)
		return;
	designationsBuilt = true;
	if (catNum(CatM) > 0) designations << QString("M %1").arg(catNum(CatM));
	if (catNum(CatC) > 0)  designations << QString("C %1").arg(catNum(CatC));
	if (catNum(CatNGC) > 0) designations << QString("NGC %1").arg(catNum(CatNGC));
	if (catNum(CatIC) > 0) designations << QString("IC %1").arg(catNum(CatIC));
	if (catNum(CatB) > 0) designations << QString("B %1").arg(catNum(CatB));
	if (catNum(CatSh2) > 0) designations << QString("SH 2-%1").arg(catNum(CatSh2));
	if (catNum(CatVdB) > 0) designations << QString("vdB %1").arg(catNum(CatVdB));
	if (catNum(CatRCW) > 0) designations << QString("RCW %1").arg(catNum(CatRCW));
	if (catNum(CatLDN) > 0) designations << QString("LDN %1").arg(catNum(CatLDN));
	if (catNum(CatLBN) > 0) designations << QString("LBN %1").arg(catNum(CatLBN));
	if (catNum(CatCr) > 0) designations << QString("Cr %1").arg(catNum(CatCr));
	if (catNum(CatMel) > 0) designations << QString("Mel %1").arg(catNum(CatMel));
	if (PGC_nb > 0) designations << QString("PGC %1").arg(PGC_nb);
	if (catNum(CatUGC) > 0) designations << QString("UGC %1").arg(catNum(CatUGC));
	if (!catStr(CatCed).isEmpty()) designations << QString("Ced %1").arg(catStr(CatCed));
	if (catNum(CatArp) > 0) designations << QString("Arp %1").arg(catNum(CatArp));
	if (catNum(CatVV) > 0) designations << QString("VV %1").arg(catNum(CatVV));
	if (!catStr(CatPK).isEmpty()) designations << QString("PK %1").arg(catStr(CatPK));
	if (!catStr(CatPNG).isEmpty()) designations << QString("PN G%1").arg(catStr(CatPNG));
	if (!catStr(CatSNRG).isEmpty()) designations << QString("SNR G%1").arg(catStr(CatSNRG));
	if (!catStr(CatACO).isEmpty()) designations << QString("Abell %1").arg(catStr(CatACO));
	if (!catStr(CatHCG).isEmpty()) designations << QString("HCG %1").arg(catStr(CatHCG));
	if (!catStr(CatESO).isEmpty()) designations << QString("ESO %1").arg(catStr(CatESO));
	if (!catStr(CatVdBH).isEmpty()) designations << QString("vdBH %1").arg(catStr(CatVdBH));
	if (catNum(CatDWB) > 0) designations << QString("DWB %1").arg(catNum(CatDWB));
	if (catNum(CatTr) > 0) designations << QString("Tr %1").arg(catNum(CatTr));
	if (catNum(CatSt) > 0) designations << QString("St %1").arg(catNum(CatSt));
	if (catNum(CatRu) > 0) designations << QString("Ru %1").arg(catNum(CatRu));
	if (catNum(CatVdBHa) > 0) designations << QString("vdB-Ha %1").arg(catNum(CatVdBHa));
}

void Nebula::readDSO(QDataStream &in)
{
	float	ra, dec;
	unsigned int oType; // Kludge for MSVC2017

	float parallaxRead = 0.f, parallaxErrRead = 0.f;
	unsigned int NGC=0, IC=0, M=0, C=0, B=0, Sh2=0, VdB=0, RCW=0, LDN=0, LBN=0, Cr=0,
		Mel=0, UGC=0, Arp=0, VV=0, DWB=0, Tr=0, St=0, Ru=0, VdBHa=0;
	QString Ced, PK, PNG, SNRG, ACO, HCG, ESO, VdBH;
	in	>> DSO_nb >> ra >> dec >> bMag >> vMag >> oType >> mTypeString >> majorAxisSize >> minorAxisSize
		>> orientationAngle >> redshift >> redshiftErr >> parallaxRead >> parallaxErrRead >> oDistance >> oDistanceErr
		>> NGC >> IC >> M >> C >> B >> Sh2 >> VdB >> RCW >> LDN >> LBN >> Cr
		>> Mel >> PGC_nb >> UGC >> Ced >> Arp >> VV >> PK >> PNG >> SNRG >> ACO
		>> HCG >> ESO >> VdBH >> DWB >> Tr >> St >> Ru >> VdBHa;

	if (parallaxRead != 0.f || parallaxErrRead != 0.f)
	{
		Extras& e = ownExtras();
		e.parallax = parallaxRead;
		e.parallaxErr = parallaxErrRead;
	}

	static QHash<QString, QString> mTypePool;
	if (!mTypeString.isEmpty())
		mTypeString = *mTypePool.insert(mTypeString, mTypeString);

	Ced = Ced.trimmed();
	PK = PK.trimmed();
	PNG = PNG.trimmed();
	SNRG = SNRG.trimmed();
	ACO = ACO.trimmed();
	HCG = HCG.trimmed();
	ESO = ESO.trimmed();
	VdBH = VdBH.trimmed();

	const std::pair<quint32, unsigned int> numbered[] = {
		{CatNGC, NGC}, {CatIC, IC}, {CatM, M}, {CatC, C}, {CatB, B}, {CatSh2, Sh2},
		{CatLBN, LBN}, {CatLDN, LDN}, {CatRCW, RCW}, {CatVdB, VdB}, {CatCr, Cr},
		{CatMel, Mel}, {CatUGC, UGC}, {CatArp, Arp}, {CatVV, VV}, {CatDWB, DWB},
		{CatTr, Tr}, {CatSt, St}, {CatRu, Ru}, {CatVdBHa, VdBHa}};
	const std::pair<quint32, const QString*> named[] = {
		{CatCed, &Ced}, {CatPK, &PK}, {CatPNG, &PNG}, {CatSNRG, &SNRG},
		{CatACO, &ACO}, {CatHCG, &HCG}, {CatESO, &ESO}, {CatVdBH, &VdBH}};

	catalogueMask = 0;
	for (const auto& entry : numbered)
		if (entry.second > 0)
			catalogueMask |= entry.first;
	for (const auto& entry : named)
		if (!entry.second->isEmpty())
			catalogueMask |= entry.first;
	if (PGC_nb > 0)
		catalogueMask |= CatPGC;
	if (catalogueMask == 0)
	{
		withoutID = true;
		catalogueMask = CatOther;
	}

	rareNumbersOffset = quint32(rareNumbers.size());
	for (const auto& entry : numbered)
		if (entry.second > 0)
			rareNumbers.push_back(entry.second);
	rareStringsOffset = quint32(rareStrings.size());
	for (const auto& entry : named)
		if (!entry.second->isEmpty())
			rareStrings.push_back(*entry.second);

	StelUtils::spheToRect(ra,dec,XYZ);
	Q_ASSERT(fabs(XYZ.normSquared()-1.)<1e-9);
	nType = static_cast<Nebula::NebulaType>(oType);
	updateTypeMask();
}

void Nebula::updateTypeMask()
{
	static const QMap<NebulaType, int>map={
		{NebGx			,  0 },  // m Galaxy
		{NebAGx			,  1 },  // Active galaxy
		{NebRGx			,  1 },  // m Radio galaxy
		{NebIGx			,  2 },  // Interacting galaxy
		{NebQSO			,  1 },  // Quasar
		{NebCl			,  3 },  // Star cluster
		{NebOc			,  3 },  // Open star cluster
		{NebGc			, 11 },  // Globular star cluster, usually in the Milky Way Galaxy
		{NebSA			,  3 },  // Stellar association
		{NebSC			,  3 },  // Star cloud
		{NebN			,  5 },  // A nebula
		{NebPn			,  7 },  // Planetary nebula
		{NebDn			,  6 },  // Dark Nebula
		{NebRn			,  5 },  // Reflection nebula
		{NebBn			,  5 },  // Bipolar nebula
		{NebEn			,  5 },  // Emission nebula
		{NebCn			,  9 },  // Cluster associated with nebulosity
		{NebHII			,  4 },  // HII Region
		{NebSNR			,  8 },  // Supernova remnant
		{NebISM			,  4 },  // Interstellar matter
		{NebEMO			,  5 },  // Emission object (from 24.4)
		{NebBLL			,  1 },  // BL Lac object
		{NebBLA			,  1 },  // Blazar
		{NebMolCld	        ,  6 },  // Molecular Cloud
		{NebYSO			,  6 },  // Young Stellar Object
		{NebPossQSO		,  1 },  // Possible Quasar
		{NebPossPN		,  7 },  // Possible Planetary Nebula
		{NebPPN			,  7 },  // Protoplanetary Nebula
		{NebSNC			,  8 },  // Supernova Candidate
		{NebSNRC		,  8 },  // Supernova Remnant Candidate
		{NebGxCl		, 10 },  // Cluster of Galaxies
		{NebPartOfGx		,  3 },  // Part of a Galaxy (from 24.4)
		{NebUnknown		, 12 }   // m Unknown type, catalog errors, "Unidentified Southern Objects" etc.
	};
	switch (map.value(nType, 12))
	{
		case  0: typeMask = TypeGalaxies; break;
		case  1: typeMask = TypeActiveGalaxies; break;
		case  2: typeMask = TypeInteractingGalaxies; break;
		case  3: typeMask = TypeOpenStarClusters; break;
		case  4: typeMask = TypeHydrogenRegions; break;
		case  5: typeMask = TypeBrightNebulae; break;
		case  6: typeMask = TypeDarkNebulae; break;
		case  7: typeMask = TypePlanetaryNebulae; break;
		case  8: typeMask = TypeSupernovaRemnants; break;
		case  9: typeMask = TypeOpenStarClusters | TypeBrightNebulae | TypeHydrogenRegions; break;
		case 10: typeMask = TypeGalaxyClusters; break;
		case 11: typeMask = TypeGlobularStarClusters; break;
		default: typeMask = TypeOther; break;
	}
}

bool Nebula::objectInDisplayedType() const
{
	if (!flagUseTypeFilters)
		return true;
	return (typeMask & static_cast<quint32>(typeFilters.toInt())) != 0;
}

bool Nebula::objectInDisplayedCatalog() const
{
	return (catalogueMask & static_cast<quint32>(catalogFilters.toInt())) != 0;
}

bool Nebula::objectInAllowedSizeRangeLimits(void) const
{
	bool r = true;
	if (flagUseSizeLimits)
	{
		const double size = 60. * static_cast<double>(qMax(majorAxisSize, minorAxisSize));
		r = (size>=minSizeLimit && size<=maxSizeLimit);
	}
	return r;
}

QString Nebula::getMorphologicalTypeString(void) const
{
	return mTypeString;
}

QString Nebula::getConcentrationClass(QString cc) const
{
	QString r = "";
	static const QStringList glclass = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
	switch(glclass.indexOf(cc))
	{
		case 0:
			r = qc_("high concentration of stars toward the center", "Shapley-Sawyer Concentration Class");
			break;
		case 1:
			r = qc_("dense central concentration of stars", "Shapley-Sawyer Concentration Class");
			break;
		case 2:
			r = qc_("strong inner core of stars", "Shapley-Sawyer Concentration Class");
			break;
		case 3:
			r = qc_("intermediate rich concentrations of stars", "Shapley-Sawyer Concentration Class");
			break;
		case 4:
		case 5:
		case 6:
			r = qc_("intermediate concentrations of stars", "Shapley-Sawyer Concentration Class");
			break;
		case 7:
			r = qc_("rather loose concentration of stars towards the center", "Shapley-Sawyer Concentration Class");
			break;
		case 8:
			r = qc_("loose concentration of stars towards the center", "Shapley-Sawyer Concentration Class");
			break;
		case 9:
			r = qc_("loose concentration of stars", "Shapley-Sawyer Concentration Class");
			break;
		case 10:
			r = qc_("very loose concentration of stars towards the center", "Shapley-Sawyer Concentration Class");
			break;
		case 11:
			r = qc_("almost no concentration towards the center", "Shapley-Sawyer Concentration Class");
			break;
		default:
			r = qc_("undocumented concentration class", "Shapley-Sawyer Concentration Class");
			break;
	}
	return r;
}

QString Nebula::getMorphologicalTypeDescription(void) const
{
	QString m, r = "";

	// Let's avoid showing a wrong morphological description for galaxies
	// NOTE: Is required the morphological description for galaxies?
	if (QList<NebulaType>({NebGx, NebAGx, NebRGx, NebIGx, NebQSO, NebPossQSO, NebBLA, NebBLL, NebGxCl}).contains(nType))
		return QString();

	static const QRegularExpression GlClRx("\\.*(I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII)\\.*");
	int idx = mTypeString.indexOf(GlClRx);
	if (idx>0)
		m = mTypeString.mid(idx);
	else
		m = mTypeString;

	QRegularExpressionMatch GlClMatch=GlClRx.match(m);
	if (GlClMatch.hasMatch()) // Globular Clusters
	{
		if (m.contains("-")) // middle concentration class
		{
			QStringList cc = m.split("-");
			r = QString("%1 -> %2").arg(getConcentrationClass(cc.at(0)), getConcentrationClass(cc.at(1)));
		}
		else
			r = getConcentrationClass(m);
	}

	static const QRegularExpression OClRx("\\.*(I|II|III|IV)(\\d)(p|m|r)(n*|N*|u*|U*|e*|E*)\\.*");
	idx = mTypeString.indexOf(OClRx);
	if (idx>0)
		m = mTypeString.mid(idx);
	else
		m = mTypeString;

	QRegularExpressionMatch OClMatch=OClRx.match(m);
	if (OClMatch.hasMatch()) // Open Clusters
	{
		QStringList rtxt;
		static const QStringList occlass = { "I", "II", "III", "IV"};
		static const QStringList ocrich = { "p", "m", "r"};

		QStringList occlassStrings = {
			qc_("strong central concentration of stars", "Trumpler's Concentration Class"),
			qc_("little central concentration of stars", "Trumpler's Concentration Class"),
			qc_("no noticeable concentration of stars", "Trumpler's Concentration Class"),
			qc_("a star field condensation", "Trumpler's Concentration Class")};

		rtxt << occlassStrings.value(occlass.indexOf(OClMatch.captured(1).trimmed()),
			 qc_("undocumented concentration class", "Trumpler's Concentration Class"));

		QStringList oclBRangeStrings = {
			qc_("small brightness range of cluster members", "Trumpler's Brightness Class"),
			qc_("medium brightness range of cluster members", "Trumpler's Brightness Class"),
			qc_("large brightness range of cluster members", "Trumpler's Brightness Class")};

		rtxt << oclBRangeStrings.value(OClMatch.captured(2).toInt()-1,
			qc_("undocumented brightness range of cluster members", "Trumpler's Brightness Class"));

		QStringList ocrichStrings = {
			qc_("poor cluster with less than 50 stars", "Trumpler's Number of Members Class"),
			qc_("moderately rich cluster with 50-100 stars", "Trumpler's Number of Members Class"),
			qc_("rich cluster with more than 100 stars", "Trumpler's Number of Members Class")};

		rtxt << ocrichStrings.value(ocrich.indexOf(OClMatch.captured(3).trimmed()),
			qc_("undocumented number of members class", "Trumpler's Number of Members Class"));

		if (!OClMatch.captured(4).trimmed().isEmpty())
			rtxt << qc_("the cluster lies within nebulosity", "nebulosity factor of open clusters");

		r = rtxt.join(", ");
	}

	static const QRegularExpression VdBRx("\\.*(I|II|I-II|II P|P),\\s+(VBR|VB|BR|M|F|VF|:)\\.*");
	idx = mTypeString.indexOf(VdBRx);
	if (idx>0)
		m = mTypeString.mid(idx);
	else
		m = mTypeString;

	QRegularExpressionMatch VdBMatch=VdBRx.match(m);
	if (VdBMatch.hasMatch()) // Reflection Nebulae
	{
		QStringList rtx;
		static const QStringList rnclass = { "I", "II", "I-II", "II P", "P"};
		static const QStringList rnbrightness = { "VBR", "VB", "BR", "M", "F", "VF", ":"};
		switch(rnbrightness.indexOf(VdBMatch.captured(2).trimmed()))
		{
			case 0:
			case 1:
				rtx << qc_("very bright", "Reflection Nebulae Brightness");
				break;
			case 2:
				rtx << qc_("bright", "Reflection Nebulae Brightness");
				break;
			case 3:
				rtx << qc_("moderate brightness", "Reflection Nebulae Brightness");
				break;
			case 4:
				rtx << qc_("faint", "Reflection Nebulae Brightness");
				break;
			case 5:
				rtx << qc_("very faint", "Reflection Nebulae Brightness");
				break;
			case 6:
				rtx << qc_("uncertain brightness", "Reflection Nebulae Brightness");
				break;
			default:
				rtx << qc_("undocumented brightness of reflection nebulae", "Reflection Nebulae Brightness");
				break;
		}
		switch(rnclass.indexOf(VdBMatch.captured(1).trimmed()))
		{
			case 0:
				rtx << qc_("the illuminating star is embedded in the nebulosity", "Reflection Nebulae Classification");
				break;
			case 1:
				rtx << qc_("star is located outside the illuminated nebulosity", "Reflection Nebulae Classification");
				break;
			case 2:
				rtx << qc_("star is located on the corner of the illuminated nebulosity", "Reflection Nebulae Classification");
				break;
			case 3:
			{
				// TRANSLATORS: peculiar: odd or unusual, cf. peculiar star, peculiar galaxy
				rtx << qc_("star is located outside the illuminated peculiar nebulosity", "Reflection Nebulae Classification");
				break;
			}
			case 4:
			{
				// TRANSLATORS: peculiar: odd or unusual, cf. peculiar star, peculiar galaxy
				rtx << qc_("the illuminated peculiar nebulosity", "Reflection Nebulae Classification");
				break;
			}
			default:
				rtx << qc_("undocumented reflection nebulae", "Reflection Nebulae Classification");
				break;
		}
		r = rtx.join(", ");
	}


	static const QRegularExpression HIIRx("\\.*(\\d+),\\s+(\\d+),\\s+(\\d+)\\.*");
	idx = mTypeString.indexOf(HIIRx);
	if (idx>0)
		m = mTypeString.mid(idx);
	else
		m = mTypeString;

	QRegularExpressionMatch HIIMatch=HIIRx.match(m);
	if (HIIMatch.hasMatch()) // HII regions
	{
		const int form	     = HIIMatch.captured(1).toInt();
		const int structure  = HIIMatch.captured(2).toInt();
		const int brightness = HIIMatch.captured(3).toInt();
		const QStringList formList={
			q_("circular form"),
			q_("elliptical form"),
			q_("irregular form")};
		const QStringList structureList={
			q_("amorphous structure"),
			q_("conventional structure"),
			q_("filamentary structure")};
		const QStringList brightnessList={
			qc_("faintest", "HII region brightness"),
			qc_("moderate brightness", "HII region brightness"),
			qc_("brightest", "HII region brightness")};

		QStringList morph;
		morph << formList.value(form-1, q_("undocumented form"));
		morph << structureList.value(structure-1, q_("undocumented structure"));
		morph << brightnessList.value(brightness-1, q_("undocumented brightness"));

		r = morph.join(", ");
	}

	if (nType==NebSNR)
	{
		const QString delim =r.isEmpty() ? "" : "; ";

		if (mTypeString.contains("S") && !mTypeString.contains("S?"))
			r = qc_("remnant shows a shell radio structure", "supernova remnant structure classification") + delim + r;

		if (mTypeString.contains("F") && !mTypeString.contains("F?"))
			r = qc_("remnant shows a filled center ('plerion') radio structure", "supernova remnant structure classification") + delim + r;

		if (mTypeString.contains("C") && !mTypeString.contains("C?"))
			r = qc_("remnant shows a composite (or combination) radio structure", "supernova remnant structure classification") + delim + r;

		if (mTypeString.contains("S?"))
			r = qc_("remnant shows a shell radio structure with some uncertainty", "supernova remnant structure classification") + delim + r;

		if (mTypeString.contains("F?"))
			r = qc_("remnant shows a filled center ('plerion') radio structure with some uncertainty", "supernova remnant structure classification") + delim + r;

		if (mTypeString.contains("C?"))
			r = qc_("remnant shows a composite (or combination) radio structure with some uncertainty", "supernova remnant structure classification") + delim + r;
	}

	return r;
}

QString Nebula::getTypeStringI18n(Nebula::NebulaType nType)
{
	return q_(typeEnglishStringMap.value(nType, q_("undocumented type")));
}

Vec3d Nebula::getJ2000EquatorialPos(const StelCore* core) const
{
	if ((core) && (core->getUseAberration()) && (core->getCurrentPlanet()))
	{
		Vec3d pos=XYZ;
		Q_ASSERT_X(fabs(pos.normSquared()-1.0)<0.0001, "Nebula aberration", "vertex length not unity");
		//pos.normalize(); // Yay - not required!
		Vec3d vel=core->getAberrationVec(core->getJDE());
		pos+=vel;
		pos.normalize();
		return pos;
	}
	else
	{
		return XYZ;
	}
}

// Implemented:
// * Name
// * CatalogNumber
// * Magnitude
// RaDecJ2000
// RaDecOfDate
// AltAzi
// * Distance
// Elongation
// Size
// Velocity
// ProperMotion
// Extra
// HourAngle
// * AbsoluteMagnitude
// GalacticCoord
// SupergalacticCoord
// OtherCoord
// * ObjectType
// EclipticCoordJ2000
// EclipticCoordOfDate
// * IAUConstellation
// - CulturalConstellation
// SiderealTime
// RTSTime
// SolarLunarPosition
QString Nebula::getNarration(const StelCore *core, const InfoStringGroup &flags) const
{
	const Vec3d pos = getEquinoxEquatorialPos(core);

	QString res;

	// Name
	const QString designation = getDSODesignationWIC();
	QStringList names = {getNameI18n()};
	if (hasCulturalNames)
	{
		for (const StelObject::CulturalName &cName: readExtras().culturalNames)
			names.append(cName.translatedI18n);
	}
	names.removeDuplicates();
	names.removeAll("");

	if (flags&Name)
	{
		res=designation;
		if (!names.isEmpty())
			res.append(", " +  q_("called") + " " + names.first() + ","); // TBD: Provide more than 1 name?
	}
	if (flags&ObjectType)// Type
		res.append(" " + q_("is") + " " + typeI18nNebulaStringMap.value(nType, qc_("an object of unknown type", "Nebula narration")) );

	if (flags&Distance)// Distance. Taken from getInfoString. Maybe move that to a private method...
	{
		const float parallax = readExtras().parallax;
		const float parallaxErr = readExtras().parallaxErr;
		if (qAbs(parallax)>0.f)
		{
			QString dx;
			// distance in light years from parallax
			float distance = 3.162e-5f/(qAbs(parallax)*4.848e-9f);
			float distanceErr = 0.f;

			if (parallaxErr>0.f)
				distanceErr = qAbs(3.162e-5f/(qAbs(parallaxErr + parallax)*4.848e-9f) - distance);

			if (distanceErr>0.f)
				dx = QString("%1%2%3").arg(StelUtils::narrateDecimal(distance, 3)).arg(QChar(0x00B1)).arg(StelUtils::narrateDecimal(distanceErr, 3));
			else
				dx = QString("%1").arg(StelUtils::narrateDecimal(distance, 3));

			if (oDistance==0.f)
			{
				// TRANSLATORS: Unit of measure for distance - Light Years
				QString ly = qc_("light years", "distance");
				res.append(QString("%1 %2 %3").arg(qc_("in a distance of", "object narration"), dx, ly));
			}
		}
		if (oDistance>0.f)
		{
			QString dx, dy;
			float dc = 3262.f;
			int ms = 1;
			//TRANSLATORS: Unit of measure for distance - kiloparsecs
			QString dupc = qc_("kiloparsec", "object narration");
			//TRANSLATORS: Unit of measure for distance - Light Years
			QString duly = qc_("light years", "object narration");

			float distance = oDistance;
			float distanceErr = oDistanceErr;
			float distanceLY = oDistance*dc;
			float distanceErrLY= oDistanceErr*dc;
			if (oDistance>=1000.f)
			{
				distance = oDistance/1000.f;
				distanceErr = oDistanceErr/1000.f;
				//TRANSLATORS: Unit of measure for distance - Megaparsecs
				dupc = qc_("Megaparsec", "object narration");
			}

			if (distanceLY>=1e6f)
			{
				distanceLY /= 1e6f;
				distanceErrLY /= 1e6f;
				ms = 3;
				//TRANSLATORS: Unit of measure for distance - Millions of Light Years
				duly = qc_("Million light years", "object narration");
			}

			if (oDistanceErr>0.f)
			{
				dx = QString("%1%2%3").arg(StelUtils::narrateDecimal(distance, 3)).arg(QChar(0x00B1)).arg(StelUtils::narrateDecimal(distanceErr, 3));
				dy = QString("%1%2%3").arg(StelUtils::narrateDecimal(distanceLY, ms)).arg(QChar(0x00B1)).arg(StelUtils::narrateDecimal(distanceErrLY, ms));
			}
			else
			{
				dx = QString("%1").arg(StelUtils::narrateDecimal(distance, 3));
				dy = QString("%1").arg(StelUtils::narrateDecimal(distanceLY, ms));
			}

			res.append(QString("%1 %2 %3 (%4 %5)").arg(qc_("in a distance of", "object narration"), dx, dupc, dy, duly));
		}
	}

	if (flags&IAUConstellation) // IAU Constellation
	{
		const QString iauConstellation = ConstellationMgr::getIAUconstellationName(core->getIAUConstellation(pos));
		res.append(" " + qc_("in the constellation of", "object narration") + " " + iauConstellation + ". ");
	}

	if (flags&Extra) // redshift, parallax, Discovery
	{
		if (redshift<99.f && nType<=NebQSO) // useful for galaxy types only.
		{
			QString z;
			if (redshiftErr>0.f)
				z = QString("%1 %2 %3").arg(StelUtils::narrateDecimal(redshift, 6), qc_("plus minus", "object narration"), StelUtils::narrateDecimal(redshiftErr, 6));
			else
				z = QString("%1").arg(StelUtils::narrateDecimal(redshift, 6));

			res += QString("%1 %2").arg(qc_("Its redshift is", "object narration"), z);
		}
		const float parallax2 = readExtras().parallax;
		const float parallaxErr2 = readExtras().parallaxErr;
		if (qAbs(parallax2)>0.5f)
		{
			QString px;
			if (parallaxErr2>0.f)
				px = QString("%1 %2 %3").arg(StelUtils::narrateDecimal(qAbs(parallax2), 3), qc_("plus minus", "object narration"), StelUtils::narrateDecimal(parallaxErr2, 3));
			else
				px = QString("%1").arg(StelUtils::narrateDecimal(qAbs(parallax2), 3));

			res += QString("%1 %2 %3").arg(qc_("Its Parallax", "object narration"), px, qc_("milli-arcseconds", "parallax"));
		}


		if (!readExtras().discoverer.isEmpty())
		{
			res.append(qc_("It was discovered by", "object narration") + " " + readExtras().discoverer);
			if (!readExtras().discoveryYear.isEmpty())
				res.append(" " + qc_("in the year", "object narration") + " " + readExtras().discoveryYear);
			res.append(". ");
		}

		// Shape
		QString morph=getMorphologicalTypeDescription();
		if (!morph.isEmpty())
		{
			res.append(qc_("Morphologically:", "object narration") + " " + morph + ".");
		}
	}
	return res;
}

QMap<Nebula::NebulaType, QString>Nebula::typeI18nNebulaStringMap;

void Nebula::updateI18n()
{
	// The word forms should be with article if needed by the language
	typeI18nNebulaStringMap=
	{
		{ Nebula::NebGx			,  qc_("a Galaxy", "Nebula narration")},
		{ Nebula::NebAGx		,  qc_("an Active galaxy", "Nebula narration")},
		{ Nebula::NebRGx		,  qc_("a Radio galaxy", "Nebula narration")},
		{ Nebula::NebIGx		,  qc_("an Interacting galaxy", "Nebula narration")},
		{ Nebula::NebQSO		,  qc_("a Quasar", "Nebula narration")},
		{ Nebula::NebCl			,  qc_("a Star cluster", "Nebula narration")},
		{ Nebula::NebOc			,  qc_("an Open star cluster", "Nebula narration")},
		{ Nebula::NebGc			,  qc_("a Globular star cluster", "Nebula narration")},
		{ Nebula::NebSA			,  qc_("a Stellar association", "Nebula narration")},
		{ Nebula::NebSC			,  qc_("a Star cloud", "Nebula narration")},
		{ Nebula::NebN			,  qc_("A nebula", "Nebula narration")},
		{ Nebula::NebPn			,  qc_("a Planetary nebula", "Nebula narration")},
		{ Nebula::NebDn			,  qc_("a Dark Nebula", "Nebula narration")},
		{ Nebula::NebRn			,  qc_("a Reflection nebula", "Nebula narration")},
		{ Nebula::NebBn			,  qc_("a Bipolar nebula", "Nebula narration")},
		{ Nebula::NebEn			,  qc_("an emission nebula", "Nebula narration")},
		{ Nebula::NebCn			,  qc_("a cluster associated with nebulosity", "Nebula narration")},
		{ Nebula::NebHII		,  qc_("H two region", "Nebula narration")},
		{ Nebula::NebSNR		,  qc_("a Supernova remnant", "Nebula narration")},
		{ Nebula::NebISM		,  qc_("Interstellar matter", "Nebula narration")},
		{ Nebula::NebEMO		,  qc_("an Emission object", "Nebula narration")},
		{ Nebula::NebBLL		,  qc_("a BL Lacertae object", "Nebula narration")},
		{ Nebula::NebBLA		,  qc_("a Blazar", "Nebula narration")},
		{ Nebula::NebMolCld		,  qc_("a Molecular Cloud", "Nebula narration")},
		{ Nebula::NebYSO		,  qc_("a Young Stellar Object", "Nebula narration")},
		{ Nebula::NebPossQSO		,  qc_("a Possible Quasar", "Nebula narration")},
		{ Nebula::NebPossPN		,  qc_("a Possible Planetary Nebula", "Nebula narration")},
		{ Nebula::NebPPN		,  qc_("a Protoplanetary Nebula", "Nebula narration")},
		{ Nebula::NebStar		,  qc_("a Star", "Nebula narration")},
		{ Nebula::NebSymbioticStar	,  qc_("a Symbiotic Star", "Nebula narration")},
		{ Nebula::NebEmissionLineStar	,  qc_("an Emission-line Star", "Nebula narration")},
		{ Nebula::NebSNC		,  qc_("a Supernova Candidate", "Nebula narration")},
		{ Nebula::NebSNRC		,  qc_("a supernova Remnant Candidate", "Nebula narration")},
		{ Nebula::NebGxCl		,  qc_("a cluster of Galaxies", "Nebula narration")},
		{ Nebula::NebPartOfGx		,  qc_("a part of a Galaxy", "Nebula narration")},
		{ Nebula::NebRegion		,  qc_("a region of the sky", "Nebula narration")},
		{ Nebula::NebUnknown		,  qc_("an object of unknown type", "Nebula narration")}
	};
}
