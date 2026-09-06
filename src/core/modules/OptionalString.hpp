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

#ifndef OPTIONALSTRING_HPP
#define OPTIONALSTRING_HPP

#include <QDataStream>
#include <QString>

//! A string slot that costs a pointer when it is empty, which most of the
//! catalogue identifier fields on most objects are. Behaves like a const
//! QString everywhere it is read.
class OptionalString
{
public:
	OptionalString() = default;
	OptionalString(const OptionalString& other) { if (other.text) text = new QString(*other.text); }
	OptionalString(OptionalString&& other) noexcept : text(other.text) { other.text = nullptr; }
	~OptionalString() { delete text; }

	OptionalString& operator=(const OptionalString& other)
	{
		if (this != &other)
			assign(other.value());
		return *this;
	}

	OptionalString& operator=(OptionalString&& other) noexcept
	{
		if (this != &other)
		{
			delete text;
			text = other.text;
			other.text = nullptr;
		}
		return *this;
	}

	OptionalString& operator=(const QString& v) { assign(v); return *this; }

	operator const QString&() const { return value(); }
	const QString& value() const { return text ? *text : none(); }

	bool isEmpty() const { return !text || text->isEmpty(); }
	void clear() { delete text; text = nullptr; }
	QString trimmed() const { return value().trimmed(); }
	QString toUpper() const { return value().toUpper(); }
	int compare(const QString& other, Qt::CaseSensitivity cs = Qt::CaseSensitive) const
	{
		return value().compare(other, cs);
	}

private:
	void assign(const QString& v)
	{
		if (v.isEmpty())
		{
			delete text;
			text = nullptr;
		}
		else if (text)
			*text = v;
		else
			text = new QString(v);
	}

	static const QString& none()
	{
		static const QString empty;
		return empty;
	}

	QString* text = nullptr;
};

inline QDataStream& operator>>(QDataStream& in, OptionalString& s)
{
	QString v;
	in >> v;
	s = v;
	return in;
}

#endif // OPTIONALSTRING_HPP
