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

#ifndef NEBULA_HPP
#define NEBULA_HPP

#include "StelObject.hpp"
#include "OptionalString.hpp"
#include "OptionalList.hpp"
#include "StelTranslator.hpp"
#include "StelTextureTypes.hpp"

#include <QString>
#include <vector>

class StelPainter;
class QDataStream;

// This only draws nebula icons. For the DSO images, see StelSkylayerMgr and StelSkyImageTile.
class Nebula : public StelObject
{
friend class NebulaMgr;

	//Required for the correct working of the Q_FLAG macro (which requires a MOC pass)
	Q_GADGET
public:
	static const QString NEBULA_TYPE;

	enum CatalogGroupFlags
	{
		CatNone				= 0x00000000,	//!< Nothing selected
		CatNGC				= 0x00000001,	//!< New General Catalogue (NGC)
		CatIC				= 0x00000002,	//!< Index Catalogue (IC)
		CatM				= 0x00000004,	//!< Messier Catalog (M)
		CatC				= 0x00000008,	//!< Caldwell Catalogue (C)
		CatB				= 0x00000010,	//!< Barnard Catalogue (B)
		CatSh2				= 0x00000020,	//!< Sharpless Catalogue (Sh 2)
		CatLBN				= 0x00000040,	//!< Lynds' Catalogue of Bright Nebulae (LBN)
		CatLDN				= 0x00000080,	//!< Lynds' Catalogue of Dark Nebulae (LDN)
		CatRCW				= 0x00000100,	//!< A catalogue of Hα-emission regions in the southern Milky Way (RCW)
		CatVdB				= 0x00000200,	//!< van den Bergh Catalogue of reflection nebulae (vdB)
		CatCr				= 0x00000400,	//!< Collinder Catalogue (Cr or Col)
		CatMel				= 0x00000800,	//!< Melotte Catalogue of Deep Sky Objects (Mel)
		CatPGC				= 0x00001000,	//!< HYPERLEDA. I. Catalog of galaxies (PGC)
		CatUGC				= 0x00002000,	//!< The Uppsala General Catalogue of Galaxies
		CatCed				= 0x00004000,	//!< Cederblad Catalog of bright diffuse Galactic nebulae (Ced)
		CatArp				= 0x00008000,	//!< Atlas of Peculiar Galaxies (Arp)
		CatVV				= 0x00010000,	//!< Interacting galaxies catalogue by Vorontsov-Velyaminov (VV)
		CatPK				= 0x00020000,	//!< Catalogue of Galactic Planetary Nebulae (PK)
		CatPNG				= 0x00040000,	//!< Strasbourg-ESO Catalogue of Galactic Planetary Nebulae (Acker+, 1992) (PN G)
		CatSNRG				= 0x00080000,	//!< A catalogue of Galactic supernova remnants (Green, 2014) (SNR G)
		CatACO				= 0x00100000,	//!< A Catalog of Rich Clusters of Galaxies (Abell+, 1989) (ACO)
		CatHCG				= 0x00200000,	//!< Hickson, Compact Group (Hickson+ 1982) (HCG)
		CatESO				= 0x00400000,	//!< ESO/Uppsala Survey of the ESO(B) Atlas (Lauberts, 1982) (ESO)
		CatVdBH				= 0x00800000,	//!< Catalogue of southern stars embedded in nebulosity (van den Bergh+, 1975) (vdBH)
		CatDWB				= 0x01000000,	//!< Catalogue and distances of optically visible H II regions (Dickel+, 1969) (DWB)
		CatTr				= 0x02000000,	//!< Trumpler Catalogue (Tr)
		CatSt				= 0x04000000,	//!< Stock Catalogue (St)
		CatRu				= 0x08000000,	//!< Ruprecht Catalogue (Ru)
		CatVdBHa			= 0x10000000,	//!< van den Bergh-Hagen Catalogue (vdB-Ha)
		CatOther				= 0x20000000,	//!< without ID
		CatAll				= 0xFFFFFFFF	//!< All catalogs selected
	};
	Q_DECLARE_FLAGS(CatalogGroup, CatalogGroupFlags)
	Q_FLAG(CatalogGroup)

