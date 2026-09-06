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

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

final class ConfigSheet extends PropertySheet
{
	private static final String[][] PAGES = {
		{ "Main",    "" },
		{ "Extras",  "" },
		{ "Time",    "" },
		{ "Scripts", "" },
		{ "Plugins", "" },
	};

	private static final String[] DATE_IDS = {
		"system_default", "yyyymmdd", "ddmmyyyy", "mmddyyyy",
		"wwyyyymmdd", "wwddmmyyyy", "wwmmddyyyy",
	};
	private static final String[] DATE_NAMES = {
		"System default", "yyyy-mm-dd (ISO 8601)", "dd-mm-yyyy", "mm-dd-yyyy",
		"ww, yyyy-mm-dd", "ww, dd-mm-yyyy", "ww, mm-dd-yyyy",
	};
	private static final String[] TIME_IDS = { "system_default", "12h", "24h" };
	private static final String[] TIME_NAMES = { "System default", "12-hour", "24-hour" };
	private static final String[] MODE_IDS = { "actual", "today", "preset" };
	private static final String[] MODE_NAMES = {
		"System date and time", "System date at", "Other",
	};

	private boolean restoreArmed = false;
	private TextView restoreButton;

	private TextView scriptStatus;
	private android.widget.Switch gyroSwitch;

	ConfigSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "config", "Configuration", PAGES);
	}

	@Override
	void buildCuratedPage(LinearLayout into, String pageName)
	{
		scriptStatus = null;
		gyroSwitch = null;
		restoreButton = null;
		restoreArmed = false;

		if ("Extras".equals(pageName))
			buildExtras(into);
		else if ("Time".equals(pageName))
			buildTime(into);
		else if ("Scripts".equals(pageName))
			buildScripts(into);
		else if ("Plugins".equals(pageName))
			buildPlugins(into);
		else
			buildMain(into);
	}

	private void buildMain(LinearLayout into)
	{
		final Context context = getContext();
		NativeBridge.request("config", "main", payload ->
		{
			into.addView(Widgets.section(context, "Language settings"));
			final JSONArray languages = payload.optJSONArray("languages");
			if (languages == null || languages.length() == 0)
			{
			}
			else
			{
				final String[] ids = new String[languages.length()];
				final String[] names = new String[languages.length()];
				for (int i = 0; i < languages.length(); ++i)
				{
					final JSONObject item = languages.optJSONObject(i);
					ids[i] = item == null ? "" : item.optString("id");
					names[i] = item == null ? "" : item.optString("name");
				}
				addPick(into, "Program Language", payload.optString("language"), ids, names,
				        id ->
				        {
					        NativeBridge.send("lang.set", id);
					        overlay().relanguage();
				        });
			}

			into.addView(Widgets.section(context, "Default options"));
			final TextView fovValue = readoutText(context, payload.optString("startupFov"));
			final TextView viewValue = readoutText(context, payload.optString("startupView"));
			into.addView(readout(context, "Startup FOV", fovValue));
			into.addView(Widgets.hairline(context));
			into.addView(readout(context, "Startup direction of view", viewValue));
			into.addView(Widgets.hairline(context));
			into.addView(button(context, "Save view", v ->
			{
				NativeBridge.send("view.save");
				NativeBridge.request("config", "main", again ->
				{
					fovValue.setText(T.t(again.optString("startupFov")));
					viewValue.setText(T.t(again.optString("startupView")));
				});
			}));

			into.addView(button(context, "Save settings", v -> NativeBridge.send("config.save")));

			restoreButton = button(context, "Restore defaults", v -> onRestoreTapped());
			into.addView(restoreButton);
			into.addView(Widgets.note(context,
					"Save either the current FOV and direction of view or all the "
					+ "current options for use at next startup. Restoring default "
					+ "settings requires a restart of the app."));
		});
	}

	private void onRestoreTapped()
	{
		if (!restoreArmed)
		{
			restoreArmed = true;
			restoreButton.setText(T.t("Tap again to confirm"));
			restoreButton.setTextColor(Theme.ACCENT);
			return;
		}
		restoreButton.setText(T.t("Restarting…"));
		restoreButton.setTextColor(Theme.ACCENT);
		NativeBridge.request("config.restore", "",
				payload -> AsteriumActivity.restart(getContext()));
	}

	private void buildExtras(LinearLayout into)
	{
		final Context context = getContext();

		if (overlay().canPoint())
		{
			into.addView(Widgets.section(context, "Pointing"));
			final View row = Widgets.switchRow(context, "Gyro mode",
					"Move the sky by pointing the phone at it", overlay().isPointing(),
					(button, checked) -> overlay().setPointing(checked));
			gyroSwitch = Widgets.switchIn(row);
			into.addView(row);
			into.addView(Widgets.hairline(context));
		}

		into.addView(Widgets.section(context, "Additional information settings"));
		addSwitch(into, "Use mag/arcsec^2 for surface brightness", null,
		          "NebulaMgr.flagSurfaceBrightnessArcsecUsage");
		addSwitch(into, "Short notation for units of surface brightness", null,
		          "NebulaMgr.flagSurfaceBrightnessShortNotationUsage");
		addSwitch(into, "Designations for celestial coordinate systems",
		          "Use common symbols for coordinate systems",
		          "StelApp.flagUseCCSDesignation");
		addSwitch(into, "Tabular output for coordinates and time", null,
		          "StelApp.flagUseFormattingOutput");
		addSwitch(into, "Use decimal degrees", "Use decimal degrees for coordinates",
		          "StelApp.flagShowDecimalDegrees");
		addSwitch(into, "Azimuth from South",
		          "Activate this option to calculate azimuth from south towards west.",
		          "StelApp.flagUseAzimuthFromSouth");
		addSwitch(into, "Allow negative hour angles",
		          "Hour angles usually run 0...24h. This counts -12h...12h.",
		          "StelApp.flagUseNegativeHourAngles");
		addSwitch(into, "Polar distance, not declination",
		          "Use distance from North Celestial Pole, not declination from equator "
		          + "in equatorial coordinates",
		          "StelApp.flagUsePolarDistance");
		addSwitch(into, "Frame rate",
		          "Show frames per second beside the field of view",
		          "AndroidUi.flagShowFps");

		buildAdditionalButtons(into);
	}

	private void buildAdditionalButtons(LinearLayout into)
	{
		final Context context = getContext();
		into.addView(Widgets.section(context, "Show additional buttons"));
		addButtonSwitch(into, "Constellation boundaries", null, "StelGui.flagShowConstellationBoundariesButton");
		addButtonSwitch(into, "Constellation art", null, "StelGui.flagShowConstellationArtsButton");
		addButtonSwitch(into, "Asterism lines", null, "StelGui.flagShowAsterismLinesButton");
		addButtonSwitch(into, "Asterism labels", null, "StelGui.flagShowAsterismLabelsButton");
		addButtonSwitch(into, "Ecliptic grid",
		                "A button to toggle ecliptic grid of date",
		                "StelGui.flagShowEclipticGridButton");
		addButtonSwitch(into, "ICRS grid",
		                "A button to toggle equatorial J2000 grid",
		                "StelGui.flagShowICRSGridButton");
		addButtonSwitch(into, "Galactic grid",
		                "A button to toggle galactic grid",
		                "StelGui.flagShowGalacticGridButton");
		addButtonSwitch(into, "Cardinal points",
		                "Show button in addition to keyboard shortcut?",
		                "StelGui.flagShowCardinalButton");
		addButtonSwitch(into, "Compass marks",
		                "Show button in addition to keyboard shortcut?",
		                "StelGui.flagShowCompassButton");
		addButtonSwitch(into, "Night mode",
		                "Show button in addition to keyboard shortcut?",
		                "StelGui.flagShowNightmodeButton");
		addButtonSwitch(into, "Fullscreen button",
		                "Show button in addition to keyboard shortcut?",
		                "StelGui.flagShowFullscreenButton");
		addButtonSwitch(into, "Quit button",
		                "Show button in addition to keyboard shortcut?",
		                "StelGui.flagShowQuitButton");
		addButtonSwitch(into, "Nebula background",
		                "Toggle display of nebula images.",
		                "StelGui.flagShowNebulaBackgroundButton");
		addButtonSwitch(into, "Flip buttons",
		                "Toggle vertical and horizontal image flip buttons.",
		                "StelGui.flagShowFlipButtons");
		addSwitch(into, "Use buttons background",
		                "Toggle the usage of background under GUI buttons",
		                "StelGui.flagUseButtonsBackground");
		addButtonSwitch(into, "Observing list highlight",
		                "A button to mark every object of the current observing list "
		                + "on the sky.",
		                "StelGui.flagShowObsListButton");
	}

	private void addButtonSwitch(LinearLayout into, String label, String sub, final String propertyId)
	{
		final JSONObject property = value(propertyId);
		final boolean on = property != null && property.optBoolean("value");
		into.addView(Widgets.switchRow(getContext(), label, sub, on, (button, checked) ->
		{
			NativeBridge.send("prop.set", propertyId + "=" + checked);
			overlay().rebuildToolbar();
		}));
		into.addView(Widgets.hairline(getContext()));
	}

	private void buildPlugins(LinearLayout into)
	{
		final Context context = getContext();
		into.addView(Widgets.section(context, "Plug-ins"));
		into.addView(Widgets.navigationRow(context, "Oculars",
				"Shows the sky as if looking through a telescope eyepiece",
				v -> overlay().open(new OcularsSheet(context, overlay()))));
		into.addView(Widgets.hairline(context));
	}

	private void buildTime(LinearLayout into)
	{
		final Context context = getContext();
		NativeBridge.request("config", "time", payload ->
		{
			into.addView(Widgets.section(context, "Startup date and time"));

			final TextView todayValue = Theme.text(context, payload.optString("todayTime"),
			                                       14, Theme.ACCENT, false);
			final View todayRow = Widgets.pickerRow(context, "Time of day", todayValue,
			                                        v -> pickTimeOfDay(todayValue));

			final TextView presetValue = readoutText(context, payload.optString("preset"));
			final LinearLayout presetBlock = new LinearLayout(context);
			presetBlock.setOrientation(LinearLayout.VERTICAL);
			presetBlock.addView(readout(context, "Fixed instant", presetValue));
			presetBlock.addView(button(context, "use current", v ->
			{
				NativeBridge.send("time.presetNow");
				final JSONObject state = NativeBridge.lastState();
				final String clock = state == null ? "" : state.optString("clock");
				presetValue.setText(T.t(clock.length() >= 16 ? clock.substring(0, 16) : clock));
			}));

			addPick(into, "Startup date and time", payload.optString("startupMode"), MODE_IDS, MODE_NAMES,
			        id ->
			        {
				        NativeBridge.send("time.startupMode", id);
				        showMode(id, todayRow, presetBlock);
			        });
			into.addView(todayRow);
			into.addView(presetBlock);
			showMode(payload.optString("startupMode"), todayRow, presetBlock);

			addSwitch(into, "Stop clock when starting", null, "StelCore.startupTimeStop");

			into.addView(Widgets.section(context, "Display formats of date and time"));
			addPick(into, "Date", payload.optString("dateFormat"), DATE_IDS, DATE_NAMES,
			        id -> NativeBridge.send("fmt.date", id));
			addPick(into, "Time", payload.optString("timeFormat"), TIME_IDS, TIME_NAMES,
			        id -> NativeBridge.send("fmt.time", id));

			into.addView(Widgets.section(context, "Time correction"));
			final JSONArray algorithms = payload.optJSONArray("algorithms");
			final int count = algorithms == null ? 0 : algorithms.length();
			final String[] ids = new String[count];
			final String[] names = new String[count];
			for (int i = 0; i < count; ++i)
			{
				final JSONObject item = algorithms.optJSONObject(i);
				ids[i] = item == null ? "" : item.optString("id");
				names[i] = item == null ? "" : item.optString("name");
			}
			final TextView description = Widgets.note(context, "");
			description.setText(html(payload.optString("deltaTDescription")));
			addPick(into, "Algorithm of ΔT", payload.optString("deltaT"), ids, names, id ->
			{
				NativeBridge.send("deltat.set", id);
				NativeBridge.request("config", "time",
						again -> description.setText(html(again.optString("deltaTDescription"))));
			});
			into.addView(description);
		});
	}

	private static void showMode(String mode, View todayRow, View presetBlock)
	{
		todayRow.setVisibility("today".equals(mode) ? View.VISIBLE : View.GONE);
		presetBlock.setVisibility("preset".equals(mode) ? View.VISIBLE : View.GONE);
	}

	private void pickTimeOfDay(final TextView shown)
	{
		final String[] parts = shown.getText().toString().split(":");
		int hour = 22, minute = 0;
		try
		{
			hour = Integer.parseInt(parts[0]);
			minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
		}
		catch (NumberFormatException ignored) { }

		new TimePickerDialog(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert,
				(view, pickedHour, pickedMinute) ->
				{
					final String value = String.format(Locale.US, "%02d:%02d",
					                                   pickedHour, pickedMinute);
					shown.setText(T.t(value));
					NativeBridge.send("time.todayTime", value);
				},
				hour, minute, DateFormat.is24HourFormat(getContext())).show();
	}

	private void buildScripts(LinearLayout into)
	{
		final Context context = getContext();
		NativeBridge.request("config", "scripts", payload ->
		{
			final JSONArray list = payload.optJSONArray("scripts");
			if (list == null || list.length() == 0)
			{
				into.addView(Widgets.note(context, "This copy ships no scripts."));
				return;
			}

			scriptStatus = readoutText(context, "");
			into.addView(readout(context, "Running", scriptStatus));
			into.addView(Widgets.hairline(context));
			into.addView(button(context, "Stop a running script", v -> NativeBridge.send("script.stop")));
			setRunning(payload.optString("running"));

			into.addView(Widgets.section(context, "Scripts"));
			for (int i = 0; i < list.length(); ++i)
			{
				final JSONObject script = list.optJSONObject(i);
				if (script == null)
					continue;
				final String id = script.optString("id");
				into.addView(Widgets.navigationRow(context, script.optString("name"),
						scriptDetail(script), v -> NativeBridge.send("script.run", id)));
				into.addView(Widgets.hairline(context));
			}
		});
	}

	private static String scriptDetail(JSONObject script)
	{
		final StringBuilder out = new StringBuilder(script.optString("sub"));
		final String[][] fields = {
			{ "Author", script.optString("author") },
			{ "License", script.optString("license") },
			{ "Version", script.optString("version") },
		};
		for (String[] field : fields)
		{
			if (field[1] == null || field[1].isEmpty())
				continue;
			if (out.length() > 0)
				out.append('\n');
			out.append(field[0]).append(": ").append(field[1]);
		}
		return out.toString();
	}

	private void setRunning(String id)
	{
		if (scriptStatus == null)
			return;
		final boolean running = id != null && !id.isEmpty();
		scriptStatus.setText(running ? id : T.t("nothing"));
		scriptStatus.setTextColor(running ? Theme.ACCENT : Theme.TEXT_DIM);
	}

	@Override
	void onState(JSONObject state)
	{
		setRunning(state.optString("script"));
		if (gyroSwitch != null && gyroSwitch.isChecked() != overlay().isPointing())
			gyroSwitch.setChecked(overlay().isPointing());
	}

	private static View readout(Context context, String label, TextView value)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(40));
		Theme.padding(row, 18, 6, 18, 6);
		row.addView(Theme.text(context, label, 13, Theme.TEXT_DIM, false));
		value.setGravity(Gravity.END);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		params.leftMargin = Theme.dp(14);
		row.addView(value, params);
		return row;
	}

	private static TextView readoutText(Context context, String text)
	{
		return Theme.text(context, text, 13, Theme.TEXT, true);
	}

	private static TextView button(Context context, String label, View.OnClickListener click)
	{
		final TextView view = Widgets.secondaryButton(context, label, click);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(48));
		params.leftMargin = params.rightMargin = Theme.dp(16);
		params.topMargin = Theme.dp(6);
		view.setLayoutParams(params);
		return view;
	}

	private static CharSequence html(String markup)
	{
		return Html.fromHtml(markup == null ? "" : markup,
		                     Html.FROM_HTML_MODE_COMPACT, source -> null, null);
	}
}
