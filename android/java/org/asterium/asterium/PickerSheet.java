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

import java.util.ArrayList;
import java.util.List;

final class PickerSheet extends Sheet
{
	interface Choice
	{
		void chosen(String id);
	}

	private static final class Item
	{
		final String id, label;
		Item(String id, String label) { this.id = id; this.label = label; }
	}

	private final List<Item> all = new ArrayList<>();
	private final List<Item> shown = new ArrayList<>();
	private final String current;
	private final Choice choice;
	private final BaseAdapter adapter;
	private final ListView list;

	PickerSheet(Context context, Overlay overlay, String title, String current, Choice choice)
	{
		super(context, overlay, "picker", title);
		this.current = current == null ? "" : current;
		this.choice = choice;

		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);

		final EditText filter = new EditText(context);
		filter.setBackground(Theme.box(0xFF1E1E1F, 10, 0xFF000000));
		filter.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
		filter.setTextColor(0xFFFFFFFF);
		filter.setHintTextColor(Theme.TEXT_FAINT);
		filter.setHint(T.t("Filter"));
		filter.setTypeface(Theme.sans());
		filter.setSingleLine(true);
		filter.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		Theme.padding(filter, 14, 0, 14, 0);
		final LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(52));
		filterParams.leftMargin = filterParams.rightMargin = Theme.dp(16);
		filterParams.topMargin = filterParams.bottomMargin = Theme.dp(12);
		column.addView(filter, filterParams);

		adapter = new BaseAdapter()
		{
			@Override public int getCount() { return shown.size(); }
			@Override public Object getItem(int position) { return shown.get(position); }
			@Override public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View recycled, ViewGroup parent)
			{
				final TextView row = recycled instanceof TextView
						? (TextView) recycled : newRow(parent.getContext());
				final Item item = shown.get(position);
				final boolean chosen = item.id.equals(PickerSheet.this.current);
				row.setText(T.t(item.label));
				row.setTextColor(chosen ? Theme.ACCENT : Theme.TEXT_CHIP);
				return row;
			}
		};

		list = new ListView(context);
		list.setAdapter(adapter);
		list.setDivider(new android.graphics.drawable.ColorDrawable(Theme.HAIRLINE));
		list.setDividerHeight(Math.max(1, Theme.dp(1)));
		list.setBackgroundColor(Color.TRANSPARENT);
		list.setOnItemClickListener((parent, view, position, id) ->
		{
			if (position < 0 || position >= shown.size())
				return;
			choice.chosen(shown.get(position).id);
			overlay.close(this);
		});
		column.addView(list, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
		setBodyView(column);

		filter.addTextChangedListener(new TextWatcher()
		{
			public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
			public void onTextChanged(CharSequence s, int a, int b, int c) {}
			public void afterTextChanged(Editable s) { apply(s.toString()); }
		});
	}

	private TextView newRow(Context context)
	{
		final TextView row = Theme.text(context, "", 15, Theme.TEXT_CHIP, false);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		row.setGravity(Gravity.CENTER_VERTICAL);
		Theme.padding(row, 20, 8, 20, 8);
		return row;
	}

	void setItems(List<String> ids, List<String> labels)
	{
		all.clear();
		for (int i = 0; i < ids.size() && i < labels.size(); ++i)
			all.add(new Item(ids.get(i), labels.get(i)));
		apply("");
	}

	private void apply(String needle)
	{
		final String lower = needle.trim().toLowerCase();
		shown.clear();
		int at = -1;
		for (Item item : all)
		{
			if (lower.isEmpty() || item.label.toLowerCase().contains(lower)
			    || item.id.toLowerCase().contains(lower))
			{
				if (item.id.equals(current))
					at = shown.size();
				shown.add(item);
			}
		}
		adapter.notifyDataSetChanged();
		if (at >= 0)
			list.setSelection(Math.max(0, at - 2));
	}
}
