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
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

final class Widgets
{
	private Widgets() {}

	static ImageView starButton(Context context, boolean saved)
	{
		final ImageView star = new ImageView(context);
		star.setScaleType(ImageView.ScaleType.FIT_CENTER);
		Theme.padding(star, 12, 12, 12, 12);
		star.setBackground(Theme.pressable(Theme.box(0, 20), 20));
		setStarred(star, saved);
		return star;
	}

	static void setStarred(ImageView star, boolean saved)
	{
		star.setImageDrawable(Theme.icon(star.getContext(), "btObsList", saved));
		star.setAlpha(saved ? 1f : 0.72f);
		star.setContentDescription(T.t(saved ? "Remove from observing list"
		                                     : "Save to observing list"));
		star.setTag(saved);
	}

	static boolean starred(ImageView star)
	{
		return Boolean.TRUE.equals(star.getTag());
	}

	static View hairline(Context context)
	{
		final View rule = new View(context);
		rule.setBackgroundColor(Theme.HAIRLINE);
		rule.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, Theme.dp(1))));
		return rule;
	}

	static TextView section(Context context, String title)
	{
		final TextView view = Theme.text(context, title, 10, Theme.TEXT_FAINT, true);
		view.setAllCaps(true);
		view.setLetterSpacing(0.14f);
		Theme.padding(view, 16, 16, 16, 10);
		return view;
	}

	static TextView note(Context context, String text)
	{
		final TextView view = Theme.text(context, text, 12, Theme.TEXT_DIM, false);
		view.setLineSpacing(Theme.dp(3), 1f);
		Theme.padding(view, 16, 4, 16, 14);
		return view;
	}

	private static Drawable switchTrack()
	{
		final GradientDrawable on = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT,
				new int[] { Theme.ACCENT_DARK, Theme.ACCENT_LIGHT });
		on.setCornerRadius(Theme.dp(16));
		on.setSize(Theme.dp(52), Theme.dp(32));
		on.setStroke(Math.max(1, Theme.dp(1)), 0xB3000000);

		final GradientDrawable off = new GradientDrawable();
		off.setColor(Theme.TRACK_OFF);
		off.setCornerRadius(Theme.dp(16));
		off.setSize(Theme.dp(52), Theme.dp(32));
		off.setStroke(Math.max(1, Theme.dp(1)), 0xB3000000);

		final StateListDrawable states = new StateListDrawable();
		states.addState(new int[] { android.R.attr.state_checked }, on);
		states.addState(new int[] {}, off);
		return states;
	}

	private static Drawable switchThumb()
	{
		final GradientDrawable on = new GradientDrawable();
		on.setShape(GradientDrawable.OVAL);
		on.setColor(Theme.KNOB_ON);
		on.setSize(Theme.dp(26), Theme.dp(26));

		final GradientDrawable off = new GradientDrawable();
		off.setShape(GradientDrawable.OVAL);
		off.setColor(Theme.KNOB_OFF);
		off.setSize(Theme.dp(26), Theme.dp(26));

		final StateListDrawable states = new StateListDrawable();
		states.addState(new int[] { android.R.attr.state_checked }, on);
		states.addState(new int[] {}, off);
		return new InsetDrawable(states, Theme.dp(3));
	}

	@SuppressWarnings("deprecation")
	static android.widget.Switch toggle(Context context, boolean checked)
	{
		final android.widget.Switch view = new android.widget.Switch(context);
		view.setTrackDrawable(switchTrack());
		view.setThumbDrawable(switchThumb());
		view.setChecked(checked);
		view.setPadding(0, 0, 0, 0);
		view.setSwitchMinWidth(Theme.dp(52));
		view.setBackground(null);
		return view;
	}

	static View switchRow(Context context, String label, String sub, boolean checked,
	                      final CompoundButton.OnCheckedChangeListener listener)
	{
		return switchRow(context, null, label, sub, checked, listener);
	}

	static View switchRow(Context context, View leading, String label, String sub, boolean checked,
	                      final CompoundButton.OnCheckedChangeListener listener)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 16, 8, 16, 8);
		if (leading != null)
			row.addView(leading);

		final LinearLayout titles = new LinearLayout(context);
		titles.setOrientation(LinearLayout.VERTICAL);
		titles.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false));
		if (sub != null && !sub.isEmpty())
		{
			final TextView subtitle = Theme.text(context, sub, 11, Theme.TEXT_DIM, false);
			subtitle.setLineSpacing(Theme.dp(2), 1f);
			titles.addView(subtitle);
		}
		row.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final android.widget.Switch control = toggle(context, checked);
		row.addView(control);
		control.setOnCheckedChangeListener(listener);
		row.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 0), 0));
		row.setOnClickListener(v -> control.toggle());
		row.setContentDescription(T.t(label));
		return row;
	}

	static void restate(android.widget.Switch control, boolean checked,
	                    CompoundButton.OnCheckedChangeListener listener)
	{
		if (control == null)
			return;
		control.setOnCheckedChangeListener(null);
		control.setChecked(checked);

		control.jumpDrawablesToCurrentState();
		control.setOnCheckedChangeListener(listener);
	}

	static android.widget.Switch switchIn(View row)
	{
		if (!(row instanceof LinearLayout))
			return null;
		final LinearLayout group = (LinearLayout) row;
		for (int i = 0; i < group.getChildCount(); ++i)
		{
			if (group.getChildAt(i) instanceof android.widget.Switch)
				return (android.widget.Switch) group.getChildAt(i);
		}
		return null;
	}

	static SeekBar slider(Context context, int max, int value)
	{
		final GradientDrawable track = new GradientDrawable();
		track.setColor(Theme.SLIDER_TRACK);
		track.setCornerRadius(Theme.dp(2));

		final GradientDrawable fill = new GradientDrawable();
		fill.setColor(Theme.ACCENT_DARK);
		fill.setCornerRadius(Theme.dp(2));

		final LayerDrawable layers = new LayerDrawable(new Drawable[] {
				track, new ClipDrawable(fill, Gravity.START, ClipDrawable.HORIZONTAL) });
		layers.setId(0, android.R.id.background);
		layers.setId(1, android.R.id.progress);

		final GradientDrawable thumb = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { Theme.THUMB_TOP, Theme.THUMB_BOT });
		thumb.setCornerRadius(Theme.dp(6));
		thumb.setSize(Theme.dp(32), Theme.dp(32));
		thumb.setStroke(Math.max(1, Theme.dp(1)), 0xFF000000);

		final SeekBar bar = new SeekBar(context);
		bar.setProgressDrawable(layers);
		bar.setThumb(thumb);
		bar.setThumbOffset(Theme.dp(16));
		bar.setSplitTrack(false);
		bar.setMax(max);
		bar.setProgress(value);
		bar.setPadding(Theme.dp(16), 0, Theme.dp(16), 0);
		bar.setMinimumHeight(Theme.dp(32));
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
			bar.setMaxHeight(Theme.dp(4));
		return bar;
	}

	static TextView tabChip(Context context, String label, boolean active, View.OnClickListener click)
	{
		final TextView chip = Theme.text(context, label, 13,
		                                 active ? Theme.ACCENT : Theme.TEXT_DIM, false);
		chip.setGravity(Gravity.CENTER);
		chip.setSingleLine(true);
		Theme.padding(chip, 14, 0, 14, 0);
		chip.setBackground(Theme.pressable(active
				? Theme.accentSoft(20)
				: Theme.box(Theme.HAIRLINE, 20, 0x0FFFFFFF), 20));
		chip.setOnClickListener(click);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, Theme.dp(Theme.CHIP));
		params.rightMargin = Theme.dp(6);
		chip.setLayoutParams(params);
		return chip;
	}

	static TextView filterChip(Context context, String label, boolean active, View.OnClickListener click)
	{
		final TextView chip = Theme.text(context, label, 12, Theme.TEXT_CHIP, false);
		chip.setGravity(Gravity.CENTER);
		chip.setSingleLine(true);
		Theme.padding(chip, 12, 0, 12, 0);
		chipStyle(chip, active);
		chip.setOnClickListener(click);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, Theme.dp(34));
		params.rightMargin = Theme.dp(8);
		chip.setLayoutParams(params);
		return chip;
	}

	static android.widget.EditText field(Context context, String name, String hint, boolean numeric)
	{
		final android.widget.EditText view = new android.widget.EditText(context);
		view.setBackground(Theme.box(0xFF1E1E1F, 10, 0xFF000000));
		view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
		view.setTextColor(0xFFFFFFFF);
		view.setHintTextColor(Theme.TEXT_FAINT);
		view.setHint(T.t(hint));
		view.setTypeface(Theme.sans());
		view.setSingleLine(true);
		view.setMinimumHeight(Theme.dp(52));
		view.setInputType(numeric
				? android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
				  | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
				: android.text.InputType.TYPE_CLASS_TEXT);
		view.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
		view.setContentDescription(T.t(name));
		Theme.padding(view, 12, 0, 12, 0);
		return view;
	}

	static double number(android.widget.EditText field)
	{
		try
		{
			return Double.parseDouble(field.getText().toString().trim()
					.replace('−', '-').replace(',', '.'));
		}
		catch (NumberFormatException e)
		{
			return Double.NaN;
		}
	}

	static void chipStyle(TextView chip, boolean active)
	{
		chip.setTextColor(active ? 0xFF1F1F1F : Theme.TEXT_CHIP);
		chip.setBackground(Theme.pressable(active
				? Theme.accentSolid(8)
				: Theme.box(0x0FFFFFFF, 8), 8));
	}

	static View colorDot(Context context, GradientDrawable fill, View.OnClickListener click)
	{
		final View view = new View(context);
		view.setBackground(Theme.pressable(fill, 4));
		view.setOnClickListener(click);
		final LinearLayout.LayoutParams params =
				new LinearLayout.LayoutParams(Theme.dp(18), Theme.dp(18));
		params.rightMargin = Theme.dp(12);
		view.setLayoutParams(params);
		return view;
	}

	static TextView toggleChip(Context context, String label, boolean on, View.OnClickListener click)
	{
		final TextView chip = Theme.text(context, label, 12, Theme.TEXT_CHIP, false);
		chip.setGravity(Gravity.CENTER);
		chip.setMaxLines(2);
		chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
		Theme.padding(chip, 8, 8, 8, 8);
		chip.setMinimumHeight(Theme.dp(Theme.TOUCH));
		chip.setOnClickListener(click);
		setChipOn(chip, on);
		return chip;
	}

	static void setChipOn(TextView chip, boolean on)
	{
		chip.setBackground(Theme.pressable(
				on ? Theme.accentSoft(10) : Theme.box(Theme.FILL_FAINT, 10, Theme.HAIRLINE), 10));
		chip.setTextColor(on ? Theme.ACCENT : Theme.TEXT_MUTED);
	}

	static GridLayout chipGrid(Context context, int columns)
	{
		final GridLayout grid = new GridLayout(context);
		grid.setColumnCount(columns);
		Theme.padding(grid, 12, 0, 12, 8);
		grid.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		return grid;
	}

	static View gap(Context context, int heightDp)
	{
		final View space = new View(context);
		space.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(heightDp)));
		return space;
	}

	static GridLayout.LayoutParams chipCell()
	{
		final GridLayout.LayoutParams cell = new GridLayout.LayoutParams();
		cell.width = 0;
		cell.height = GridLayout.LayoutParams.WRAP_CONTENT;
		cell.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f);
		cell.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL);
		cell.setMargins(Theme.dp(4), Theme.dp(4), Theme.dp(4), Theme.dp(4));
		return cell;
	}

	static TextView primaryButton(Context context, String label, View.OnClickListener click)
	{
		final TextView button = Theme.text(context, label, 15, 0xFF1F1F1F, false);
		button.setGravity(Gravity.CENTER);
		button.setBackground(Theme.pressable(Theme.accentSolid(10), 10));
		button.setOnClickListener(click);
		button.setMinimumHeight(Theme.dp(48));
		return button;
	}

	static TextView secondaryButton(Context context, String label, View.OnClickListener click)
	{
		final TextView button = Theme.text(context, label, 14, Theme.TEXT_CHIP, false);
		button.setGravity(Gravity.CENTER);
		button.setBackground(Theme.pressable(Theme.box(0x12FFFFFF, 10), 10));
		button.setOnClickListener(click);
		button.setMinimumHeight(Theme.dp(48));
		return button;
	}

	static View valueRow(Context context, String key, String value)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(40));
		Theme.padding(row, 18, 6, 18, 6);

		final TextView keyView = Theme.text(context, key, 13, Theme.TEXT_DIM, false);
		row.addView(keyView);

		final TextView valueView = Theme.text(context, value, 13, Theme.TEXT, true);
		valueView.setGravity(Gravity.END);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		params.leftMargin = Theme.dp(14);
		row.addView(valueView, params);
		return row;
	}

	static View navigationRow(Context context, String label, String sub, View.OnClickListener click)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 16, 8, 16, 8);
		row.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 0), 0));
		row.setOnClickListener(click);

		final LinearLayout titles = new LinearLayout(context);
		titles.setOrientation(LinearLayout.VERTICAL);
		titles.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false));
		final TextView subtitle = Theme.text(context, sub == null ? "" : sub, 11, Theme.TEXT_DIM, false);
		subtitle.setVisibility(sub == null || sub.isEmpty() ? View.GONE : View.VISIBLE);
		titles.addView(subtitle);
		row.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final TextView chevron = Theme.text(context, "›", 20, Theme.ACCENT_LINK, false);
		row.addView(chevron);
		row.setTag(subtitle);
		return row;
	}

	static View pickerRow(Context context, String label, TextView value, View.OnClickListener click)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 16, 8, 16, 8);
		row.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 0), 0));
		row.setOnClickListener(click);
		row.setContentDescription(T.t(label));
		row.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false));
		value.setGravity(Gravity.END);
		value.setSingleLine(true);
		value.setEllipsize(android.text.TextUtils.TruncateAt.START);
		final LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		valueParams.leftMargin = Theme.dp(12);
		row.addView(value, valueParams);
		final TextView chevron = Theme.text(context, "›", 20, Theme.ACCENT_LINK, false);
		Theme.padding(chevron, 10, 0, 0, 0);
		row.addView(chevron);
		return row;
	}

	static View statTile(Context context, String label, String value, boolean accent)
	{
		final LinearLayout tile = new LinearLayout(context);
		tile.setOrientation(LinearLayout.VERTICAL);
		tile.setBackground(Theme.box(0x59000000, 10));
		Theme.padding(tile, 12, 10, 12, 10);
		tile.addView(Theme.text(context, label, 10, Theme.TEXT_DIM, false));
		tile.addView(Theme.text(context, value, 15, accent ? Theme.ACCENT : Theme.TEXT, true));
		return tile;
	}
}