	enum TypeGroupFlags
	{
		TypeNone			= 0x00000000,	//!< Nothing selected
		TypeGalaxies			= 0x00000001,	//!< Galaxies
		TypeActiveGalaxies		= 0x00000002,	//!< Different Active Galaxies
		TypeInteractingGalaxies		= 0x00000004,	//!< Interacting Galaxies
		TypeOpenStarClusters		= 0x00000008,	//!< Open Star Clusters
		TypeGlobularStarClusters		= 0x00000010,	//!< Globular Star Clusters
		TypeHydrogenRegions		= 0x00000020,	//!< Hydrogen Regions
		TypeBrightNebulae		= 0x00000040,	//!< Bright Nebulae
		TypeDarkNebulae			= 0x00000080,	//!< Dark Nebulae
		TypePlanetaryNebulae		= 0x00000100,	//!< Planetary Nebulae
		TypeSupernovaRemnants		= 0x00000200,	//!< Supernova Remnants
		TypeGalaxyClusters		= 0x00000400,	//!< Galaxy Clusters
		TypeOther			= 0x00000800,	//!< Other types
		TypeAll				= 0xFFFFFFFF	//!< All types
	};
	Q_DECLARE_FLAGS(TypeGroup, TypeGroupFlags)
	Q_FLAG(TypeGroup)

	//! @enum NebulaType Nebula types
	enum NebulaType
	{
		NebGx			= 0,	//!< m Galaxy
		NebAGx			= 1,	//!< Active galaxy
		NebRGx			= 2,	//!< m Radio galaxy
		NebIGx			= 3,	//!< Interacting galaxy
		NebQSO			= 4,	//!< Quasar
		NebCl			= 5,	//!< Star cluster
		NebOc			= 6,	//!< Open star cluster
		NebGc			= 7,	//!< Globular star cluster, usually in the Milky Way Galaxy
		NebSA			= 8,	//!< Stellar association
		NebSC			= 9,	//!< Star cloud
		NebN			= 10,	//!< A nebula
		NebPn			= 11,	//!< Planetary nebula
		NebDn			= 12,	//!< Dark Nebula
		NebRn			= 13,	//!< Reflection nebula
		NebBn			= 14,	//!< Bipolar nebula
		NebEn			= 15,	//!< Emission nebula
		NebCn			= 16,	//!< Cluster associated with nebulosity
		NebHII			= 17,	//!< HII Region
		NebSNR			= 18,	//!< Supernova remnant
		NebISM			= 19,	//!< Interstellar matter
		NebEMO			= 20,	//!< Emission object
		NebBLL			= 21,	//!< BL Lac object
		NebBLA			= 22,	//!< Blazar
		NebMolCld		= 23,	//!< Molecular Cloud
		NebYSO			= 24,	//!< Young Stellar Object
		NebPossQSO		= 25,	//!< Possible Quasar
		NebPossPN		= 26,	//!< Possible Planetary Nebula
		NebPPN			= 27,	//!< Protoplanetary Nebula
		NebStar			= 28,	//!< Star
		NebSymbioticStar		= 29,	//!< Symbiotic Star
		NebEmissionLineStar	= 30,	//!< Emission-line Star
		NebSNC			= 31,	//!< Supernova Candidate
		NebSNRC			= 32,	//!< Supernova Remnant Candidate
		NebGxCl			= 33,	//!< Cluster of Galaxies
		NebPartOfGx		= 34,	//!< Part of a Galaxy
		NebRegion		= 35,	//!< Region of the sky
		NebUnknown		= 36	//!< m Unknown type, catalog errors, "Unidentified Southern Objects" etc.
	};
	Q_ENUM(NebulaType)

	Nebula();
	~Nebula() override;

