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

#ifndef ASTERIUM_ASTROCALCPAGES_P_HPP
#define ASTERIUM_ASTROCALCPAGES_P_HPP

#include <QJsonObject>
#include <QString>
#include <QStringList>

#include "StelObjectType.hpp"

class StelMovementMgr;

void asteriumMoveToSelected(StelMovementMgr* movement, const StelObjectP& object, float duration);

QStringList asteriumObsKeys(const StelObjectP& object);

bool asteriumObsHoldsAny(const QStringList& keys);

namespace AsteriumAstroCalc
{
namespace Rts
{
	bool answer(const QString& verb, const QString& arg, QJsonObject& out);
	bool perform(const QString& verb, const QString& arg);
}

namespace Phenomena
{
	bool answer(const QString& verb, const QString& arg, QJsonObject& out);
	bool perform(const QString& verb, const QString& arg);
}

namespace Graphs
{
	bool answer(const QString& verb, const QString& arg, QJsonObject& out);
	bool perform(const QString& verb, const QString& arg);
}

namespace Wut
{
	bool answer(const QString& verb, const QString& arg, QJsonObject& out);
	bool perform(const QString& verb, const QString& arg);
}

namespace Pc
{
	bool answer(const QString& verb, const QString& arg, QJsonObject& out);
	bool perform(const QString& verb, const QString& arg);
}

namespace Eclipses
{
	bool answer(const QString& verb, const QString& arg, QJsonObject& out);
	bool perform(const QString& verb, const QString& arg);
}

namespace Almanac
{
	bool answer(const QString& verb, const QString& arg, QJsonObject& out);
	bool perform(const QString& verb, const QString& arg);
}
}

#endif
