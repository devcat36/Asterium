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
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PhenomenaPage extends AstroCalcPage
{
	private static final String GROUP_LATEST_SELECTED = "-1";

	private JSONObject state = new JSONObject();
	private final List<JSONObject> rows = new ArrayList<>();
	private BaseAdapter adapter;
	private int expanded = -1;

	private String from = "";

	private TextView bodyValue, groupValue, fromValue, selectedNote, separationValue;
	private SeekBar separationBar;
	private View durationRow, oppositionRow, perihelionRow, quadratureRow;
	private TextView durationField;

	private final android.widget.CompoundButton.OnCheckedChangeListener oppositionListener =
			(button, checked) -> set("flag_phenomena_opposition", checked ? "true" : "false");
	private final android.widget.CompoundButton.OnCheckedChangeListener perihelionListener =
			(button, checked) -> set("flag_phenomena_perihelion", checked ? "true" : "false");
	private final android.widget.CompoundButton.OnCheckedChangeListener quadratureListener =
			(button, checked) -> set("flag_phenomena_quadratures", checked ? "true" : "false");

	@Override
	View build()
	{
		final Context context = context();
		host.addButton("Cleanup", false, () -> { NativeBridge.send("phenomena.clear"); refresh(); });
		host.addButton("Calculate", true, this::calculate);

		adapter = new BaseAdapter()
		{
			@Override public int getCount() { return rows.size(); }
			@Override public Object getItem(int position) { return rows.get(position); }
			@Override public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View recycled, ViewGroup parent)
			{
				final PhenomenonRow row = recycled instanceof PhenomenonRow
						? (PhenomenonRow) recycled : new PhenomenonRow(parent.getContext());
				row.bind(rows.get(position), position, position == expanded, () -> toggle(position));
				return row;
			}
		};

		return resultsList(controls(), adapter);
	}

	@Override
	void onShow()
	{
		refresh();
	}

	private void toggle(int position)
	{
		expanded = (expanded == position) ? -1 : position;
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}

	private void refresh()
	{
		NativeBridge.request("phenomena", "", payload -> { state = payload; show(); });
	}

	private void set(String key, String value)
	{
		NativeBridge.send("phenomena.set", key + "=" + value);
		refresh();
	}

	private void calculate()
	{
		host.setSummary("Calculating…");
		scrollToNextResult = true;
		NativeBridge.request("phenomena.generate", from, payload -> { state = payload; show(); });
	}

	private View controls()
	{
		final Context context = context();
		final LinearLayout group = new LinearLayout(context);
		group.setOrientation(LinearLayout.VERTICAL);

		group.addView(Widgets.section(context, "Between objects"));
		bodyValue = pickerValue();
		group.addView(pickerRow("Object", "Object", bodyValue,
				() -> state.optJSONArray("bodies"), id -> set("phenomena_celestial_body", id)));
		groupValue = pickerValue();
		group.addView(pickerRow("And", "Second object", groupValue,
				() -> state.optJSONArray("groups"), id -> set("phenomena_celestial_group", id)));

		selectedNote = Widgets.note(context, "");
		selectedNote.setVisibility(View.GONE);
		group.addView(selectedNote);

		group.addView(separationRow());

		group.addView(Widgets.section(context, "Time span"));
		fromValue = pickerValue();
		group.addView(Widgets.pickerRow(context, "From", fromValue, v -> pickFrom()));
		durationRow = numberRow("To the next, months", 1,
				value -> set("phenomena_duration_months", String.valueOf(Math.max(1, Math.min(60, value)))));
		group.addView(durationRow);

		final View field = ((LinearLayout) durationRow).getChildAt(1);
		if (field instanceof TextView)
			durationField = (TextView) field;

		group.addView(Widgets.section(context, "Also find"));
		oppositionRow = switchRow("Oppositions", null, false, oppositionListener);
		group.addView(oppositionRow);
		perihelionRow = switchRow("Perihelion and aphelion", null, false, perihelionListener);
		group.addView(perihelionRow);
		quadratureRow = switchRow("Elongations and quadratures",
				"Greatest elongation for an inner planet, quadrature for an outer one",
				false, quadratureListener);
		group.addView(quadratureRow);
		group.addView(Widgets.hairline(context));
		return group;
	}

	private View separationRow()
	{
		final Context context = context();
		final LinearLayout box = new LinearLayout(context);
		box.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(box, 16, 10, 16, 6);

		final LinearLayout head = new LinearLayout(context);
		head.setOrientation(LinearLayout.HORIZONTAL);

		head.addView(Theme.text(context, "Maximum allowed separation", 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		separationValue = Theme.text(context, "", 14, Theme.ACCENT, true);
		head.addView(separationValue);
		box.addView(head);

		separationBar = Widgets.slider(context, 200, 10);
		separationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			public void onProgressChanged(SeekBar seek, int progress, boolean fromUser)
			{
				separationValue.setText(T.t(degreesMinutesSeconds(progress / 10.)));
			}
			public void onStartTrackingTouch(SeekBar seek) {}
			public void onStopTrackingTouch(SeekBar seek)
			{
				set("phenomena_angular_separation",
						String.format(Locale.US, "%.5f", seek.getProgress() / 10.));
			}
		});
		box.addView(separationBar);
		return box;
	}

	private static String degreesMinutesSeconds(double degrees)
	{
		final int d = (int) Math.floor(degrees);
		final double restMinutes = (degrees - d) * 60.;
		final int m = (int) Math.floor(restMinutes);
		final double s = (restMinutes - m) * 60.;
		return String.format(Locale.US, "+%d° %d' %.2f\"", d, m, s);
	}

	private void pickFrom()
	{
		final java.util.Calendar at = java.util.Calendar.getInstance();
		try
		{
			at.set(Integer.parseInt(from.substring(0, 4)), Integer.parseInt(from.substring(5, 7)) - 1, 1);
		}
		catch (RuntimeException e)
		{
		}
		new android.app.DatePickerDialog(context(), (view, year, month, day) ->
		{
			from = String.format(Locale.US, "%04d-%02d", year, month + 1);
			if (fromValue != null)
				fromValue.setText(T.t(from));
		}, at.get(java.util.Calendar.YEAR), at.get(java.util.Calendar.MONTH), 1).show();
	}

	private void show()
	{
		rows.clear();
		rows.addAll(rowsOf(state, "rows"));
		expanded = -1;
		if (adapter != null)
			adapter.notifyDataSetChanged();
		showResults(rows.size());

		if (from.isEmpty())
			from = trimToMonth(state.optString("from"));

		setPickerValue(bodyValue, state.optString("body"), state.optJSONArray("bodies"));
		final String groupId = state.optString("group", "1");
		setPickerValue(groupValue, groupId, state.optJSONArray("groups"));
		if (fromValue != null)
			fromValue.setText(T.t(from));

		if (selectedNote != null)
		{
			final boolean latest = GROUP_LATEST_SELECTED.equals(groupId);
			final String selected = state.optString("selected");
			selectedNote.setVisibility(latest ? View.VISIBLE : View.GONE);
			selectedNote.setText(T.t(selected.isEmpty()
					? "Nothing is selected. Pick an object in the sky first."
					: "Currently selected: " + selected));
		}

		if (separationBar != null)
		{
			final double degrees = state.optDouble("separation", 1.0);
			separationBar.setProgress((int) Math.round(degrees * 10.));
			separationValue.setText(T.t(degreesMinutesSeconds(degrees)));
		}
		if (durationField != null && !durationField.hasFocus())
			durationField.setText(T.t(String.valueOf(state.optInt("months", 1))));

		applySwitch(oppositionRow, state.optBoolean("opposition", false), oppositionListener);
		applySwitch(perihelionRow, state.optBoolean("perihelion", false), perihelionListener);
		applySwitch(quadratureRow, state.optBoolean("quadratures", false), quadratureListener);

		host.setSummary(rows.isEmpty()
				? "Nothing computed yet."
				: rows.size() + (rows.size() == 1 ? " phenomenon" : " phenomena"));
	}

	private static String trimToMonth(String text)
	{
		return text.length() >= 7 ? text.substring(0, 7) : text;
	}

	private static void applySwitch(View row, boolean checked,
	                                android.widget.CompoundButton.OnCheckedChangeListener listener)
	{
		Widgets.restate(row == null ? null : Widgets.switchIn(row), checked, listener);
	}

	private final class PhenomenonRow extends LinearLayout
	{
		private final TextView type, pair, when, separation;
		private final LinearLayout detail;

		PhenomenonRow(Context context)
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
			type = Theme.text(context, "", 14, Theme.TEXT, false);
			type.setSingleLine(true);
			type.setEllipsize(android.text.TextUtils.TruncateAt.END);
			pair = Theme.text(context, "", 10, Theme.TEXT_DIM, false);
			pair.setSingleLine(true);
			pair.setEllipsize(android.text.TextUtils.TruncateAt.END);
			titles.addView(type);
			titles.addView(pair);

			head.addView(titles, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

			final LinearLayout numbers = new LinearLayout(context);
			numbers.setOrientation(VERTICAL);
			when = Theme.text(context, "", 13, Theme.ACCENT, true);
			when.setGravity(Gravity.END);
			separation = Theme.text(context, "", 10, Theme.TEXT_DIM, true);
			separation.setGravity(Gravity.END);
			numbers.addView(when);
			numbers.addView(separation);
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

		void bind(final JSONObject entry, final int position, boolean open, final Runnable click)
		{
			final Context context = getContext();
			type.setText(T.t(entry.optString("type")));
			pair.setText(T.t(entry.optString("obj1") + "  ·  " + entry.optString("obj2")));
			final String at = entry.optString("when");

			when.setText(T.t(at.length() >= 10 ? at.substring(0, 10) : at));
			separation.setText(T.t(entry.optString("sep")));
			setBackground(Theme.pressable(Theme.box(open ? Theme.FILL_SOFT : 0, 0), 0));
			setOnClickListener(v -> click.run());

			detail.setVisibility(open ? VISIBLE : GONE);
			detail.removeAllViews();
			if (!open)
				return;

			detail.addView(Widgets.valueRow(context, "Date and time", at));
			detail.addView(Widgets.valueRow(context, "Object 1", entry.optString("obj1")));
			detail.addView(Widgets.valueRow(context, "Mag. 1", entry.optString("mag1")));
			detail.addView(Widgets.valueRow(context, "Object 2", entry.optString("obj2")));
			detail.addView(Widgets.valueRow(context, "Mag. 2", entry.optString("mag2")));
			detail.addView(Widgets.valueRow(context, "Separation", entry.optString("sep")));
			detail.addView(Widgets.valueRow(context, "Elevation", entry.optString("elev")));

			detail.addView(Widgets.valueRow(context, entry.optString("elongLabel"), entry.optString("elong")));
			detail.addView(Widgets.valueRow(context, entry.optString("angDistLabel"), entry.optString("angDist")));

			final TextView button = Widgets.primaryButton(context, "Go to this moment",
					v ->
					{
						NativeBridge.send("phenomena.goto", String.valueOf(position));
						host.overlay().close(host.sheet());
					});
			final LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(Theme.dp(18), Theme.dp(8), Theme.dp(18), 0);
			detail.addView(button, params);
		}
	}
}
