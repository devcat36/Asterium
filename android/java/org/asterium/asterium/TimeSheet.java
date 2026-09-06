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
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Locale;

final class TimeSheet extends Sheet
{
	private static final int YEAR_MIN = -4000;
	private static final int YEAR_MAX = 9999;
	private static final double MJD_EPOCH = 2400000.5;

	private final NumberPicker year, month, day, hour, minute, second;
	private final EditText jdField, mjdField;
	private final TextView siderealText;
	private long editingUntil = 0;

	TimeSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "time", "Date and time");

		final Context dark = new android.view.ContextThemeWrapper(
				context, android.R.style.Theme_DeviceDefault);

		year   = wheel(dark, YEAR_MIN, YEAR_MAX, false, false);
		month  = wheel(dark, 1, 12, true, true);
		day    = wheel(dark, 1, 31, true, true);
		hour   = wheel(dark, 0, 23, true, true);
		minute = wheel(dark, 0, 59, true, true);
		second = wheel(dark, 0, 59, true, true);

		body.addView(panel(context, wheelRow(context, new NumberPicker[] { year, month, day },
		                                     new String[] { "YEAR", "MONTH", "DAY" })),
		             panelParams(Theme.dp(12)));
		body.addView(panel(context, wheelRow(context, new NumberPicker[] { hour, minute, second },
		                                     new String[] { "HOUR", "MIN", "SEC" })),
		             panelParams(Theme.dp(10)));

		final LinearLayout now = new LinearLayout(context);
		now.setOrientation(LinearLayout.HORIZONTAL);
		now.setGravity(Gravity.CENTER);
		now.setBackground(Theme.pressable(Theme.accentSolid(10), 10));
		now.setOnClickListener(v -> NativeBridge.send("time.now"));
		final ImageView nowGlyph = new ImageView(context);
		nowGlyph.setImageDrawable(Theme.icon(context, "btTimeNow", true));
		now.addView(nowGlyph, new LinearLayout.LayoutParams(Theme.dp(22), Theme.dp(22)));
		final TextView nowLabel = Theme.text(context, "Set to now", 15, 0xFF1F1F1F, false);
		final LinearLayout.LayoutParams nowLabelParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		nowLabelParams.leftMargin = Theme.dp(10);
		now.addView(nowLabel, nowLabelParams);
		final LinearLayout.LayoutParams nowParams =
				new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(52));
		nowParams.leftMargin = nowParams.rightMargin = Theme.dp(16);
		nowParams.topMargin = Theme.dp(16);
		body.addView(now, nowParams);

		body.addView(Widgets.section(context, "Julian day"));
		body.addView(Widgets.hairline(context));
		jdField = numberField(context, jd -> NativeBridge.send("time.jd", String.valueOf(jd)));
		mjdField = numberField(context, mjd -> NativeBridge.send("time.jd", String.valueOf(mjd + MJD_EPOCH)));
		body.addView(fieldRow(context, "JD", jdField));
		body.addView(Widgets.hairline(context));
		body.addView(fieldRow(context, "MJD", mjdField));
		body.addView(Widgets.hairline(context));

		siderealText = Theme.text(context, "", 14, Theme.TEXT, true);
		final LinearLayout sidereal = new LinearLayout(context);
		sidereal.setOrientation(LinearLayout.HORIZONTAL);
		sidereal.setGravity(Gravity.CENTER_VERTICAL);
		sidereal.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(sidereal, 16, 8, 16, 8);
		sidereal.addView(Theme.text(context, "Sidereal time", 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		sidereal.addView(siderealText);
		body.addView(sidereal);

		final JSONObject state = NativeBridge.lastState();
		if (state != null)
			onState(state);
	}

	private View panel(Context context, View content)
	{
		final LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setBackground(Theme.box(Theme.FILL_SOFT, 12, Theme.HAIRLINE));
		Theme.padding(panel, 8, 10, 8, 10);
		panel.addView(content);
		return panel;
	}

	private LinearLayout.LayoutParams panelParams(int topMargin)
	{
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = params.rightMargin = Theme.dp(16);
		params.topMargin = topMargin;
		return params;
	}

	private NumberPicker wheel(Context dark, final int min, int max, boolean wrap, final boolean pad)
	{
		final NumberPicker picker = new NumberPicker(dark);
		picker.setMinValue(0);
		picker.setMaxValue(max - min);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
		{
			picker.setTextSize(Theme.dp(22));
			picker.setTextColor(Theme.TEXT);
		}
		picker.setFormatter(value -> pad ? String.format(Locale.US, "%02d", value + min)
		                                 : String.valueOf(value + min));
		if (min != 0)
			picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		picker.setWrapSelectorWheel(wrap);
		picker.setOnValueChangedListener((p, was, is) -> apply());
		picker.setOnTouchListener((v, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
				v.getParent().requestDisallowInterceptTouchEvent(true);
			return false;
		});
		return picker;
	}

	private View wheelRow(Context context, NumberPicker[] pickers, String[] captions)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		for (int i = 0; i < pickers.length; ++i)
		{
			final LinearLayout column = new LinearLayout(context);
			column.setOrientation(LinearLayout.VERTICAL);
			column.setGravity(Gravity.CENTER_HORIZONTAL);
			column.addView(pickers[i], new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			final TextView caption = Theme.text(context, captions[i], 10, Theme.TEXT_FAINT, false);
			caption.setLetterSpacing(0.1f);
			column.addView(caption);
			row.addView(column, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		}
		return row;
	}

	private EditText numberField(Context context, final java.util.function.DoubleConsumer commit)
	{
		final EditText field = new EditText(context);
		field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
		                   | InputType.TYPE_NUMBER_FLAG_SIGNED);
		field.setImeOptions(EditorInfo.IME_ACTION_DONE);
		field.setSingleLine(true);
		field.setGravity(Gravity.END);
		field.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
		field.setTextColor(Theme.TEXT);
		field.setTypeface(Theme.mono());
		field.setBackground(Theme.box(Theme.FILL_SOFT, 8));
		Theme.padding(field, 10, 8, 10, 8);
		field.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId != EditorInfo.IME_ACTION_DONE)
				return false;
			try
			{
				commit.accept(Double.parseDouble(v.getText().toString().trim()));
			}
			catch (NumberFormatException ignored) { }
			editingUntil = 0;
			v.clearFocus();
			final android.view.inputmethod.InputMethodManager ime =
					(android.view.inputmethod.InputMethodManager)
					v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			if (ime != null)
				ime.hideSoftInputFromWindow(v.getWindowToken(), 0);
			return true;
		});
		return field;
	}

	private View fieldRow(Context context, String label, EditText field)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 16, 8, 16, 8);
		row.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		row.addView(field, new LinearLayout.LayoutParams(Theme.dp(190), LinearLayout.LayoutParams.WRAP_CONTENT));
		return row;
	}

	private static int daysInMonth(int y, int m)
	{
		switch (m)
		{
			case 2:  return (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) ? 29 : 28;
			case 4: case 6: case 9: case 11: return 30;
			default: return 31;
		}
	}

	private void apply()
	{
		editingUntil = android.os.SystemClock.uptimeMillis() + 1200;
		final int y = year.getValue() + YEAR_MIN;
		final int m = month.getValue() + 1;
		final int last = daysInMonth(y, m);
		if (day.getMaxValue() != last - 1)
			day.setMaxValue(last - 1);
		NativeBridge.send("time.set", y + "," + m + "," + (day.getValue() + 1) + ","
		                              + hour.getValue() + "," + minute.getValue() + "," + second.getValue());
	}

	@Override
	void onState(JSONObject state)
	{
		final double jd = state.optDouble("jd", 0.);
		siderealText.setText(T.t(state.optString("sidereal")));
		if (!jdField.hasFocus())
			jdField.setText(T.t(String.format(Locale.US, "%.5f", jd)));
		if (!mjdField.hasFocus())
			mjdField.setText(T.t(String.format(Locale.US, "%.5f", jd - MJD_EPOCH)));

		if (android.os.SystemClock.uptimeMillis() < editingUntil)
			return;
		final JSONObject wheels = state.optJSONObject("wheels");
		if (wheels == null)
			return;
		final int y = wheels.optInt("year");
		final int m = wheels.optInt("month");
		if (y < YEAR_MIN || y > YEAR_MAX)
			return;
		year.setValue(y - YEAR_MIN);
		month.setValue(m - 1);
		day.setMaxValue(daysInMonth(y, m) - 1);
		day.setValue(wheels.optInt("day") - 1);
		hour.setValue(wheels.optInt("hour"));
		minute.setValue(wheels.optInt("minute"));
		second.setValue(wheels.optInt("second"));
	}
}
