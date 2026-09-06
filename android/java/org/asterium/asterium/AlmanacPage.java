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
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

final class AlmanacPage extends AstroCalcPage
{
	private JSONObject state = new JSONObject();
	private FrameLayout body;
	private LinearLayout root;
	private boolean today = true;

	@Override
	View build()
	{
		root = new LinearLayout(context());
		root.setOrientation(LinearLayout.VERTICAL);
		body = new FrameLayout(context());
		buildTabs();
		root.addView(body, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
		return root;
	}

	@Override
	void onShow()
	{
		refresh();
	}

	private void buildTabs()
	{
		if (root.getChildCount() > 0 && root.getChildAt(0) != body)
			root.removeViewAt(0);
		root.addView(subTabs(new String[] { "Today", "Seasons" }, today ? 0 : 1, index ->
		{
			today = (index == 0);
			buildTabs();
			show();
		}), 0);
	}

	private void refresh()
	{
		NativeBridge.request("almanac", "", payload -> { state = payload; show(); });
	}

	private void set(String key, String value)
	{
		NativeBridge.send("almanac.set", key + "=" + value);
		refresh();
	}

	private void show()
	{
		if (body == null)
			return;
		body.removeAllViews();
		final ScrollView scroll = new ScrollView(context());
		final LinearLayout column = column(scroll);
		if (today)
			fillToday(column);
		else
			fillSeasons(column);
		body.addView(scroll);
		host.setSummary(today ? state.optString("today") : "Year " + state.optInt("year", 0)
				+ "  (" + state.optString("yearDuration") + ")");
	}

	private void fillToday(LinearLayout column)
	{
		final Context context = context();
		column.addView(dayStepper());

		if (!state.optBoolean("onEarth", false))
		{
			column.addView(note("Twilight and rise and set times are worked out for an observer"
					+ " on Earth. Move back to Earth to see them."));
			return;
		}

		final JSONArray rows = state.optJSONArray("rows");
		for (int i = 0; rows != null && i < rows.length(); ++i)
		{
			final JSONObject entry = rows.optJSONObject(i);
			if (entry == null)
				continue;
			final String section = entry.optString("section");
			if (!section.isEmpty())
				column.addView(Widgets.section(context, section));
			else
				column.addView(timeRow(entry));
		}

		column.addView(Widgets.section(context, "Custom values"));
		column.addView(numberRow("Minutes before sunrise / after sunset", state.optInt("minutes", 60),
				value -> set("custom_minutes", String.valueOf(value))));
		column.addView(sliderRow("Custom altitude of the Sun, °", -90., 90.,
				state.optDouble("sunAltitude", -7.), value -> set("custom_sun_altitude", value)));
		column.addView(sliderRow("Custom altitude of the Moon, °", -90., 90.,
				state.optDouble("moonAltitude", 18.), value -> set("custom_moon_altitude", value)));
		column.addView(Widgets.gap(context, 12));
	}

	private View dayStepper()
	{
		final Context context = context();
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		Theme.padding(row, 12, 8, 12, 4);
		row.addView(stepButton("‹", () -> step(-1.)));
		final TextView date = Theme.text(context, state.optString("today"), 14, Theme.ACCENT, false);
		date.setGravity(Gravity.CENTER);
		row.addView(date, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		row.addView(stepButton("›", () -> step(1.)));
		return row;
	}

	private void step(double days)
	{
		NativeBridge.send("almanac.day", String.valueOf(days));
		refresh();
	}

	private TextView stepButton(String glyph, final Runnable action)
	{
		final TextView button = Widgets.secondaryButton(context(), glyph, v -> action.run());
		button.setMinimumWidth(Theme.dp(Theme.TOUCH));
		Theme.padding(button, 14, 0, 14, 0);
		return button;
	}

	private View timeRow(final JSONObject entry)
	{
		final Context context = context();
		final double jd = entry.optDouble("jd", 0.);
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(40));
		Theme.padding(row, 18, 6, 18, 6);

		final TextView label = Theme.text(context, entry.optString("label"), 13, Theme.TEXT_DIM, false);

		row.addView(label, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final TextView value = Theme.text(context, entry.optString("value"), 13,
				jd == 0. ? Theme.TEXT : Theme.ACCENT, true);
		value.setGravity(Gravity.END);
		row.addView(value);

		if (jd == 0.)
			return row;
		final TextView go = Theme.text(context, " ↗", 13, Theme.ACCENT_LINK, false);
		row.addView(go);
		row.setBackground(Theme.pressable(Theme.box(0, 0), 0));
		row.setContentDescription(String.format(T.t("Set the clock to %s"), entry.optString("label")));
		row.setOnClickListener(v ->
		{
			NativeBridge.send("almanac.goto", String.format(Locale.US, "%.8f", jd));
			refresh();
		});
		return row;
	}

	private void fillSeasons(LinearLayout column)
	{
		final Context context = context();
		column.addView(Widgets.note(context, "Year " + state.optInt("year", 0)
				+ " lasts " + state.optString("yearDuration") + "."));

		final JSONArray seasons = state.optJSONArray("seasons");
		for (int i = 0; seasons != null && i < seasons.length(); ++i)
		{
			final JSONObject entry = seasons.optJSONObject(i);
			if (entry == null)
				continue;
			column.addView(Widgets.section(context,
					entry.optString("season") + " · " + entry.optString("event")));
			column.addView(Widgets.valueRow(context, "Local time", entry.optString("local")));
			column.addView(Widgets.valueRow(context, "Julian day", entry.optString("julian")));
			column.addView(Widgets.valueRow(context, "Duration", entry.optString("duration")));
			column.addView(seasonButtons(i));
		}
		column.addView(Widgets.gap(context, 12));
	}

	private View seasonButtons(final int which)
	{
		final LinearLayout row = new LinearLayout(context());
		row.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(row, 14, 4, 14, 8);
		row.addView(seasonButton(which, -1, "Previous"));
		row.addView(seasonButton(which, 0, "This year"));
		row.addView(seasonButton(which, 1, "Next"));
		return row;
	}

	private View seasonButton(final int which, final int delta, String label)
	{
		final TextView button = Widgets.secondaryButton(context(), label, v ->
		{
			NativeBridge.send("almanac.season", which + "," + delta);
			refresh();
		});
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		params.setMargins(Theme.dp(4), 0, Theme.dp(4), 0);
		button.setLayoutParams(params);
		return button;
	}
}
