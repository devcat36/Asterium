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

#ifndef OPTIONALLIST_HPP
#define OPTIONALLIST_HPP

#include <QtGlobal>

//! A list slot that costs a pointer while it is empty, which is what these are
//! on almost every catalogue object. Behaves like the list it wraps.
template<typename List>
class OptionalList
{
public:
	using value_type = typename List::value_type;

	OptionalList() = default;
	OptionalList(const OptionalList& other) { if (other.list) list = new List(*other.list); }
	OptionalList(OptionalList&& other) noexcept : list(other.list) { other.list = nullptr; }
	~OptionalList() { delete list; }

	OptionalList& operator=(const OptionalList& other)
	{
		if (this != &other)
			assign(other.value());
		return *this;
	}

	OptionalList& operator=(OptionalList&& other) noexcept
	{
		if (this != &other)
		{
			delete list;
			list = other.list;
			other.list = nullptr;
		}
		return *this;
	}

	OptionalList& operator=(const List& v) { assign(v); return *this; }

	operator const List&() const { return value(); }
	const List& value() const { return list ? *list : none(); }
	List& grown() { if (!list) list = new List; return *list; }

	bool isEmpty() const { return !list || list->isEmpty(); }
	bool empty() const { return isEmpty(); }
	qsizetype size() const { return list ? list->size() : 0; }
	qsizetype count() const { return size(); }
	qsizetype length() const { return size(); }
	void clear() { delete list; list = nullptr; }

	void append(const value_type& v) { grown().append(v); }
	void append(const List& v) { grown().append(v); }
	OptionalList& operator<<(const value_type& v) { grown() << v; return *this; }
	OptionalList& operator<<(const List& v) { grown() << v; return *this; }

	const value_type& at(qsizetype i) const { return value().at(i); }
	const value_type& operator[](qsizetype i) const { return value().at(i); }
	void push_back(const value_type& v) { grown().push_back(v); }
	const value_type& first() const { return value().first(); }
	const value_type& constFirst() const { return value().constFirst(); }
	bool contains(const value_type& v) const { return list && list->contains(v); }

	auto begin() { return grown().begin(); }
	auto end() { return grown().end(); }
	auto begin() const { return value().begin(); }
	auto end() const { return value().end(); }
	auto cbegin() const { return value().cbegin(); }
	auto cend() const { return value().cend(); }
	auto constBegin() const { return value().constBegin(); }
	auto constEnd() const { return value().constEnd(); }

	template<typename Sep>
	auto join(const Sep& sep) const { return value().join(sep); }

	void removeDuplicates() { if (list) list->removeDuplicates(); }

private:
	void assign(const List& v)
	{
		if (v.isEmpty())
		{
			delete list;
			list = nullptr;
		}
		else if (list)
			*list = v;
		else
			list = new List(v);
	}

	static const List& none()
	{
		static const List empty;
		return empty;
	}

	List* list = nullptr;
};

#endif // OPTIONALLIST_HPP
