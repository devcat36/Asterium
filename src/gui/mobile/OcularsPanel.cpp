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

#include "OcularsPanel.hpp"
#include "AndroidUi.hpp"

#include "StelApp.hpp"
#include "StelCore.hpp"
#include "StelModule.hpp"
#include "StelModuleMgr.hpp"
#include "StelPropertyMgr.hpp"
#include "StelTranslator.hpp"
#include "StelSkyDrawer.hpp"

#include <QtMath>

#include <cmath>

#include <QJsonArray>
#include <QJsonDocument>
#include <QMetaObject>
#include <QSettings>
#include <QVariantMap>
#include <QString>
#include <QStringList>
#include <QVariant>

namespace
{
const char* const kOculars = "Oculars";
const QString kDash = QStringLiteral("—");

StelModule* ocularsModule()
{
	return StelApp::getInstance().getModuleMgr().getModule(kOculars, true);
}

QSettings* equipmentFile()
{
	StelModule* module = ocularsModule();
	return module ? module->getSettings() : nullptr;
}

QVariant ocularsProperty(const char* name)
{
	StelPropertyMgr* properties = StelApp::getInstance().getStelPropertyManager();
	const QString id = QStringLiteral("Oculars.") + QString::fromLatin1(name);
	if (!properties->getProperty(id, true))
		return QVariant();
	return properties->getStelPropertyValue(id, true);
}

int selectedIndex(const char* name)
{
	const QVariant value = ocularsProperty(name);
	return value.isValid() ? value.toInt() : -1;
}

int entryCount(QSettings* equipment, const char* countKey)
{
	return equipment ? equipment->value(QString::fromLatin1(countKey), 0).toInt() : 0;
}

QString entryKey(const char* group, int index, const char* field)
{
	return QString::fromLatin1(group) + QLatin1Char('/') + QString::number(index)
	       + QLatin1Char('/') + QString::fromLatin1(field);
}

QString entryName(QSettings* equipment, const char* group, int index, const QString& noun)
{
	if (!equipment || index < 0)
		return noun;
	const QString name = equipment->value(entryKey(group, index, "name")).toString();
	return name.isEmpty() ? QString("%1 #%2").arg(noun).arg(index) : name;
}

double entryValue(QSettings* equipment, const char* group, int index, const char* key,
                  double fallback)
{
	if (!equipment || index < 0)
		return fallback;
	return equipment->value(entryKey(group, index, key), fallback).toDouble();
}

QString detailOf(QSettings* equipment, const char* group, int index)
{
	const QLatin1String kind(group);
	QStringList parts;
	if (kind == QLatin1String("ocular"))
	{
		if (equipment->value(entryKey(group, index, "binoculars"), false).toBool())
			parts << ct_("Binocular");
		parts << QString("%1 mm").arg(entryValue(equipment, group, index, "efl", 0.), 0, 'f', 1);
		parts << QString("%1° %2").arg(entryValue(equipment, group, index, "afov", 0.), 0, 'f', 0)
		                          .arg(ct_("aFOV"));
	}
	else if (kind == QLatin1String("telescope"))
	{
		const double focal = entryValue(equipment, group, index, "focalLength", 0.);
		const double aperture = entryValue(equipment, group, index, "diameter", 0.);
		parts << QString("%1 mm").arg(focal, 0, 'f', 0);
		parts << QString("⌀ %1 mm").arg(aperture, 0, 'f', 0);
		if (aperture > 0.)
			parts << QString("f/%1").arg(focal / aperture, 0, 'f', 1);
	}
	else if (kind == QLatin1String("lens"))
	{
		parts << QString("×%1").arg(entryValue(equipment, group, index, "multipler", 1.), 0, 'f', 2);
	}
	else
	{
		parts << QString("%1 × %2")
		         .arg(entryValue(equipment, group, index, "resolutionX", 0.), 0, 'f', 0)
		         .arg(entryValue(equipment, group, index, "resolutionY", 0.), 0, 'f', 0);
		parts << QString("%1 × %2 mm")
		         .arg(entryValue(equipment, group, index, "chip_width", 0.), 0, 'f', 1)
		         .arg(entryValue(equipment, group, index, "chip_height", 0.), 0, 'f', 1);
	}
	return parts.join(QStringLiteral(" · "));
}

QString groupKey(const char* group, int index)
{
	return QString::fromLatin1(group) + QLatin1Char('/') + QString::number(index);
}

QJsonObject fieldsOf(QSettings* equipment, const char* group, int index)
{
	QJsonObject fields;
	equipment->beginGroup(groupKey(group, index));
	const QStringList keys = equipment->allKeys();
	for (const QString& key : keys)
		fields.insert(key, equipment->value(key).toString());
	equipment->endGroup();
	return fields;
}

QJsonArray entryList(QSettings* equipment, const char* group, const char* countKey,
                     const QString& noun)
{
	QJsonArray items;
	const int count = entryCount(equipment, countKey);
	for (int index = 0; index < count; ++index)
	{
		QJsonObject item;
		item["name"] = entryName(equipment, group, index, noun);
		item["detail"] = detailOf(equipment, group, index);
		item["fields"] = fieldsOf(equipment, group, index);
		items.append(item);
	}
	return items;
}

const struct { const char* group; const char* countKey; int floor; } kGroups[] = {
	{ "ocular",    "ocular_count",    1 },
	{ "telescope", "telescope_count", 1 },
	{ "ccd",       "ccd_count",       1 },
	{ "lens",      "lens_count",      0 },
};

int groupIndex(const QString& group)
{
	for (int i = 0; i < 4; ++i)
		if (group == QLatin1String(kGroups[i].group))
			return i;
	return -1;
}

void moveEntry(QSettings* equipment, const char* group, int from, int to)
{
	const QString source = groupKey(group, from);
	const QString target = groupKey(group, to);
	equipment->beginGroup(source);
	const QStringList keys = equipment->allKeys();
	QVariantMap held;
	for (const QString& key : keys)
		held.insert(key, equipment->value(key));
	equipment->endGroup();
	equipment->remove(target);
	for (auto it = held.constBegin(); it != held.constEnd(); ++it)
		equipment->setValue(target + QLatin1Char('/') + it.key(), it.value());
}

void reloadEquipment()
{
	if (StelModule* module = ocularsModule())
		QMetaObject::invokeMethod(module, "asteriumReloadEquipment", Qt::DirectConnection);
}

double limitingMagnitude(bool binocular, double aperture, double eyepieceFocal, double fieldStop)
{
	if (!binocular)
		return 4.5 + 4.4 * std::log10(aperture > 0. ? aperture : 0.1);
	StelSkyDrawer* drawer = StelApp::getInstance().getCore()->getSkyDrawer();
	const double nakedEye = drawer
			? StelCore::luminanceToNELM(drawer->getLightPollutionLuminance()) : 6.;
	return 3. * std::log10(fieldStop > 0. ? fieldStop / 10. : 0.1)
	       + 2. * std::log10(eyepieceFocal > 0. ? eyepieceFocal : 0.1) + 0.6 + nakedEye;
}

double chipAngle(double millimetres, double focalLength)
{
	return 2. * std::atan(millimetres / (2. * focalLength)) * 180. / M_PI;
}

QString angleText(double degrees)
{
	if (degrees >= 1.)
	{
		const int whole = static_cast<int>(degrees);
		return QString("%1°%2′").arg(whole).arg((degrees - whole) * 60., 0, 'f', 1);
	}
	return QString("%1′").arg(degrees * 60., 0, 'f', 1);
}
}

