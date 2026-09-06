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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

abstract class AstroCalcPage
{
	interface Host
	{
		Context context();
		Overlay overlay();
		Sheet sheet();

		void setSummary(String text);

		TextView addButton(String label, boolean primary, Runnable action);
	}

	protected Host host;

	final void attach(Host host)
	{
		this.host = host;
	}

	protected final Context context()
	{
		return host.context();
	}

	abstract View build();

	void onShow() {}

	protected LinearLayout column(android.widget.ScrollView into)
	{
		final LinearLayout column = new LinearLayout(context());
		column.setOrientation(LinearLayout.VERTICAL);
		into.addView(column);
		return column;
	}

	protected View subTabs(String[] labels, int active, final IntAction choose)
	{
		final LinearLayout row = new LinearLayout(context());
		row.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(row, 16, 12, 16, 4);
		final android.widget.HorizontalScrollView scroll =
				new android.widget.HorizontalScrollView(context());
		scroll.setHorizontalScrollBarEnabled(false);
		for (int i = 0; i < labels.length; ++i)
		{
			final int index = i;
			row.addView(Widgets.filterChip(context(), labels[i], i == active, v ->
			{
				if (index != activeTab(scroll))
					choose.with(index);
			}));
		}
		scroll.setTag(active);
		scroll.addView(row);
		return scroll;
	}

	protected static void setActiveTab(View rail, int active)
	{
		if (!(rail instanceof ViewGroup))
			return;
		rail.setTag(active);
		final View inner = ((ViewGroup) rail).getChildAt(0);
		if (!(inner instanceof ViewGroup))
			return;
		final ViewGroup row = (ViewGroup) inner;
		for (int i = 0; i < row.getChildCount(); ++i)
		{
			if (row.getChildAt(i) instanceof TextView)
				Widgets.chipStyle((TextView) row.getChildAt(i), i == active);
		}
	}

	private static int activeTab(View rail)
	{
		return rail.getTag() instanceof Integer ? (Integer) rail.getTag() : -1;
	}

	private ListView table;
	private TextView tableHeading;

	protected ListView resultsList(View controls, android.widget.ListAdapter adapter)
	{
		table = new ListView(context());
		table.addHeaderView(controls, null, false);
		tableHeading = Widgets.section(context(), "Results");
		tableHeading.setVisibility(View.GONE);
		table.addHeaderView(tableHeading, null, false);
		table.setAdapter(adapter);
		table.setDivider(new android.graphics.drawable.ColorDrawable(Theme.HAIRLINE));
		table.setDividerHeight(Math.max(1, Theme.dp(1)));
		table.setDrawSelectorOnTop(false);
		table.setSelector(new android.graphics.drawable.ColorDrawable(0));
		return table;
	}

	protected boolean scrollToNextResult;

	protected void showResults(int count)
	{
		final boolean scroll = scrollToNextResult;
		scrollToNextResult = false;
		if (tableHeading == null)
			return;
		tableHeading.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
		if (!scroll || count == 0 || table == null)
			return;

		table.post(() -> table.setSelectionFromTop(1, 0));
	}

	protected interface IntAction { void with(int value); }
	protected interface TextAction { void with(String value); }

	protected View chipRow(JSONArray items, String current, final TextAction choose)
	{
		final LinearLayout row = new LinearLayout(context());
		row.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(row, 16, 4, 16, 10);
		for (int i = 0; items != null && i < items.length(); ++i)
		{
			final JSONObject item = items.optJSONObject(i);
			if (item == null)
				continue;
			final String id = item.optString("id");
			row.addView(Widgets.filterChip(context(), item.optString("name"),
					id.equals(current), v -> choose.with(id)));
		}
		final android.widget.HorizontalScrollView scroll =
				new android.widget.HorizontalScrollView(context());
		scroll.setHorizontalScrollBarEnabled(false);
		scroll.addView(row);
		return scroll;
	}

