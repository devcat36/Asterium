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

#ifndef ASTERIUM_ANDROIDUI_HPP
#define ASTERIUM_ANDROIDUI_HPP

#include <QObject>
#include <QString>
#include <QElapsedTimer>

QString asteriumFormatSimTime(double jd, const QString& format);

QString asteriumTranslate(const QString& text);

#define ct_(str) asteriumTranslate(str)

QString asteriumShortLabel(const QString& text);

class AndroidUi : public QObject
{
	Q_OBJECT
	Q_PROPERTY(bool obsListHighlight READ obsListHighlight WRITE setObsListHighlight
	           NOTIFY obsListHighlightChanged)
	Q_PROPERTY(bool flagShowFps READ flagShowFps WRITE setFlagShowFps
	           NOTIFY flagShowFpsChanged)

public:
	static void install();

	bool obsListHighlight() const;
	bool flagShowFps() const;

public slots:
	void setObsListHighlight(bool on);
	void setFlagShowFps(bool on);

signals:
	void obsListHighlightChanged(bool on);
	void flagShowFpsChanged(bool on);

public:

	static void update();

	static bool ready();

private:
	explicit AndroidUi(QObject* parent = nullptr);

	Q_INVOKABLE void dispatch(int token, const QString& verb, const QString& arg);

	QString snapshot() const;

	QString answer(const QString& verb, const QString& arg) const;

	void perform(const QString& verb, const QString& arg);

	void lookupSimbad(int token, const QString& arg, bool byCoordinates);

	class SimbadSearcher* simbad = nullptr;

	static void callJava(const char* method, const char* signature, int token, const QString& payload);

	QString lastSnapshot;
	QElapsedTimer sinceSnapshot;

	bool announced = false;

	bool fpsShown = false;
};

#endif