void AsteriumOculars::install()
{
	StelModuleMgr& modules = StelApp::getInstance().getModuleMgr();
	if (!modules.getModule(kOculars, true))
	{
		StelModule* loaded = modules.loadPlugin(kOculars);
		if (!loaded)
			return;
		modules.registerModule(loaded, true);
		modules.loadExtensions(kOculars);
		loaded->init();
		modules.setPluginLoadAtStartup(kOculars, true);
	}
	StelPropertyMgr* properties = StelApp::getInstance().getStelPropertyManager();
	properties->setStelPropertyValue("Oculars.flagGuiPanelEnabled", QVariant(false), true);

	static const struct { const char* property; const char* key; int floor; } kSelections[] = {
		{ "Oculars.selectedOcularIndex",    "ocular_index",    0  },
		{ "Oculars.selectedTelescopeIndex", "telescope_index", 0  },
		{ "Oculars.selectedCCDIndex",       "ccd_index",       0  },
		{ "Oculars.selectedLensIndex",      "lens_index",      -1 },
	};
	for (const auto& selection : kSelections)
	{
		StelProperty* property = properties->getProperty(QLatin1String(selection.property), true);
		if (!property)
			continue;
		QObject::connect(property, &StelProperty::changed, property, [selection](const QVariant& value)
		{
			QSettings* equipment = equipmentFile();
			if (!equipment)
				return;
			equipment->setValue(QLatin1String(selection.key), qMax(selection.floor, value.toInt()));
			equipment->sync();
		});
	}
}

