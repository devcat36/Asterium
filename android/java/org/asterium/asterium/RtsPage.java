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

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RtsPage extends AstroCalcPage
{
	private JSONObject state = new JSONObject();
	private final List<JSONObject> rows = new ArrayList<>();
	private BaseAdapter adapter;
	private int expanded = -1;

	private int year = 0, month = 1;
	private boolean started = false;

	private TextView objectValue, monthValue;
	private android.widget.EditText yearField, monthsField;

	@Override
	View build()
	{
		final Context context = context();
		host.addButton("Cleanup", false, () ->
		{
			NativeBridge.send("rts.clear");
			refresh();
		});
		host.addButton("Calculate", true, this::calculate);

		adapter = new BaseAdapter()
		{
			@Override public int getCount() { return rows.size(); }
			@Override public Object getItem(int position) { return rows.get(position); }
			@Override public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View recycled, ViewGroup parent)
			{
				final Row row = recycled instanceof Row ? (Row) recycled : new Row(parent.getContext());
				row.bind(rows.get(position), position);
				return row;
			}
		};

		final ListView list = resultsList(controls(), adapter);

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
		NativeBridge.request("rts", "", payload -> { state = payload; show(); });
	}

	private void calculate()
	{
		host.setSummary("Calculating…");
		scrollToNextResult = true;
		NativeBridge.request("rts.generate", String.format(Locale.US, "%04d-%02d", year, month),
				payload -> { state = payload; show(); });
	}

	private View controls()
	{
		final Context context = context();
		final LinearLayout group = new LinearLayout(context);
		group.setOrientation(LinearLayout.VERTICAL);

		group.addView(Widgets.section(context, "Selected object"));
		objectValue = Theme.text(context, "—", 15, Theme.ACCENT, false);
		final LinearLayout objectRow = new LinearLayout(context);
		objectRow.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(objectRow, 18, 4, 18, 8);
		objectRow.addView(objectValue, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		group.addView(objectRow);

		group.addView(Widgets.section(context, "From"));
		yearField = numberField(group, "Year", value -> year = value);
		monthValue = pickerValue();
		group.addView(pickerRow("Month", "Month", monthValue, RtsPage::months,
				id -> { month = Integer.parseInt(id); setPickerValue(monthValue, id, months()); }));
		monthsField = numberField(group, "To the next (mths)", value ->
		{
			NativeBridge.send("rts.set", "rts_duration_months=" + Math.max(1, Math.min(60, value)));
			refresh();
		});

		group.addView(note("Artificial satellites and unnamed stars are excluded from calculation."));
		group.addView(Widgets.hairline(context));
		return group;
	}

	private android.widget.EditText numberField(LinearLayout into, String label, final IntAction commit)
	{
		final LinearLayout row = new LinearLayout(context());
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		Theme.padding(row, 16, 8, 16, 4);
		row.addView(Theme.text(context(), label, 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final android.widget.EditText field = Widgets.field(context(), label, "", true);
		field.setGravity(Gravity.END);
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
		into.addView(row);
		return field;
	}

	private static JSONArray months()
	{
		final JSONArray list = new JSONArray();
		final String[] names = new DateFormatSymbols().getMonths();
		for (int i = 0; i < 12; ++i)
		{
			final JSONObject entry = new JSONObject();
			try
			{
				entry.put("id", String.valueOf(i + 1));
				entry.put("name", names[i]);
			}
			catch (org.json.JSONException e)
			{
			}
			list.put(entry);
		}
		return list;
	}

	private void show()
	{
		rows.clear();
		rows.addAll(rowsOf(state, "rows"));
		expanded = -1;
		if (adapter != null)
			adapter.notifyDataSetChanged();
		showResults(rows.size());

		if (!started)
		{
			year = state.optInt("year", 2000);
			month = Math.max(1, Math.min(12, state.optInt("month", 1)));
			started = true;
			if (yearField != null)
				yearField.setText(T.t(String.valueOf(year)));
		}
		if (monthValue != null)
			setPickerValue(monthValue, String.valueOf(month), months());
		if (monthsField != null && !monthsField.hasFocus())
			monthsField.setText(T.t(String.valueOf(state.optInt("months", 1))));

		final String object = state.optString("object");
		if (objectValue != null)
			objectValue.setText(object.isEmpty() ? T.t("Nothing selected") : object);

		if (object.isEmpty())
			host.setSummary("Select something in the sky first — this page works on the selection.");
		else if (rows.isEmpty())
			host.setSummary("Nothing computed yet for " + object + ".");
		else
			host.setSummary(rows.size() + (rows.size() == 1 ? " day" : " days")
					+ " for " + state.optString("computedFor", object));
	}

	private void toggle(int position)
	{
		expanded = (expanded == position) ? -1 : position;
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}

	private final class Row extends LinearLayout
	{
		private final TextView date, name, transit, riseSet;
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
			date = Theme.text(context, "", 14, Theme.TEXT, true);
			name = Theme.text(context, "", 10, Theme.TEXT_DIM, false);
			name.setSingleLine(true);
			name.setEllipsize(android.text.TextUtils.TruncateAt.END);
			titles.addView(date);
			titles.addView(name);

			head.addView(titles, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

			final LinearLayout numbers = new LinearLayout(context);
			numbers.setOrientation(VERTICAL);
			transit = Theme.text(context, "", 14, Theme.ACCENT, true);
			transit.setGravity(Gravity.END);
			riseSet = Theme.text(context, "", 10, Theme.TEXT_DIM, true);
			riseSet.setGravity(Gravity.END);
			numbers.addView(transit);
			numbers.addView(riseSet);
			final LayoutParams numberParams = new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			numberParams.leftMargin = Theme.dp(10);
			head.addView(numbers, numberParams);
			addView(head);

			detail = new LinearLayout(context);
			detail.setOrientation(VERTICAL);
			detail.setBackgroundColor(Theme.FILL_FAINT);
			Theme.padding(detail, 0, 4, 0, 10);
			addView(detail);
		}

		void bind(final JSONObject entry, final int position)
		{
			date.setText(T.t(entry.optString("date")));
			name.setText(T.t(entry.optString("name")));
			transit.setText(T.t(time(entry.optString("transit"))));
			riseSet.setText(T.t(time(entry.optString("rise")) + " – " + time(entry.optString("set"))));

			final boolean open = position == expanded;
			setBackground(Theme.pressable(Theme.box(open ? Theme.FILL_SOFT : 0, 0), 0));
			setOnClickListener(v -> toggle(position));

			detail.setVisibility(open ? VISIBLE : GONE);
			detail.removeAllViews();
			if (!open)
				return;

			final Context context = getContext();
			detail.addView(Widgets.valueRow(context, "Rise", entry.optString("rise")));
			detail.addView(Widgets.valueRow(context, "Transit", entry.optString("transit")));
			detail.addView(Widgets.valueRow(context, "Set", entry.optString("set")));
			detail.addView(Widgets.valueRow(context, "Altitude", entry.optString("alt")));
			detail.addView(Widgets.valueRow(context, "Mag.", entry.optString("mag")));
			detail.addView(Widgets.valueRow(context, "Solar elongation", entry.optString("elongSun")));
			detail.addView(Widgets.valueRow(context, "Lunar elongation", entry.optString("elongMoon")));

			final LinearLayout actions = new LinearLayout(context);
			actions.setOrientation(HORIZONTAL);
			Theme.padding(actions, 14, 8, 14, 0);
			addAction(actions, entry, position, 0, "Rise", "riseJD");
			addAction(actions, entry, position, 1, "Transit", "transitJD");
			addAction(actions, entry, position, 2, "Set", "setJD");
			if (actions.getChildCount() > 0)
				detail.addView(actions);
		}

		private void addAction(LinearLayout into, JSONObject entry, final int position,
		                       final int which, String label, String key)
		{
			if (entry.optDouble(key, 0.) == 0.)
				return;
			final TextView button = Widgets.primaryButton(getContext(), label, v ->
			{
				NativeBridge.send("rts.goto", position + "," + which);
				host.overlay().close(host.sheet());
			});
			final LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
			params.setMargins(Theme.dp(4), 0, Theme.dp(4), 0);
			into.addView(button, params);
		}

		private String time(String stamp)
		{
			return stamp.length() >= 16 ? stamp.substring(11) : stamp;
		}
	}
}
