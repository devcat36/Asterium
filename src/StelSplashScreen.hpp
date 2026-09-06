/*
 * Copyright (C) 2019 Ruslan Kabatsayev
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

#ifndef STELLARIUM_SPLASH_SCREEN_HPP
#define STELLARIUM_SPLASH_SCREEN_HPP

#include <memory>
#include <QThread>
#include <QApplication>
#include <QSplashScreen>

class SplashTextHolder;
struct SplashScreenTextHorizMetrics
{
	int shift;
	int width;
};
class SplashScreen
{
	class SplashScreenWidget : public QSplashScreen
	{
		QPixmap makePixmap(double sizeRatio, const QSize& target);
		int textPixelSize(double sizeRatio) const;
	public:
		SplashScreenWidget(double sizeRatio);
		void setStatusMessage(const QString& m) { statusMessage = m; }
		void ensureFirstPaint() const
		{
			while(!painted)
			{
				QThread::msleep(1);
				qApp->processEvents();
			}
		}
		bool hasPainted() const { return painted; }

	protected:
		void paintEvent(QPaintEvent*) override;
#if defined(Q_OS_ANDROID)
		void mousePressEvent(QMouseEvent*) override {}
#endif
		void resizeEvent(QResizeEvent* event) override;
		void drawContents(QPainter* painter) override;

	private:
		std::unique_ptr<SplashTextHolder> textHolder;
		QFont splashFont;
		QFont titleFont;
		QFont subtitleFont;
		QFont versionFont;
		double sizeRatio = 1;
		QRect artRect;
		QString statusMessage;
		bool painted=false;
		SplashScreenTextHorizMetrics titleHM;
		SplashScreenTextHorizMetrics subtitleHM;
		SplashScreenTextHorizMetrics versionHM;
	};

	static SplashScreenWidget* instance;

public:
	static void present(double sizeRatio);
	static void finish(QWidget* mainWindow);
	static void showMessage(QString const& message);
	static void clearMessage();
};

#endif