void AsteriumOculars::addSnapshot(QJsonObject& out)
{
	if (!ocularsModule())
		return;

	QJsonObject panel;
	panel["on"] = ocularsProperty("enableOcular").toBool();
	panel["sensor"] = ocularsProperty("enableCCD").toBool();
	panel["telrad"] = ocularsProperty("enableTelrad").toBool();
	panel["crosshairs"] = ocularsProperty("enableCrosshairs").toBool();

	QSettings* equipment = equipmentFile();
	const int eyepiece = selectedIndex("selectedOcularIndex");
	const int telescope = selectedIndex("selectedTelescopeIndex");
	const int lens = selectedIndex("selectedLensIndex");

	const bool binocular = equipment && eyepiece >= 0
			&& equipment->value(entryKey("ocular", eyepiece, "binoculars"), false).toBool();
	const double apparentField = entryValue(equipment, "ocular", eyepiece, "afov", 0.);
	const double eyepieceFocal = entryValue(equipment, "ocular", eyepiece, "efl", 0.);
	const double fieldStop = entryValue(equipment, "ocular", eyepiece, "fieldStop", 0.);
	const double aperture = entryValue(equipment, "telescope", telescope, "diameter", 0.);
	const double telescopeFocal = entryValue(equipment, "telescope", telescope, "focalLength", 0.);
	const double multiplier = lens < 0 ? 1. : entryValue(equipment, "lens", lens, "multipler", 1.);
	const double drawn = telescopeFocal * multiplier;

	double magnification = 0.;
	if (binocular)
		magnification = eyepieceFocal;
	else if (eyepieceFocal > 0. && drawn > 0.)
		magnification = drawn / eyepieceFocal;

	double trueField = 0.;
	if (binocular)
		trueField = apparentField;
	else if (fieldStop > 0. && drawn > 0.)
		trueField = fieldStop / drawn * 57.3;
	else if (magnification > 0.)
		trueField = apparentField / magnification;

	panel["binocular"] = binocular;
	panel["eyepiece"] = entryName(equipment, "ocular", eyepiece, ct_("Eyepiece"));

	const QString ocularNoun = binocular ? ct_("Binocular") : ct_("Ocular");
	const QString eyepieceTitle = entryName(equipment, "ocular", eyepiece, ocularNoun);
	const QString telescopeTitle = entryName(equipment, "telescope", telescope, ct_("Telescope"));
	const QString magnificationFull = magnification > 0.
			? (aperture > 0. ? QString("%1× (%2D)").arg(magnification, 0, 'f', 1)
			                           .arg(magnification / aperture, 0, 'f', 2)
			                 : QString("%1×").arg(magnification, 0, 'f', 1))
			: kDash;
	const QString limitMagnitude = QString::number(
			limitingMagnitude(binocular, aperture, eyepieceFocal, fieldStop), 'f', 2);

	const int chip = selectedIndex("selectedCCDIndex");
	const QString sensorName = entryName(equipment, "ccd", chip, ct_("Sensor"));
	panel["sensorName"] = sensorName;
	const double chipWidth = entryValue(equipment, "ccd", chip, "chip_width", 0.);
	const double chipHeight = entryValue(equipment, "ccd", chip, "chip_height", 0.);
	const QString sensorField = chipWidth > 0. && chipHeight > 0. && drawn > 0.
			? QString("%1 × %2").arg(angleText(chipAngle(chipWidth, drawn)),
			                         angleText(chipAngle(chipHeight, drawn)))
			: kDash;
	const QString sensorBinning = QString("%1 × %2")
			.arg(entryValue(equipment, "ccd", chip, "binningX", 1.), 0, 'f', 0)
			.arg(entryValue(equipment, "ccd", chip, "binningY", 1.), 0, 'f', 0);

	const bool imaging = panel["sensor"].toBool();
	QJsonObject card;
	card["eyepieceTitle"] = eyepiece < 0 ? ocularNoun
			: QString("%1 #%2: %3").arg(ocularNoun).arg(eyepiece).arg(eyepieceTitle);
	QJsonArray eyepieceLines;
	if (!binocular)
	{
		eyepieceLines.append(QString(ct_("Ocular FL: %1 mm"))
				.arg(QString::number(eyepieceFocal, 'f', 1)));
		eyepieceLines.append(QString(ct_("Ocular aFOV: %1"))
				.arg(QString::number(apparentField, 'f', 2) + QChar(0x00B0)));
	}
	card["eyepieceLines"] = eyepieceLines;

	card["telescopeTitle"] = telescope < 0 ? ct_("Telescope")
			: QString(ct_("Telescope #%1: %2")).arg(telescope).arg(telescopeTitle);
	QJsonArray telescopeLines;
	if (!imaging && !binocular && magnification > 0.)
	{
		telescopeLines.append(QString(ct_("Magnification: %1"))
				.arg(magnificationFull));
		if (aperture > 0.)
			telescopeLines.append(QString(ct_("Exit pupil: %1 mm"))
					.arg(QString::number(aperture / magnification, 'f', 2)));
		telescopeLines.append(QString(ct_("FOV: %1")).arg(angleText(trueField)));
		telescopeLines.append(StelTranslator::globalTranslator->qtranslate(
					"Limiting magnitude", "Limiting magnitude of device")
				+ QStringLiteral(": ") + limitMagnitude);
	}
	card["telescopeLines"] = telescopeLines;

	card["sensorTitle"] = chip < 0 ? ct_("Sensor")
			: QString(ct_("Sensor #%1: %2")).arg(chip).arg(sensorName);
	QJsonArray sensorLines;
	sensorLines.append(QString(ct_("Dimensions: %1")).arg(sensorField));
	sensorLines.append(ct_("Binning") + QStringLiteral(": ") + sensorBinning);
	card["sensorLines"] = sensorLines;

	card["lensTitle"] = lens < 0 ? ct_("Lens: none")
			: QString(ct_("Lens #%1: %2")).arg(lens)
					.arg(entryName(equipment, "lens", lens, ct_("Lens")));
	QJsonArray lensLines;
	if (lens >= 0)
		lensLines.append(QString(ct_("Multiplicity: %1"))
				.arg(QString("%1×").arg(multiplier, 0, 'f', 2)));
	card["lensLines"] = lensLines;
	panel["card"] = card;

	out["oculars"] = panel;
}

