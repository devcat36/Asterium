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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class SkyCultureSheet extends Sheet
{
	private static final class Item
	{
		final String id, name, region;
		final int begin, end;

		Item(String id, String name, String region, int begin, int end)
		{
			this.id = id;
			this.name = name;
			this.region = region;
			this.begin = begin;
			this.end = end;
		}

		boolean isHeading()
		{
			return id == null;
		}
	}

	private final List<Item> all = new ArrayList<>();
	private final List<Item> shown = new ArrayList<>();
	private final BaseAdapter adapter;
	private final ListView list;
	private final EditText search;
	private final EditText minYear;
	private final EditText maxYear;
	private final LinearLayout yearRow;
	private boolean applyTime = false;
	private String current = "";

	SkyCultureSheet(Context context, Overlay overlay, Runnable changed)
	{
		super(context, overlay, "skyculture", "Sky Culture");

		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);

		search = field(context, "Enter Culture...", false);
		final LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(52));
		searchParams.leftMargin = searchParams.rightMargin = Theme.dp(16);
		searchParams.topMargin = searchParams.bottomMargin = Theme.dp(12);
		column.addView(search, searchParams);

		yearRow = new LinearLayout(context);
		yearRow.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(yearRow, 16, 0, 16, 10);
		minYear = field(context, "Minimum year", true);
		maxYear = field(context, "Maximum year", true);
		maxYear.setText(T.t(String.valueOf(Calendar.getInstance().get(Calendar.YEAR))));
		yearRow.addView(labelled(context, "Minimum year", minYear),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		yearRow.addView(new View(context), new LinearLayout.LayoutParams(Theme.dp(12), 1));
		yearRow.addView(labelled(context, "Maximum year", maxYear),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		yearRow.setVisibility(View.GONE);

		column.addView(Widgets.switchRow(context, "Apply time interval limits to list", null, false,
				(button, checked) ->
				{
					applyTime = checked;
					yearRow.setVisibility(checked ? View.VISIBLE : View.GONE);
					apply();
				}));
		column.addView(yearRow);

		adapter = new BaseAdapter()
		{
			@Override public int getCount() { return shown.size(); }
			@Override public Object getItem(int position) { return shown.get(position); }
			@Override public long getItemId(int position) { return position; }
			@Override public int getViewTypeCount() { return 2; }
			@Override public boolean areAllItemsEnabled() { return false; }

			@Override
			public int getItemViewType(int position)
			{
				return shown.get(position).isHeading() ? 0 : 1;
			}

			@Override
			public boolean isEnabled(int position)
			{
				return !shown.get(position).isHeading();
			}

			@Override
			public View getView(int position, View recycled, ViewGroup parent)
			{
				final Item item = shown.get(position);
				if (item.isHeading())
				{
					final TextView heading = recycled instanceof TextView
							? (TextView) recycled : Widgets.section(parent.getContext(), "");
					heading.setText(T.t(item.name.toUpperCase()));
					return heading;
				}
				final View row = recycled instanceof LinearLayout
						? recycled : newRow(parent.getContext());
				bind(row, item);
				return row;
			}
		};

		list = new ListView(context);
		list.setAdapter(adapter);
		list.setDivider(null);
		list.setBackgroundColor(Color.TRANSPARENT);
		list.setOnItemClickListener((parent, view, position, id) ->
		{
			if (position < 0 || position >= shown.size())
				return;
			final Item item = shown.get(position);
			if (item.isHeading())
				return;
			NativeBridge.send("skyculture.set", item.id);
			if (changed != null)
				changed.run();
			overlay.close(this);
		});
		column.addView(list, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
		setBodyView(column);

		final TextWatcher refilter = new TextWatcher()
		{
			public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
			public void onTextChanged(CharSequence s, int a, int b, int c) {}
			public void afterTextChanged(Editable s) { apply(); }
		};
		search.addTextChangedListener(refilter);
		minYear.addTextChangedListener(refilter);
		maxYear.addTextChangedListener(refilter);

		NativeBridge.request("skycultures", "", this::setCultures);
	}

	private void setCultures(JSONObject payload)
	{
		current = payload.optString("current");
		all.clear();
		final JSONArray cultures = payload.optJSONArray("cultures");
		int earliest = Calendar.getInstance().get(Calendar.YEAR);
		for (int i = 0; cultures != null && i < cultures.length(); ++i)
		{
			final JSONObject culture = cultures.optJSONObject(i);
			if (culture == null)
				continue;
			final int begin = culture.optInt("begin");
			all.add(new Item(culture.optString("id"), culture.optString("name"),
			                 culture.optString("region"), begin, culture.optInt("end")));
			earliest = Math.min(earliest, begin);
		}
		if (minYear.getText().length() == 0)
			minYear.setText(T.t(String.valueOf(earliest)));
		apply();
		for (int i = 0; i < shown.size(); ++i)
		{
			if (current.equals(shown.get(i).id))
			{
				list.setSelection(Math.max(0, i - 2));
				break;
			}
		}
	}

	private void apply()
	{
		final String needle = fold(search.getText().toString());
		final int from = year(minYear, Integer.MIN_VALUE);
		final int to = year(maxYear, Integer.MAX_VALUE);
		shown.clear();
		String region = null;
		for (Item item : all)
		{
			if (!needle.isEmpty() && !fold(item.name).contains(needle))
				continue;
			if (applyTime && (item.begin > to || item.end < from))
				continue;
			if (!item.region.equals(region))
			{
				region = item.region;
				shown.add(new Item(null, region, region, 0, 0));
			}
			shown.add(item);
		}
		adapter.notifyDataSetChanged();
	}

	private static String fold(String text)
	{
		return Normalizer.normalize(text, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "").toLowerCase(Locale.US).trim();
	}

	private static int year(EditText field, int fallback)
	{
		try
		{
			return Integer.parseInt(field.getText().toString().trim().replace('−', '-'));
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}

	static String period(int begin, int end)
	{
		if (begin == 0 && end == 0)
			return null;
		return era(begin) + " – " + (end == 9146 ? "∞" : era(end));
	}

	private static String era(int value)
	{
		return value < 0 ? (-value) + " BCE" : value + " CE";
	}

	private static View newRow(Context context)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.VERTICAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 20, 8, 20, 8);
		row.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 0), 0));
		row.addView(Theme.text(context, "", 15, Theme.TEXT_CHIP, false));
		row.addView(Theme.text(context, "", 11, Theme.TEXT_FAINT, false));
		return row;
	}

	private void bind(View view, Item item)
	{
		final LinearLayout row = (LinearLayout) view;
		final TextView name = (TextView) row.getChildAt(0);
		final TextView sub = (TextView) row.getChildAt(1);
		final boolean chosen = item.id.equals(current);
		name.setText(T.t(chosen ? "✓  " + item.name : item.name));
		name.setTextColor(chosen ? Theme.ACCENT : Theme.TEXT_CHIP);
		final String period = period(item.begin, item.end);
		sub.setText(T.t(period == null ? "" : period));
		sub.setVisibility(period == null ? View.GONE : View.VISIBLE);
	}

	private static EditText field(Context context, String hint, boolean numeric)
	{
		final EditText field = new EditText(context);
		field.setBackground(Theme.box(0xFF1E1E1F, 10, 0xFF000000));
		field.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, numeric ? 14 : 16);
		field.setTextColor(0xFFFFFFFF);
		field.setHintTextColor(Theme.TEXT_FAINT);
		field.setHint(T.t(hint));
		field.setTypeface(Theme.sans());
		field.setSingleLine(true);
		field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		if (numeric)
		{
			field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
					| android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
			field.setHint(T.t(""));
		}
		Theme.padding(field, 14, 0, 14, 0);
		return field;
	}

	private static View labelled(Context context, String label, EditText field)
	{
		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);
		column.addView(Theme.text(context, label, 11, Theme.TEXT_DIM, false));
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(44));
		params.topMargin = Theme.dp(4);
		column.addView(field, params);
		return column;
	}
}
