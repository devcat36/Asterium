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
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class WutPage extends AstroCalcPage
{
	private JSONObject state = new JSONObject();
	private final List<JSONObject> rows = new ArrayList<>();
	private BaseAdapter adapter;
	private int expanded = -1;

	private LinearLayout controls;

	@Override
	View build()
	{
		host.addButton("Clear", false, () ->
		{
			NativeBridge.send("wut.clear");
			refresh();
		});
		host.addButton("Calculate", true, this::calculate);

		final Context context = context();
		controls = new LinearLayout(context);
		controls.setOrientation(LinearLayout.VERTICAL);

		adapter = new BaseAdapter()
		{
			@Override public int getCount() { return rows.size(); }
			@Override public Object getItem(int position) { return rows.get(position); }
			@Override public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View recycled, ViewGroup parent)
			{
				final Row row = recycled instanceof Row ? (Row) recycled : new Row(parent.getContext());
				row.bind(rows.get(position), position == expanded, () -> toggle(position));
				return row;
			}
		};

		final ListView list = resultsList(controls, adapter);

		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);
		column.addView(list, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
		return column;
	}

	@Override
	void onShow()
	{
		refresh();
	}

	private void refresh()
	{
		NativeBridge.request("wut", "", payload -> { state = payload; show(); });
	}

	private void calculate()
	{
		host.setSummary("Calculating…");
		scrollToNextResult = true;
		NativeBridge.request("wut.generate", "", payload -> { state = payload; show(); });
	}

	private void set(String key, String value)
	{
		NativeBridge.send("wut.set", key + "=" + value);
		calculate();
	}

	private void toggle(int position)
	{
		expanded = (expanded == position) ? -1 : position;
		adapter.notifyDataSetChanged();
	}

	private void show()
	{
		rows.clear();
		rows.addAll(rowsOf(state, "rows"));
		expanded = -1;
		adapter.notifyDataSetChanged();
		buildControls();
		showResults(rows.size());

		final String when = state.optString("when");
		if (rows.isEmpty())
			host.setSummary("Nothing computed yet, or nothing in this category is up."
					+ (when.isEmpty() ? "" : "\n" + when));
		else
			host.setSummary(rows.size() + (rows.size() == 1 ? " object" : " objects")
					+ (when.isEmpty() ? "" : "\nUp at " + when));
	}

	private void buildControls()
	{
		final Context context = context();
		controls.removeAllViews();

		final TextView category = pickerValue();
		setPickerValue(category, state.optString("category", "0"), state.optJSONArray("categories"));
		controls.addView(pickerRow("Category", "Category", category,
				() -> state.optJSONArray("categories"), id -> set("category", id)));

		final TextView interval = pickerValue();
		setPickerValue(interval, state.optString("interval", "0"), state.optJSONArray("intervals"));
		controls.addView(pickerRow("Objects which are up", "Show objects which are up", interval,
				() -> state.optJSONArray("intervals"), id -> set("wut_time_interval", id)));

		controls.addView(sliderRow("Up to mag.", 0., 25., state.optDouble("mag", 10.),
				v -> set("wut_magnitude_limit", v)));
		controls.addView(sliderRow("Above altitude, °", 0., 90., state.optDouble("altitude", 0.),
				v -> set("wut_altitude_min", v)));

		if (!state.optBoolean("angularLimits", true))
			return;
		final boolean limiting = state.optBoolean("angularLimit", false);
		controls.addView(switchRow(state.optString("limitLabel", "Limit angular size"), null,
				limiting, (button, checked) ->
						set("wut_angular_limit_flag", checked ? "true" : "false")));
		if (!limiting)
			return;

		controls.addView(sliderRow("Smallest, ′", 0., 600., state.optDouble("angularMin", 10.),
				v -> set("wut_angular_limit_min", v)));
		controls.addView(sliderRow("Largest, ′", 0., 600., state.optDouble("angularMax", 600.),
				v -> set("wut_angular_limit_max", v)));
	}

	private final class Row extends LinearLayout
	{
		private final TextView name, type, mag, times;
		private final LinearLayout detail;

		Row(Context context)
		{
			super(context);
			setOrientation(VERTICAL);

			final LinearLayout head = new LinearLayout(context);
			head.setOrientation(HORIZONTAL);
			head.setGravity(Gravity.CENTER_VERTICAL);
			head.setMinimumHeight(Theme.dp(52));
			Theme.padding(head, 16, 8, 16, 8);

			final LinearLayout titles = new LinearLayout(context);
			titles.setOrientation(VERTICAL);
			name = Theme.text(context, "", 14, Theme.TEXT, false);
			name.setSingleLine(true);
			name.setEllipsize(android.text.TextUtils.TruncateAt.END);
			type = Theme.text(context, "", 10, Theme.TEXT_DIM, false);
			type.setSingleLine(true);
			titles.addView(name);
			titles.addView(type);

			head.addView(titles, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

			final LinearLayout numbers = new LinearLayout(context);
			numbers.setOrientation(VERTICAL);
			mag = Theme.text(context, "", 14, Theme.ACCENT, true);
			mag.setGravity(Gravity.END);
			times = Theme.text(context, "", 10, Theme.TEXT_DIM, true);
			times.setGravity(Gravity.END);
			numbers.addView(mag);
			numbers.addView(times);
			final LayoutParams numberParams =
					new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			numberParams.leftMargin = Theme.dp(10);
			head.addView(numbers, numberParams);
			addView(head);

			detail = new LinearLayout(context);
			detail.setOrientation(VERTICAL);
			detail.setBackgroundColor(Theme.FILL_FAINT);
			Theme.padding(detail, 0, 4, 0, 10);
			addView(detail);
		}

		void bind(final JSONObject entry, boolean open, final Runnable click)
		{
			final Context context = getContext();
			name.setText(T.t(entry.optString("name")));
			type.setText(T.t(entry.optString("type")));
			mag.setText(T.t(state.optBoolean("showMag", true) ? entry.optString("mag") : ""));

			final String rise = entry.optString("rise"), sets = entry.optString("set");
			times.setText(T.t(rise.equals("—") && sets.equals("—")
					? entry.optString("transit") : rise + " → " + sets));

			setBackground(Theme.pressable(Theme.box(open ? Theme.FILL_SOFT : 0, 0), 0));
			setOnClickListener(v -> click.run());
			detail.setVisibility(open ? VISIBLE : GONE);
			detail.removeAllViews();
			if (!open)
				return;

			if (state.optBoolean("showMag", true))
				detail.addView(Widgets.valueRow(context, state.optString("magLabel", "Mag."),
						entry.optString("mag")));
			detail.addView(Widgets.valueRow(context, state.optString("riseLabel", "Rise"), rise));
			detail.addView(Widgets.valueRow(context, state.optString("transitLabel", "Transit"),
					entry.optString("transit")));
			detail.addView(Widgets.valueRow(context, state.optString("elevLabel", "Elev."),
					entry.optString("elev")));
			detail.addView(Widgets.valueRow(context, state.optString("setLabel", "Set"), sets));
			if (state.optBoolean("showSize", true))
				detail.addView(Widgets.valueRow(context, state.optString("sizeLabel", "Ang. Size"),
						entry.optString("size")));
			detail.addView(Widgets.valueRow(context, state.optString("constLabel", "Const."),
					entry.optString("const")));

			final TextView button = Widgets.primaryButton(context, "Show in sky", v ->
			{
				NativeBridge.send("wut.select", entry.optString("select"));
				host.overlay().close(host.sheet());
			});
			final LayoutParams params =
					new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(Theme.dp(18), Theme.dp(8), Theme.dp(18), 0);
			detail.addView(button, params);
		}
	}
}
