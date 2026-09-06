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

#ifndef ASTERIUM_ASTROCALCPAGES_HPP
#define ASTERIUM_ASTROCALCPAGES_HPP

#include <QJsonArray>
#include <QJsonObject>
#include <QPair>
#include <QString>

#include "StelObjectType.hpp"
#include "VecMath.hpp"

class StelCore;
class SolarSystem;

namespace AsteriumAstroCalc
{
bool answer(const QString& verb, const QString& arg, QJsonObject& out);

bool perform(const QString& verb, const QString& arg);

extern const QString kDash;

QPair<QString, QString> coordinates(const Vec3d& coord, bool horizontal, bool southAzimuth,
                                    bool decimalDegrees, bool polarDistance);

double culminationElevation(const StelObjectP& obj, StelCore* core);

void transitStrings(const StelObjectP& obj, StelCore* core, double utcShift, bool decimalDegrees,
                    QString& transit, QString& elevation);

QString angleString(double radians, bool decimalDegrees);

QJsonArray bodies(SolarSystem* solarSystem, StelCore* core);

QString setting(const char* key, const QString& fallback);
bool settingBool(const char* key, bool fallback);
double settingDouble(const char* key, double fallback);
int settingInt(const char* key, int fallback);
bool writeSetting(const QString& key, const QString& value);

double parseLocalTime(const QString& text, StelCore* core);
}

#endif
