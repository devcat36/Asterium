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

package org.asterium.asterium;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

final class SkyPointer implements SensorEventListener
{
	interface AimListener
	{
		void onAim(double[] world);
	}

	private final Context context;
	private final SensorManager sensors;
	private final Sensor rotation;
	private AimListener listener;

	private final float[] quaternion = new float[4];
	private final float[] deviceMatrix = new float[9];
	private final float[] screenMatrix = new float[9];
	private final double[] aim = new double[6];
	private final double[] world = new double[6];
	private boolean settled = false;
	private boolean on = false;

	private double declination = 0.;
	private double lastLat = Double.NaN;
	private double lastLon = Double.NaN;

	private static final double FOLLOW = 0.25;

	SkyPointer(Context context)
	{
		this.context = context;
		sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		rotation = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
	}

	boolean available()
	{
		return rotation != null;
	}

	boolean isOn()
	{
		return on;
	}

	void setEnabled(boolean value)
	{
		if (value == on || rotation == null)
			return;
		on = value;
		NativeBridge.send("view.hold", value ? "1" : "0");
		if (value)
		{
			settled = false;
			sensors.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME);
		}
		else
		{
			sensors.unregisterListener(this);
		}
	}

	void setObserver(double latitude, double longitude)
	{
		if (Double.isNaN(latitude) || Double.isNaN(longitude))
			return;
		if (latitude == lastLat && longitude == lastLon)
			return;
		lastLat = latitude;
		lastLon = longitude;
		declination = new GeomagneticField((float) latitude, (float) longitude, 0f,
				System.currentTimeMillis()).getDeclination() * Math.PI / 180.;
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (!on)
			return;
		final int used = Math.min(event.values.length, 4);
		System.arraycopy(event.values, 0, quaternion, 0, used);
		if (used < 4)
		{
			final float squared = quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1]
					+ quaternion[2] * quaternion[2];
			quaternion[3] = squared < 1f ? (float) Math.sqrt(1f - squared) : 0f;
		}
		SensorManager.getRotationMatrixFromVector(deviceMatrix, quaternion);

		final float[] m;
		switch (displayRotation())
		{
			case Surface.ROTATION_90:
				m = remap(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X);
				break;
			case Surface.ROTATION_180:
				m = remap(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y);
				break;
			case Surface.ROTATION_270:
				m = remap(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X);
				break;
			default:
				m = deviceMatrix;
				break;
		}

		follow(0, -m[2], -m[5], -m[8]);
		follow(3, m[1], m[4], m[7]);

		final double cos = Math.cos(declination), sin = Math.sin(declination);
		trueNorth(0, cos, sin);
		trueNorth(3, cos, sin);
		NativeBridge.send("view.point",
				world[0] + "," + world[1] + "," + world[2] + ","
				+ world[3] + "," + world[4] + "," + world[5]);
		settled = true;
		if (listener != null)
			listener.onAim(world);
	}

	void setAimListener(AimListener value)
	{
		listener = value;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}

	private float[] remap(int screenX, int screenY)
	{
		return SensorManager.remapCoordinateSystem(deviceMatrix, screenX, screenY, screenMatrix)
				? screenMatrix : deviceMatrix;
	}

	private int displayRotation()
	{
		final Display display;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			display = context.getDisplay();
		else
			display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
					.getDefaultDisplay();
		return display == null ? Surface.ROTATION_0 : display.getRotation();
	}

	private void follow(int at, double east, double north, double up)
	{
		final double a = settled ? FOLLOW : 1.;
		aim[at] += a * (east - aim[at]);
		aim[at + 1] += a * (north - aim[at + 1]);
		aim[at + 2] += a * (up - aim[at + 2]);
	}

	private void trueNorth(int at, double cos, double sin)
	{
		final double east = aim[at], north = aim[at + 1];
		world[at] = east * cos + north * sin;
		world[at + 1] = north * cos - east * sin;
		world[at + 2] = aim[at + 2];
	}
}
