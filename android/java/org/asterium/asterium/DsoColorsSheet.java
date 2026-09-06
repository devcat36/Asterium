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
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

final class DsoColorsSheet extends Sheet
{
	DsoColorsSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "dsocolors", "Configure colors of markers");

		body.addView(Widgets.note(context,
				"Tap a colour to change it. Changes show on the sky at once and are kept."));
		NativeBridge.request("dso.colors", "", payload ->
		{
			final JSONArray colors = payload.optJSONArray("colors");
			if (colors == null || colors.length() == 0)
			{
				body.addView(Widgets.note(context, "The engine did not answer with any colours."));
				return;
			}
			for (int i = 0; i < colors.length(); ++i)
			{
				final JSONObject color = colors.optJSONObject(i);
				if (color != null)
					body.addView(colorRow(context, color.optString("id"),
					                      color.optString("label"),
					                      ColorPicker.parse(color.optString("value"))));
			}
		});
	}

	private View colorRow(Context context, final String propertyId, final String label, int color)
	{
		final GradientDrawable swatch = Theme.circle(color, Theme.PANEL_EDGE);

		final LinearLayout group = new LinearLayout(context);
		group.setOrientation(LinearLayout.VERTICAL);

		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 16, 8, 16, 8);
		row.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 0), 0));
		row.setContentDescription(T.t(label));
		row.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final View well = new View(context);
		well.setBackground(swatch);
		row.addView(well, new LinearLayout.LayoutParams(Theme.dp(28), Theme.dp(28)));
		group.addView(row);
		group.addView(Widgets.hairline(context));

		row.setOnClickListener(v -> ColorPicker.show(context, label, currentColor(swatch), chosen ->
		{
			swatch.setColor(chosen);
			NativeBridge.send("prop.set", propertyId + "=" + ColorPicker.hex(chosen));
		}));
		return group;
	}

	private static int currentColor(GradientDrawable swatch)
	{
		final android.content.res.ColorStateList tint = swatch.getColor();
		return tint == null ? Color.WHITE : tint.getDefaultColor();
	}

}