	//! Nebula support the following InfoStringGroup flags:
	//! - Name
	//! - CatalogNumber
	//! - Magnitude
	//! - RaDec
	//! - AltAzi
	//! - Distance
	//! - Size
	//! - Extra (contains the Nebula type, which might be "Galaxy", "Cluster" or similar)
	//! - PlainText
	//! @param core the StelCore object
	//! @param flags a set of InfoStringGroup items to include in the return value.
	//! @return a QString containing an HTML encoded description of the Nebula.
	QString getInfoString(const StelCore *core, const InfoStringGroup& flags) const override;
	//! In addition to the entries from StelObject::getInfoMap(), Nebula objects provide
	//! - bmag (photometric B magnitude. 99 if unknown)
	//! - morpho (longish description; translated!)
	//! - surface-brightness
	//! - designations (all designations of DSO)
	//! - axis-major (major axis in radians)
	//! - axis-major-dd (major axis in decimal degrees)
	//! - axis-major-deg (major axis in decimal degrees (formatted string))
	//! - axis-major-dms (major axis in DMS format)
	//! - axis-minor (minor axis in radians)
	//! - axis-minor-dd (minor axis in decimal degrees)
	//! - axis-minor-deg (minor axis in decimal degrees (formatted string))
	//! - axis-minor-dms (minor axis in DMS format)
	//! - orientation-angle (in degrees)
	//! A few entries are optional
	//! - bV (B-V index)
	//! - redshift
	QVariantMap getInfoMap(const StelCore *core) const override;
	QString getType() const override {return NEBULA_TYPE;}
	QString getObjectType() const override
	{
		return typeEnglishStringMap.value(nType, "undocumented type");
	}
	QString getObjectTypeI18n() const override
	{
		return q_(typeEnglishStringMap.value(nType, q_("undocumented type")));
	}
	QString getID() const override {return getDSODesignation(); } //this depends on the currently shown catalog flags, should this be changed?
	Vec3d getJ2000EquatorialPos(const StelCore* core) const override;
	double getCloseViewFov(const StelCore* core = Q_NULLPTR) const override;
	float getVMagnitude(const StelCore* core) const override;
	float getSelectPriority(const StelCore* core) const override;
	Vec3f getInfoColor() const override;
	QString getNameI18n() const override {return nameI18;}
	QString getEnglishName() const override {return englishName;}
	QString getEnglishAliases() const;
	QString getI18nAliases() const;

	//! retrieve pronunciation from the first of the cultural names
	QString getNamePronounce() const override {return (readExtras().culturalNames.empty() ? "" : readExtras().culturalNames.constFirst().pronounceI18n);}
	//! Combine screen label from various components, depending on settings in SkyCultureMgr
	QString getScreenLabel() const override;
	//! Combine InfoString label from various components, depending on settings in SkyCultureMgr
	QString getInfoLabel() const override;
	//! Underlying worker that processes the culturalNames
	QStringList getCultureLabels(StelObject::CulturalDisplayStyle style) const;

	//! Return the angular radius of a circle containing the object as seen from the observer
	//! with the circle center assumed to be at getJ2000EquatorialPos().
	//! @return radius in degree. This value is the apparent angular size of the object, and is independent of the current FOV.
	double getAngularRadius(const StelCore*) const override;
	SphericalRegionP getRegion() const override {return SphericalRegionP(new SphericalPoint(XYZ));}

	// Methods specific to Nebula
	void setLabelColor(const Vec3f& v) {labelColor = v;}
	// void setCircleColor(const Vec3f& v) {hintColorMap.insert(NebUnknown, v);}

	//! Get the printable localized nebula Type for @arg nType.
	//! @return the localized nebula type code.
	//! @note for actual objects, use getObjectTypeI18n()
	static QString getTypeStringI18n(Nebula::NebulaType nType);

	NebulaType getDSOType() const {return nType;}

	//! Get the printable morphological nebula Type.
	//! @return the nebula morphological type string.
	QString getMorphologicalTypeString() const;

	float getSurfaceBrightness(const StelCore* core, bool arcsec=false) const;
	float getSurfaceBrightnessWithExtinction(const StelCore* core, bool arcsec=false) const;
	//! Compute an extended object's contrast index
	float getContrastIndex(const StelCore* core) const;

	//! Return object's B magnitude as seen from observer, without including extinction.
	virtual float getBMagnitude(const StelCore* core) const;

	//! Return object's B magnitude as seen from observer including extinction.
	//! Extinction obviously only if atmosphere=on.
	float getBMagnitudeWithExtinction(const StelCore* core) const;

	//! Get the surface area.
	//! @return surface area in square degrees.
	float getSurfaceArea(void) const;

	//! Sets englishName
	void setEnglishName(const QString &name) { englishName = name; }
	//! adds a name to the list of common alias names
	void addNameAlias(const QString &name) { if (!englishAliases.contains(name)) englishAliases.append(name);}
	//! Removes englishName, any aliases and cultural names
	void removeAllNames() { englishName.clear(); englishAliases.clear(); clearCulturalNames();}
	//! Add a name for the currently set skyculture
	void addCulturalName(const StelObject::CulturalName &culturalName){ownExtras().culturalNames.append(culturalName); hasCulturalNames=true;}
	void clearCulturalNames() { if (hasCulturalNames) { ownExtras().culturalNames.clear(); hasCulturalNames=false; } }