bool AsteriumOculars::answer(const QString& verb, const QString& arg, QJsonObject& out)
{
	if (verb == QLatin1String("oculars.about"))
	{
		Q_UNUSED(arg)
		const QList<StelModuleMgr::PluginDescriptor> plugins =
				StelApp::getInstance().getModuleMgr().getPluginsList();
		for (const StelModuleMgr::PluginDescriptor& plugin : plugins)
		{
			if (plugin.info.id != QLatin1String(kOculars))
				continue;
			out["name"] = plugin.info.displayedName;
			out["version"] = plugin.info.version;
			out["license"] = plugin.info.license;
			out["author"] = plugin.info.authors;
			out["contact"] = plugin.info.contact;
		}
		return true;
	}
	if (verb != QLatin1String("oculars"))
		return false;
	Q_UNUSED(arg)

	QSettings* equipment = equipmentFile();
	out["loaded"] = ocularsModule() != nullptr;
	out["eyepieces"] = entryList(equipment, "ocular", "ocular_count", ct_("Eyepiece"));
	out["telescopes"] = entryList(equipment, "telescope", "telescope_count", ct_("Telescope"));
	out["lenses"] = entryList(equipment, "lens", "lens_count", ct_("Lens"));
	out["sensors"] = entryList(equipment, "ccd", "ccd_count", ct_("Sensor"));
	out["eyepiece"] = selectedIndex("selectedOcularIndex");
	out["telescope"] = selectedIndex("selectedTelescopeIndex");
	out["lens"] = selectedIndex("selectedLensIndex");
	out["sensor"] = selectedIndex("selectedCCDIndex");
	return true;
}

bool AsteriumOculars::perform(const QString& verb, const QString& arg)
{
	const bool put = verb == QLatin1String("oculars.put");
	if (!put && verb != QLatin1String("oculars.remove"))
		return false;

	QSettings* equipment = equipmentFile();
	if (!equipment)
		return true;

	const QJsonObject request = QJsonDocument::fromJson(arg.toUtf8()).object();
	const int which = groupIndex(request.value("group").toString());
	if (which < 0)
		return true;
	const char* const group = kGroups[which].group;
	const char* const countKey = kGroups[which].countKey;
	const int count = entryCount(equipment, countKey);
	int index = request.value("index").toInt(-1);

	if (put)
	{
		if (index < 0 || index >= count)
		{
			index = count;
			equipment->setValue(QString::fromLatin1(countKey), count + 1);
		}
		const QJsonObject fields = request.value("fields").toObject();
		for (auto it = fields.constBegin(); it != fields.constEnd(); ++it)
			equipment->setValue(groupKey(group, index) + QLatin1Char('/') + it.key(),
			                    it.value().toString());
	}
	else
	{
		if (index < 0 || index >= count || count <= kGroups[which].floor)
			return true;
		for (int at = index; at < count - 1; ++at)
			moveEntry(equipment, group, at + 1, at);
		equipment->remove(groupKey(group, count - 1));
		equipment->setValue(QString::fromLatin1(countKey), count - 1);
	}

	equipment->sync();
	reloadEquipment();
	return true;
}