	protected View pickerRow(String label, String title, final TextView value,
	                         final ListSource list, final TextAction choose)
	{
		return Widgets.pickerRow(context(), label, value, v ->
		{
			final JSONArray items = list.get();
			final List<String> ids = new ArrayList<>(), labels = new ArrayList<>();
			for (int i = 0; items != null && i < items.length(); ++i)
			{
				final JSONObject item = items.optJSONObject(i);
				if (item == null)
					continue;
				ids.add(item.optString("id"));
				labels.add(item.optString("name"));
			}
			final PickerSheet picker = new PickerSheet(context(), host.overlay(), title,
					value.getTag() == null ? "" : String.valueOf(value.getTag()), choose::with);
			host.overlay().open(picker);
			picker.setItems(ids, labels);
		});
	}

	protected interface ListSource { JSONArray get(); }

	protected TextView pickerValue()
	{
		return Theme.text(context(), "", 13, Theme.ACCENT, false);
	}

	protected static void setPickerValue(TextView view, String id, JSONArray from)
	{
		if (view == null)
			return;
		view.setTag(id);
		view.setText(T.t(nameIn(from, id)));
	}

	protected static String nameIn(JSONArray list, String id)
	{
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject item = list.optJSONObject(i);
			if (item != null && id.equals(item.optString("id")))
				return item.optString("name");
		}
		return id == null || id.isEmpty() ? "—" : id;
	}

	protected View numberRow(String label, int value, final IntAction commit)
	{
		final LinearLayout row = new LinearLayout(context());
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		Theme.padding(row, 16, 8, 16, 4);
		row.addView(Theme.text(context(), label, 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final android.widget.EditText field = Widgets.field(context(), label, "", true);
		field.setGravity(Gravity.END);
		field.setText(T.t(String.valueOf(value)));
		field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
		final Runnable report = () ->
		{
			final double typed = Widgets.number(field);
			if (!Double.isNaN(typed))
				commit.with((int) Math.round(typed));
		};
		field.setOnFocusChangeListener((v, focused) -> { if (!focused) report.run(); });
		field.setOnEditorActionListener((v, action, event) -> { report.run(); return false; });
		row.addView(field, new LinearLayout.LayoutParams(Theme.dp(96), Theme.dp(48)));
		return row;
	}

	protected View sliderRow(String label, final double min, final double max, double value,
	                         final TextAction commit)
	{
		final LinearLayout group = new LinearLayout(context());
		group.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(group, 16, 10, 16, 6);

		final LinearLayout head = new LinearLayout(context());
		head.setOrientation(LinearLayout.HORIZONTAL);
		head.addView(Theme.text(context(), label, 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		final TextView shown = Theme.text(context(), "", 14, Theme.ACCENT, true);
		head.addView(shown);
		group.addView(head);

		final android.widget.SeekBar bar = Widgets.slider(context(),
				(int) Math.round((max - min) * 10.), (int) Math.round((value - min) * 10.));
		shown.setText(T.t(String.format(Locale.getDefault(), "%.1f", value)));
		bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener()
		{
			public void onProgressChanged(android.widget.SeekBar seek, int progress, boolean fromUser)
			{
				shown.setText(T.t(String.format(Locale.getDefault(), "%.1f", min + progress / 10.)));
			}
			public void onStartTrackingTouch(android.widget.SeekBar seek) {}
			public void onStopTrackingTouch(android.widget.SeekBar seek)
			{
				commit.with(String.format(Locale.US, "%.2f", min + seek.getProgress() / 10.));
			}
		});
		group.addView(bar);
		return group;
	}

	protected View switchRow(String label, String sub, boolean checked,
	                         final android.widget.CompoundButton.OnCheckedChangeListener listener)
	{
		return Widgets.switchRow(context(), label, sub, checked, listener);
	}

	protected static List<JSONObject> rowsOf(JSONObject payload, String key)
	{
		final List<JSONObject> rows = new ArrayList<>();
		final JSONArray list = payload == null ? null : payload.optJSONArray(key);
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject row = list.optJSONObject(i);
			if (row != null)
				rows.add(row);
		}
		return rows;
	}

	protected View note(String text)
	{
		return Widgets.note(context(), text);
	}

	protected static LinearLayout.LayoutParams fill()
	{
		return new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	}
}