	//! Set additional information pieces
	void setDiscoveryData(const QString &discovererName, const QString &year) { Extras& e=ownExtras(); e.discoverer=discovererName; e.discoveryYear=year; }

	//! Get designation for DSO (with priority: M, C, NGC, IC, B, Sh2, vdB, RCW, LDN, LBN, Cr, Mel, PGC, UGC, Ced, Arp, VV, PK, PN G, SNR G, ACO, HCG, ESO, vdBH, DWB, Tr, St, Ru, vdB-Ha)
	//! from the first catalog that is activated.
	//! @return a designation for DSO
	QString getDSODesignation() const;
	//! Get designation for DSO (with priority: M, C, NGC, IC, B, Sh2, vdB, RCW, LDN, LBN, Cr, Mel, PGC, UGC, Ced, Arp, VV, PK, PN G, SNR G, ACO, HCG, ESO, vdBH, DWB, Tr, St, Ru, vdB-Ha)
	//! without accounting for activation of catalogs. This should be preferred to retrieve the most common designation regardless of settings.
	//! @return a designation for DSO
	QString getDSODesignationWIC() const;	

	bool objectInDisplayedCatalog() const;

	bool objectInAllowedSizeRangeLimits() const;

	//! Return a narration text ready for synthesized speech output
	QString getNarration(const StelCore *core, const InfoStringGroup& flags=StelObject::AllInfo) const override;

protected:
	//! Format the magnitude info string for the object
	QString getMagnitudeInfoString(const StelCore *core, const InfoStringGroup& flags, const int decimals=1, const float& magOffset=0.f) const override;
	//! (Re-)Fill TypeI18nNebulaStringMap. Called by NebulaMgr when required.
	static void updateI18n();

private:
	friend struct DrawNebulaFuncObject;

	//! Translate nebula name using the passed translator
	void translateName(const StelTranslator& trans)
	{
		nameI18 = trans.qtranslate(englishName);
		nameI18Aliases.clear();
		for (auto &alias : englishAliases)
			nameI18Aliases.append(trans.qtranslate(alias));

		for (StelObject::CulturalName cName : std::as_const(readExtras().culturalNames))
		{
			cName.pronounceI18n = trans.qtranslate(cName.pronounce);
			cName.translatedI18n = trans.qtranslate(cName.translated);
		}
	}	

	void readDSO(QDataStream& in);

	void drawLabel(StelPainter& sPainter, const Vec3d& XY, float maxMagLabel) const;
	void drawHints(StelPainter& sPainter, const Vec3d& XY, float maxMagHints, StelCore *core) const;
	void drawOutlines(StelPainter& sPainter, float maxMagHints) const;
	void renderDarkNebulaMarker(StelPainter& sPainter, float x, float y, float size, Vec3f color) const;
	void renderRoundMarker(StelPainter& sPainter, float x, float y, float size, Vec3f color, bool crossed) const;
	void renderEllipticMarker(StelPainter& sPainter, float x, float y, float size, float aspectRatio, float angle, Vec3f color) const;
	void renderMarkerRoundedRect(StelPainter& sPainter, float x, float y, float size, Vec3f color) const;
	void renderMarkerPointedCircle(StelPainter& sPainter, float x, float y, float size, Vec3f color, bool insideRect) const;

	bool objectInDisplayedType() const;

	static Vec3f getHintColor(Nebula::NebulaType nType);
	float getVisibilityLevelByMagnitude() const;
	float getHintSize(StelPainter& sPainter) const;

	//! Get the printable description of morphological nebula type.
	//! @return the nebula morphological type string.
	QString getMorphologicalTypeDescription() const;

	//! Get the description of concentration class of globular clusters
	QString getConcentrationClass(QString cc) const;

	unsigned int DSO_nb;
	unsigned int PGC_nb;        //!< PGC number (Catalog of galaxies)
	quint32 rareNumbersOffset = 0;
	quint32 rareStringsOffset = 0;
	bool withoutID;
	mutable bool designationsBuilt = false;
	bool hasExtras = false;
	bool hasCulturalNames = false;
	quint32 catalogueMask = 0;
	quint32 typeMask = 0;

