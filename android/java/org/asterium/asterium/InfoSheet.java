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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

final class InfoSheet extends Sheet
{
	private final TextView nameText;
	private final TextView designationText;
	private final TextView magnitudeText;
	private final ImageView starButton;
	private boolean saved;
	private final LinearLayout rows;
	private final LinearLayout tiles;

	InfoSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "info", "Object", !overlay.isTablet());

		final LinearLayout head = new LinearLayout(context);
		head.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(head, 18, 6, 18, 12);

		final LinearLayout names = new LinearLayout(context);
		names.setOrientation(LinearLayout.VERTICAL);
		final LinearLayout nameRow = new LinearLayout(context);
		nameRow.setOrientation(LinearLayout.HORIZONTAL);
		nameRow.setGravity(Gravity.CENTER_VERTICAL);
		nameText = Theme.text(context, "", 26, Theme.ACCENT, false);
		nameRow.addView(nameText, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		saved = selectionSaved();
		starButton = Widgets.starButton(context, saved);
		starButton.setOnClickListener(v -> NativeBridge.send("obslist.save"));
		final LinearLayout.LayoutParams starParams =
				new LinearLayout.LayoutParams(Theme.dp(40), Theme.dp(40));
		starParams.leftMargin = Theme.dp(4);
		nameRow.addView(starButton, starParams);
		designationText = Theme.text(context, "", 12, Theme.TEXT_DIM, true);
		Theme.padding(designationText, 0, 4, 0, 0);
		names.addView(nameRow, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		names.addView(designationText);
		head.addView(names, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final LinearLayout magnitude = new LinearLayout(context);
		magnitude.setOrientation(LinearLayout.VERTICAL);
		magnitude.setGravity(Gravity.END);
		magnitudeText = Theme.text(context, "", 22, Theme.TEXT, true);
		magnitudeText.setGravity(Gravity.END);
		final TextView caption = Theme.text(context, "magnitude", 10, Theme.TEXT_DIM, false);
		caption.setGravity(Gravity.END);
		magnitude.addView(magnitudeText);
		magnitude.addView(caption);
		head.addView(magnitude);
		body.addView(head);

		tiles = new LinearLayout(context);
		tiles.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(tiles, 18, 0, 18, 14);
		body.addView(tiles);

		rows = new LinearLayout(context);
		rows.setOrientation(LinearLayout.VERTICAL);
		body.addView(rows);
	}

	@Override
	void onShown()
	{
		refresh();
	}

	@Override
	void onState(JSONObject state)
	{
		final JSONObject selection = state.optJSONObject("sel");
		if (selection == null)
		{
			overlay().close(this);
			return;
		}
		if (!selection.optString("name").contentEquals(nameText.getText()))
			refresh();
		final boolean onList = selection.optBoolean("saved");
		if (onList != saved)
		{
			saved = onList;
			Widgets.setStarred(starButton, onList);
		}
	}

	private static boolean selectionSaved()
	{
		final JSONObject state = NativeBridge.lastState();
		final JSONObject selection = state == null ? null : state.optJSONObject("sel");
		return selection != null && selection.optBoolean("saved");
	}

	private void refresh()
	{
		NativeBridge.request("info", "all", payload ->
		{
			nameText.setText(T.t(payload.optString("name")));
			final String designation = payload.optString("sub");
			designationText.setText(T.t(designation));
			designationText.setVisibility(designation.isEmpty() ? GONE : VISIBLE);
			final String magnitude = payload.optString("mag");
			magnitudeText.setText(T.t(magnitude.isEmpty() ? "—" : magnitude));

			tiles.removeAllViews();
			final JSONObject times = payload.optJSONObject("rts");
			if (times != null && times.has("rise"))
			{
				addTile("Rises", times.optString("rise"), false);
				addTile("Transit", times.optString("transit"), true);
				addTile("Sets", times.optString("set"), false);
			}

			rows.removeAllViews();
			final JSONArray list = payload.optJSONArray("rows");
			if (list != null)
			{
				for (int i = 0; i < list.length(); ++i)
				{
					final JSONObject row = list.optJSONObject(i);
					if (row == null)
						continue;
					rows.addView(Widgets.valueRow(getContext(),
							row.optString("k"), row.optString("v")));
					rows.addView(Widgets.hairline(getContext()));
				}
			}
		});
	}

	private void addTile(String label, String value, boolean accent)
	{
		final View tile = Widgets.statTile(getContext(), label, value, accent);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		if (tiles.getChildCount() > 0)
			params.leftMargin = Theme.dp(8);
		tiles.addView(tile, params);
	}
}
