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
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;

import org.qtproject.qt.android.bindings.QtActivity;

import java.util.Locale;

public class AsteriumActivity extends QtActivity
{
	private Overlay overlay;
	private boolean fullScreen = true;

	static void restart(Context context)
	{
		final Intent intent = context.getPackageManager()
				.getLaunchIntentForPackage(context.getPackageName());
		if (intent == null)
			return;
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		context.startActivity(intent);
		Runtime.getRuntime().exit(0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Theme.init(this);
		T.init(this);

		getWindow().setStatusBarColor(Color.TRANSPARENT);
		getWindow().setNavigationBarColor(Color.TRANSPARENT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		applySystemBars();

		overlay = new Overlay(this);
		addContentView(overlay, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		overlay.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
				Gravity.FILL));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
					OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);
	}

	private void back()
	{
		if (overlay == null || !overlay.handleBack())
			moveTaskToBack(true);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		savedInstanceState.putBoolean("Started", false);
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event)
	{
		if (overlay != null)
			overlay.onTouchObserved(event);
		return super.dispatchTouchEvent(event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
		{
			if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled())
				back();
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	void askForLocation(Runnable answered)
	{
		if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
				== android.content.pm.PackageManager.PERMISSION_GRANTED)
		{
			answered.run();
			return;
		}
		locationAnswer = answered;
		requestPermissions(new String[] {
				android.Manifest.permission.ACCESS_FINE_LOCATION,
				android.Manifest.permission.ACCESS_COARSE_LOCATION }, LOCATION_REQUEST);
	}

	enum LocationTrouble { NO_PERMISSION, NO_SERVICE, PROVIDERS_OFF, NO_FIX }

	interface LocationSink
	{
		void onFix(Location fix, boolean fresh);
		void onTrouble(LocationTrouble trouble);
	}

	static Location lastKnownLocation(Context context)
	{
		if (context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
				!= android.content.pm.PackageManager.PERMISSION_GRANTED)
			return null;
		final LocationManager manager =
				(LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (manager == null)
			return null;
		Location best = null;
		try
		{
			for (String provider : manager.getProviders(true))
			{
				final Location known = manager.getLastKnownLocation(provider);
				if (known != null && (best == null || known.getTime() > best.getTime()))
					best = known;
			}
		}
		catch (SecurityException revoked)
		{
			return null;
		}
		return best;
	}

	void findLocation(final LocationSink sink)
	{
		askForLocation(() ->
		{
			if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
					!= android.content.pm.PackageManager.PERMISSION_GRANTED)
			{
				sink.onTrouble(LocationTrouble.NO_PERMISSION);
				return;
			}
			requestFix(sink);
		});
	}

	private void requestFix(final LocationSink sink)
	{
		final LocationManager manager =
				(LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (manager == null)
		{
			sink.onTrouble(LocationTrouble.NO_SERVICE);
			return;
		}
		stopLocating();
		final Location known = lastKnownLocation(this);
		if (known != null)
			sink.onFix(known, false);

		try
		{
			fixListener = new LocationListener()
			{
				@Override public void onLocationChanged(Location fix)
				{
					stopLocating();
					sink.onFix(fix, true);
				}
				@Override public void onProviderEnabled(String provider) {}
				@Override public void onProviderDisabled(String provider) {}
				@Override public void onStatusChanged(String provider, int s, Bundle extras) {}
			};
			boolean listening = false;
			for (String provider : new String[] {
					LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER })
			{
				if (!manager.isProviderEnabled(provider))
					continue;
				manager.requestLocationUpdates(provider, 0L, 0f, fixListener,
						Looper.getMainLooper());
				listening = true;
			}
			if (!listening)
			{
				stopLocating();
				if (known == null)
					sink.onTrouble(LocationTrouble.PROVIDERS_OFF);
				return;
			}
			fixTimeout.postDelayed(() ->
			{
				if (fixListener == null)
					return;
				stopLocating();
				sink.onTrouble(LocationTrouble.NO_FIX);
			}, 30000);
		}
		catch (SecurityException revoked)
		{
			sink.onTrouble(LocationTrouble.NO_PERMISSION);
		}
	}

	void stopLocating()
	{
		fixTimeout.removeCallbacksAndMessages(null);
		if (fixListener == null)
			return;
		final LocationManager manager =
				(LocationManager) getSystemService(Context.LOCATION_SERVICE);
		try
		{
			if (manager != null)
				manager.removeUpdates(fixListener);
		}
		catch (SecurityException ignored)
		{
		}
		fixListener = null;
	}

	void locateOnStart()
	{
		if (locationApplied)
			return;
		locationApplied = true;
		findLocation(new LocationSink()
		{
			@Override public void onFix(Location fix, boolean fresh)
			{
				NativeBridge.send("loc.set", String.format(Locale.US, "%.5f,%.5f,%s",
						fix.getLatitude(), fix.getLongitude(),
						fix.hasAltitude() ? String.valueOf(Math.round(fix.getAltitude())) : ""));
			}

			@Override public void onTrouble(LocationTrouble trouble) {}
		});
	}

	private static final int LOCATION_REQUEST = 4711;
	private Runnable locationAnswer;
	private boolean locationApplied;
	private LocationListener fixListener;
	private final Handler fixTimeout = new Handler(Looper.getMainLooper());

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results)
	{
		super.onRequestPermissionsResult(requestCode, permissions, results);
		if (requestCode != LOCATION_REQUEST || locationAnswer == null)
			return;
		final Runnable answered = locationAnswer;
		locationAnswer = null;
		answered.run();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		if (!hasFocus)
			return;
		applySystemBars();
		if (overlay != null)
			overlay.bringToFront();
	}

	void setFullScreen(boolean on)
	{
		if (fullScreen == on)
			return;
		fullScreen = on;
		applySystemBars();
	}

	void applySystemBars()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
		{
			final int cutout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
					? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
					: WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
			final WindowManager.LayoutParams attributes = getWindow().getAttributes();
			if (attributes.layoutInDisplayCutoutMode != cutout)
			{
				attributes.layoutInDisplayCutoutMode = cutout;
				getWindow().setAttributes(attributes);
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
		{
			getWindow().setDecorFitsSystemWindows(false);
			final WindowInsetsController bars = getWindow().getInsetsController();
			if (bars == null)
				return;
			if (!fullScreen)
			{
				bars.show(WindowInsets.Type.systemBars());
				return;
			}
			bars.hide(WindowInsets.Type.systemBars());
			bars.setSystemBarsBehavior(
					WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
		}
		else
		{
			final int edgeToEdge = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
			getWindow().getDecorView().setSystemUiVisibility(fullScreen
					? edgeToEdge
					  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					  | View.SYSTEM_UI_FLAG_FULLSCREEN
					  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					: edgeToEdge);
		}
	}
}