	static constexpr quint32 numericCatalogues = CatNGC|CatIC|CatM|CatC|CatB|CatSh2|CatLBN|CatLDN
		|CatRCW|CatVdB|CatCr|CatMel|CatUGC|CatArp|CatVV|CatDWB|CatTr|CatSt|CatRu|CatVdBHa;
	static constexpr quint32 stringCatalogues = CatCed|CatPK|CatPNG|CatSNRG|CatACO|CatHCG|CatESO|CatVdBH;
	static std::vector<unsigned int> rareNumbers;
	static std::vector<QString> rareStrings;
	static const QString noDesignation;

	unsigned int catNum(quint32 bit) const
	{
		if (!(catalogueMask & bit))
			return 0;
		return rareNumbers[rareNumbersOffset
			+ quint32(qPopulationCount(quint32(catalogueMask & numericCatalogues & (bit-1))))];
	}
	const QString& catStr(quint32 bit) const
	{
		if (!(catalogueMask & bit))
			return noDesignation;
		return rareStrings[rareStringsOffset
			+ quint32(qPopulationCount(quint32(catalogueMask & stringCatalogues & (bit-1))))];
	}
	void updateTypeMask();

	struct Extras
	{
		QString discoverer;
		QString discoveryYear;
		QList<StelObject::CulturalName> culturalNames;
		float parallax = 0.f;
		float parallaxErr = 0.f;
	};
	static QHash<unsigned int, Extras> extrasByDSO;
	static const Extras emptyExtras;
	Extras& ownExtras() { hasExtras = true; return extrasByDSO[DSO_nb]; }
	const Extras& readExtras() const
	{
		if (!hasExtras)
			return emptyExtras;
		const auto it = extrasByDSO.constFind(DSO_nb);
		return it == extrasByDSO.constEnd() ? emptyExtras : *it;
	}
	OptionalString englishName;        //!< English (preferred) name
	OptionalList<QStringList> englishAliases; //!< English aliases
	OptionalString nameI18;            //!< Nebula (preferred) name in user language
	OptionalList<QStringList> nameI18Aliases; //!< Nebula aliases in user language
	QString mTypeString;        //!< Morphological type of object (as string)
	float bMag;                 //!< B magnitude
	float vMag;                 //!< V magnitude. For Dark Nebulae, opacity is stored here.
	float majorAxisSize;        //!< Major axis size in degrees
	float minorAxisSize;        //!< Minor axis size in degrees
	int orientationAngle;       //!< Orientation angle in degrees
	float oDistance;            //!< distance (kpc)
	float oDistanceErr;         //!< Error of distance (kpc)
	float redshift;
	float redshiftErr;
	Vec3d XYZ;                  //!< Cartesian equatorial position (J2000.0)
	NebulaType nType;

	mutable OptionalList<QStringList> designations;       // List of Catalog number entries
	void buildDesignations() const;

	static StelTextureSP texRegion;				// The symbolic dashed shape texture
	static StelTextureSP texPointElement;
	static StelTextureSP texPlanetaryNebula;		// Type 3
	static float hintsBrightness;
	static float labelsBrightness;

	static Vec3f labelColor;				// The color of labels
	static QMap<Nebula::NebulaType, Vec3f>hintColorMap;	// map for rapid lookup. Updated by NebulaMgr whenever a color changes.
	static const QMap<Nebula::NebulaType, QString> typeEnglishStringMap; // map that keeps type strings for NebulaType
	static QMap<Nebula::NebulaType, QString>typeI18nNebulaStringMap;     // map that keeps translated type strings for NebulaType

	static bool drawHintProportional;     // scale hint with nebula size?
	static bool surfaceBrightnessUsage;
	static bool designationUsage;

	static bool flagUseTypeFilters;
	static CatalogGroup catalogFilters;
	static TypeGroup typeFilters;

	static bool flagUseArcsecSurfaceBrightness;
	static bool flagUseShortNotationSurfaceBrightness;
	static bool flagUseOutlines;
	static bool flagShowAdditionalNames;
	static bool flagShowOnlyNamedDSO;

	static bool flagUseSizeLimits;
	static double minSizeLimit;
	static double maxSizeLimit;

	OptionalList<QList<std::vector<Vec3d> *>> outlineSegments;
};

Q_DECLARE_OPERATORS_FOR_FLAGS(Nebula::CatalogGroup)
Q_DECLARE_OPERATORS_FOR_FLAGS(Nebula::TypeGroup)

#endif // NEBULA_HPP

