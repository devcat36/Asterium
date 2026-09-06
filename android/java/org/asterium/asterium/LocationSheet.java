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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class LocationSheet extends Sheet
{
	private static final int AUTO = 0, TYPED = 1, CITY = 2;
	private static final String[] MODES = { "Locate me", "Coordinates", "City" };

	private final TextView currentPlace;
	private final TextView currentCoords;
	private final WorldMap map;
	private final TextView mapHint;
	private final TextView modeHeading;
	private final LinearLayout modeChips;
	private final LinearLayout pane;
	private final TextView planetValue;
	private final TextView zoneValue;
	private View zoneRow;
	private final android.widget.Switch customZone;
	private final android.widget.CompoundButton.OnCheckedChangeListener customZoneListener =
			(button, checked) -> NativeBridge.send("prop.set",
					"StelCore.flagUseCTZ=" + (checked ? "true" : "false"));
	private final Handler debounce = new Handler(Looper.getMainLooper());

	private int mode = AUTO;
	private Runnable pendingSearch;
	private double lat, lon;
	private int altitude;
	private String planet = "Earth";
	private String zone = "";
	private boolean observer;
	private TextView autoStatus;
	private EditText latField, lonField, altField;
	private LinearLayout results;

	LocationSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "location", "Location");

		final LinearLayout current = new LinearLayout(context);
		current.setOrientation(LinearLayout.VERTICAL);
		current.setBackground(Theme.box(Theme.HAIRLINE, 12));
		Theme.padding(current, 16, 14, 16, 14);
		currentPlace = Theme.text(context, "", 17, Theme.ACCENT, false);
		currentCoords = Theme.text(context, "", 12, Theme.TEXT_DIM, true);
		current.addView(currentPlace);
		current.addView(currentCoords);
		body.addView(current, margins(16, 16, 0));

		mapHint = Theme.text(context, "Tap the map to stand there.", 11, Theme.TEXT_FAINT, false);
		mapHint.setGravity(Gravity.CENTER_HORIZONTAL);

		map = new WorldMap(context);
		map.setListener((pickedLat, pickedLon, done) ->
		{
			mapHint.setText(T.t(String.format(Locale.US, "%s   %s",
					degrees(pickedLat, 'N', 'S'), degrees(pickedLon, 'E', 'W'))));
			if (!done)
				return;
			lat = pickedLat;
			lon = pickedLon;
			fillTypedFields();
			send(null);
		});
		body.addView(map, margins(16, 14, 0));
		body.addView(mapHint, margins(16, 8, 0));

		body.addView(Widgets.section(context, "Planet and time zone"));
		body.addView(Widgets.hairline(context));
		planetValue = Theme.text(context, "", 13, Theme.ACCENT_LINK, false);
		zoneValue = Theme.text(context, "", 13, Theme.ACCENT_LINK, false);
		body.addView(Widgets.pickerRow(context, "Planet", planetValue, v -> pickPlanet()));
		body.addView(Widgets.hairline(context));
		zoneRow = Widgets.pickerRow(context, "Time zone", zoneValue, v -> pickZone());
		body.addView(zoneRow);
		body.addView(Widgets.hairline(context));

		customZone = Widgets.toggle(context, false);
		body.addView(switchRow(context, "Use custom time zone",
				"Otherwise every move adopts the zone of the new place.", customZone));
		customZone.setOnCheckedChangeListener(customZoneListener);

		modeHeading = Widgets.section(context, "Set the place");
		body.addView(modeHeading);
		modeChips = new LinearLayout(context);
		modeChips.setOrientation(LinearLayout.HORIZONTAL);
		body.addView(modeChips, margins(16, 0, 0));

		pane = new LinearLayout(context);
		pane.setOrientation(LinearLayout.VERTICAL);
		body.addView(pane, margins(0, 8, 0));

		showMode(AUTO);
	}

	private LinearLayout.LayoutParams margins(int side, int top, int bottom)
	{
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = params.rightMargin = Theme.dp(side);
		params.topMargin = Theme.dp(top);
		params.bottomMargin = Theme.dp(bottom);
		return params;
	}

	private void showMode(int wanted)
	{
		mode = wanted;
		if (getContext() instanceof AsteriumActivity)
			((AsteriumActivity) getContext()).stopLocating();
		results = null;
		latField = null;

		modeChips.removeAllViews();
		for (int i = 0; i < MODES.length; ++i)
		{
			final int index = i;
			modeChips.addView(Widgets.filterChip(getContext(), MODES[i], i == mode,
					v -> showMode(index)));
		}

		pane.removeAllViews();
		switch (mode)
		{
			case AUTO:  buildAutoPane();  break;
			case TYPED: buildTypedPane(); break;
			case CITY:  buildCityPane();  break;
			default: break;
		}
	}

	private void buildAutoPane()
	{
		final Context context = getContext();
		pane.addView(Widgets.primaryButton(context, "Use my location", v -> locate()),
				margins(16, 6, 0));

		autoStatus = Theme.text(context, "", 12, Theme.TEXT_DIM, false);
		autoStatus.setGravity(Gravity.CENTER_HORIZONTAL);
		pane.addView(autoStatus, margins(16, 10, 16));
	}

	private void buildTypedPane()
	{
		final Context context = getContext();
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		latField = field("Latitude", "north +", true);
		lonField = field("Longitude", "east +", true);
		altField = field("Elevation in metres", "0", true);
		row.addView(column(context, "LATITUDE", latField), fieldParams(1f, false));
		row.addView(column(context, "LONGITUDE", lonField), fieldParams(1f, true));
		row.addView(column(context, "ELEV. M", altField), fieldParams(0.8f, true));
		pane.addView(row, margins(16, 6, 0));
		fillTypedFields();

		pane.addView(Widgets.primaryButton(context, "Go there", v -> goToTyped()),
				margins(16, 14, 0));
		pane.addView(Widgets.note(context,
				"Decimal degrees. −33.87 means 33.87° south. Elevation is metres above "
				+ "sea level; it moves the horizon down by a few arcminutes."));
	}

	private void buildCityPane()
	{
		final Context context = getContext();
		final EditText filter = field("Find a city", "Find a city", false);
		filter.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		pane.addView(filter, margins(16, 6, 0));
		filter.addTextChangedListener(new TextWatcher()
		{
			public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
			public void onTextChanged(CharSequence s, int a, int b, int c) {}
			public void afterTextChanged(Editable s)
			{
				if (pendingSearch != null)
					debounce.removeCallbacks(pendingSearch);
				pendingSearch = () -> loadCities(filter.getText().toString());
				debounce.postDelayed(pendingSearch, 220);
			}
		});

		results = new LinearLayout(context);
		results.setOrientation(LinearLayout.VERTICAL);
		pane.addView(results, margins(0, 10, 0));
		loadCities("");
	}

	private void loadCities(String needle)
	{
		final LinearLayout into = results;
		NativeBridge.request("cities", needle, payload ->
		{
			if (into != results)
				return;
			into.removeAllViews();
			final JSONArray list = payload.optJSONArray("cities");
			if (list == null || list.length() == 0)
			{
				into.addView(Widgets.note(getContext(), "No city matches that."));
				return;
			}
			for (int i = 0; i < list.length(); ++i)
			{
				final JSONObject city = list.optJSONObject(i);
				if (city == null)
					continue;
				final String id = city.optString("id");
				into.addView(Widgets.navigationRow(getContext(),
						city.optString("name"), city.optString("sub"),
						v -> NativeBridge.send("city.set", id)));
				into.addView(Widgets.hairline(getContext()));
			}
		});
	}

	private void fillTypedFields()
	{
		if (latField == null)
			return;
		latField.setText(T.t(String.format(Locale.US, "%.4f", lat)));
		lonField.setText(T.t(String.format(Locale.US, "%.4f", lon)));
		altField.setText(T.t(String.valueOf(altitude)));
	}

	private void goToTyped()
	{
		final double typedLat = parse(latField), typedLon = parse(lonField);
		final double typedAlt = parse(altField);
		if (Double.isNaN(typedLat) || Double.isNaN(typedLon)
		    || Math.abs(typedLat) > 90. || Math.abs(typedLon) > 180.)
		{
			mapHint.setText(T.t("Latitude −90…90, longitude −180…180."));
			return;
		}
		lat = typedLat;
		lon = typedLon;
		if (!Double.isNaN(typedAlt))
			altitude = (int) Math.round(typedAlt);
		map.setMarker(lat, lon);
		send(null);
	}

	private static double parse(EditText field)
	{
		return Widgets.number(field);
	}

	private void pickPlanet()
	{
		final PickerSheet picker = new PickerSheet(getContext(), overlay(), "Planet", planet,
				id ->
				{
					planet = id;
					send(id);
				});
		overlay().open(picker);
		NativeBridge.request("planets", "", payload -> fill(picker, payload, "planets"));
	}

	private void pickZone()
	{
		final PickerSheet picker = new PickerSheet(getContext(), overlay(), "Time zone",
				zone, id -> NativeBridge.send("tz.set", id));
		overlay().open(picker);
		NativeBridge.request("timezones", "", payload -> fill(picker, payload, "zones"));
	}

	private static void fill(PickerSheet picker, JSONObject payload, String key)
	{
		final JSONArray list = payload.optJSONArray(key);
		final List<String> ids = new ArrayList<>(), labels = new ArrayList<>();
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject item = list.optJSONObject(i);
			if (item == null)
				continue;
			ids.add(item.optString("id"));
			labels.add(item.optString("name"));
		}
		picker.setItems(ids, labels);
	}

	private void locate()
	{
		if (!(getContext() instanceof AsteriumActivity))
			return;
		status("Locating…");
		((AsteriumActivity) getContext()).findLocation(new AsteriumActivity.LocationSink()
		{
			@Override public void onFix(Location fix, boolean fresh)
			{
				apply(fix, fresh);
			}

			@Override public void onTrouble(AsteriumActivity.LocationTrouble trouble)
			{
				status(explain(trouble));
			}
		});
	}

	private static String explain(AsteriumActivity.LocationTrouble trouble)
	{
		switch (trouble)
		{
			case NO_SERVICE:
				return "This device has no location service.";
			case PROVIDERS_OFF:
				return "Location is switched off in the system settings.";
			case NO_FIX:
				return "No fresh fix - the receiver may have no sky. "
						+ "Try again outdoors, or set the place another way.";
			default:
				return "Location permission refused. The other two ways still work.";
		}
	}

	private void apply(Location fix, boolean fresh)
	{
		lat = fix.getLatitude();
		lon = fix.getLongitude();
		if (fix.hasAltitude())
			altitude = (int) Math.round(fix.getAltitude());
		map.setMarker(lat, lon);
		fillTypedFields();
		planet = "Earth";
		send("Earth");
		status(String.format(Locale.US, "%s from %s, ±%.0f m",
				fresh ? "Fix" : "Last known position", fix.getProvider(), fix.getAccuracy()));
	}

	private void status(String text)
	{
		if (autoStatus != null)
			autoStatus.setText(T.t(text));
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		if (getContext() instanceof AsteriumActivity)
			((AsteriumActivity) getContext()).stopLocating();
		debounce.removeCallbacksAndMessages(null);
	}

	private void send(String toPlanet)
	{
		NativeBridge.send("loc.set", String.format(Locale.US, "%.5f,%.5f,%d,%s",
				lat, lon, altitude, toPlanet == null ? "" : toPlanet));
	}

	@Override
	void onState(JSONObject state)
	{
		currentPlace.setText(T.t(state.optString("place")));
		currentCoords.setText(T.t(state.optString("coords")));
		lat = state.optDouble("lat", lat);
		lon = state.optDouble("lon", lon);
		altitude = state.optInt("alt", altitude);
		planet = state.optString("planet", planet);
		planetValue.setText(T.t(planet));
		zone = state.optString("tzName");
		zoneValue.setText(T.t(zoneLabel(zone)));
		map.setMarker(lat, lon);
		map.setMap(state.optString("planetMap"));

		final boolean custom = state.optBoolean("customTz");
		if (custom != customZone.isChecked())
		{
			Widgets.restate(customZone, custom, customZoneListener);
		}
		zoneRow.setEnabled(custom);
		zoneRow.setAlpha(custom ? 1f : 0.45f);

		final boolean isObserver = state.optBoolean("observer");
		if (isObserver != observer)
		{
			observer = isObserver;
			modeHeading.setVisibility(isObserver ? GONE : VISIBLE);
			modeChips.setVisibility(isObserver ? GONE : VISIBLE);
			pane.setVisibility(isObserver ? GONE : VISIBLE);
			currentCoords.setVisibility(isObserver ? GONE : VISIBLE);
			map.setPickable(!isObserver);
			mapHint.setText(T.t(isObserver
					? "An observer floats above its planet, so there is nowhere on it to "
					  + "stand. Choose another planet to come back down."
					: "Tap the map to stand there."));
		}
	}

	private static String zoneLabel(String id)
	{
		switch (id)
		{
			case "LMST":           return "Local Mean Solar Time";
			case "LTST":           return "Local True Solar Time";
			case "system_default": return "System default";
			default:               return id;
		}
	}

	private EditText field(String name, String hint, boolean numeric)
	{
		return Widgets.field(getContext(), name, hint, numeric);
	}

	private View column(Context context, String caption, EditText field)
	{
		final LinearLayout box = new LinearLayout(context);
		box.setOrientation(LinearLayout.VERTICAL);
		final TextView label = Theme.text(context, caption, 10, Theme.TEXT_FAINT, true);
		label.setLetterSpacing(0.12f);
		label.setSingleLine(true);
		Theme.padding(label, 2, 0, 2, 6);
		box.addView(label);
		box.addView(field, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(52)));
		return box;
	}

	private LinearLayout.LayoutParams fieldParams(float weight, boolean spaced)
	{
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
		if (spaced)
			params.leftMargin = Theme.dp(10);
		return params;
	}

	private View switchRow(Context context, String label, String sub,
	                       android.widget.Switch control)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 16, 8, 16, 8);
		row.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 0), 0));
		row.setOnClickListener(v -> control.toggle());
		row.setContentDescription(T.t(label));

		final LinearLayout titles = new LinearLayout(context);
		titles.setOrientation(LinearLayout.VERTICAL);
		titles.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false));
		final TextView subtitle = Theme.text(context, sub, 11, Theme.TEXT_DIM, false);
		subtitle.setLineSpacing(Theme.dp(2), 1f);
		titles.addView(subtitle);
		row.addView(titles, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		row.addView(control);
		return row;
	}

	private static String degrees(double value, char positive, char negative)
	{
		final double magnitude = Math.abs(value);
		final int d = (int) magnitude;
		final int m = (int) Math.round((magnitude - d) * 60.);
		return String.format(Locale.US, "%d°%02d'%s", m == 60 ? d + 1 : d, m == 60 ? 0 : m,
				value < 0 ? negative : positive);
	}

	private static final class WorldMap extends View
	{
		interface Listener
		{
			void onPick(double lat, double lon, boolean done);
		}

		private static Bitmap surface;
		private static String surfaceAsset;

		private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final RectF frame = new RectF();
		private final Path clip = new Path();
		private Listener listener;
		private double lat = Double.NaN, lon = Double.NaN;
		private boolean picking;
		private boolean pickable = true;

		WorldMap(Context context)
		{
			super(context);
			setContentDescription(T.t("Map of the planet. Tap to set the observer's place."));
		}

		void setListener(Listener value)
		{
			listener = value;
		}

		void setPickable(boolean value)
		{
			pickable = value;
			picking = false;
			invalidate();
		}

		void setMap(String asset)
		{
			if (asset == null || asset.isEmpty() || asset.equals(surfaceAsset))
				return;
			surfaceAsset = asset;
			surface = decode(getContext(), asset);
			invalidate();
		}

		private static Bitmap decode(Context context, String asset)
		{
			final BitmapFactory.Options bounds = new BitmapFactory.Options();
			bounds.inJustDecodeBounds = true;
			try (InputStream in = context.getAssets().open(asset))
			{
				BitmapFactory.decodeStream(in, null, bounds);
			}
			catch (Exception e)
			{
				return null;
			}
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			options.inSampleSize = Math.max(1, bounds.outWidth / 1400);
			try (InputStream in = context.getAssets().open(asset))
			{
				return BitmapFactory.decodeStream(in, null, options);
			}
			catch (Exception e)
			{
				return null;
			}
		}

		void setMarker(double newLat, double newLon)
		{
			if (picking)
				return;
			lat = newLat;
			lon = newLon;
			invalidate();
		}

		@Override
		protected void onMeasure(int widthSpec, int heightSpec)
		{
			final int width = MeasureSpec.getSize(widthSpec);
			setMeasuredDimension(width, width / 2);
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			frame.set(0, 0, getWidth(), getHeight());
			final float radius = Theme.dp(12);
			clip.reset();
			clip.addRoundRect(frame, radius, radius, Path.Direction.CW);
			canvas.save();
			canvas.clipPath(clip);

			if (surface != null)
			{
				paint.setFilterBitmap(true);
				canvas.drawBitmap(surface, null, frame, paint);
			}
			else
			{
				paint.setColor(0xFF10161C);
				canvas.drawRect(frame, paint);
			}

			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(Math.max(1f, Theme.dp(1) * 0.5f));
			paint.setColor(0x33FFFFFF);
			canvas.drawLine(frame.left, frame.centerY(), frame.right, frame.centerY(), paint);
			canvas.drawLine(frame.centerX(), frame.top, frame.centerX(), frame.bottom, paint);

			if (pickable && !Double.isNaN(lat) && !Double.isNaN(lon))
			{
				final float x = frame.left + (float) ((lon + 180.) / 360.) * frame.width();
				final float y = frame.top + (float) ((90. - lat) / 180.) * frame.height();
				final float reach = Theme.dp(10);
				paint.setColor(Theme.ACCENT);
				paint.setStrokeWidth(Math.max(1f, Theme.dp(1.5f)));
				canvas.drawLine(x - reach, y, x - reach * 0.35f, y, paint);
				canvas.drawLine(x + reach * 0.35f, y, x + reach, y, paint);
				canvas.drawLine(x, y - reach, x, y - reach * 0.35f, paint);
				canvas.drawLine(x, y + reach * 0.35f, x, y + reach, paint);
				canvas.drawCircle(x, y, Theme.dp(5), paint);
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(picking ? Theme.ACCENT : 0xCCFDD886);
				canvas.drawCircle(x, y, Theme.dp(2.5f), paint);
			}

			canvas.restore();

			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(Math.max(1f, Theme.dp(1)));
			paint.setColor(0x33000000);
			canvas.drawRoundRect(frame, radius, radius, paint);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event)
		{
			if (!pickable)
				return false;
			switch (event.getActionMasked())
			{
				case MotionEvent.ACTION_DOWN:
					getParent().requestDisallowInterceptTouchEvent(true);
					picking = true;
					report(event, false);
					return true;
				case MotionEvent.ACTION_MOVE:
					report(event, false);
					return true;
				case MotionEvent.ACTION_UP:
					picking = false;
					report(event, true);
					return true;
				case MotionEvent.ACTION_CANCEL:
					picking = false;
					invalidate();
					return true;
				default:
					return false;
			}
		}

		private void report(MotionEvent event, boolean done)
		{
			if (frame.width() <= 0f)
				return;
			final float x = Math.min(Math.max(event.getX(), frame.left), frame.right);
			final float y = Math.min(Math.max(event.getY(), frame.top), frame.bottom);
			lon = (x - frame.left) / frame.width() * 360. - 180.;
			lat = 90. - (y - frame.top) / frame.height() * 180.;
			invalidate();
			if (listener != null)
				listener.onPick(lat, lon, done);
		}
	}
}
