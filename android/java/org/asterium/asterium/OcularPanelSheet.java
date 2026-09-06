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
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

final class OcularPanelSheet extends Sheet
{
	private static final int LINES = 4;

	private final Block eyepiece;
	private final Block sensor;
	private final Block telescope;
	private final Block lens;

	private static final class Block
	{
		final LinearLayout view;
		final TextView title;
		final TextView[] lines = new TextView[LINES];
		final String titleKey;
		final String linesKey;

		Block(OcularPanelSheet sheet, Context context, String titleKey, String linesKey,
		      String action)
		{
			this.titleKey = titleKey;
			this.linesKey = linesKey;
			title = Theme.text(context, "", 15, Theme.ACCENT, false);
			title.setSingleLine(true);
			title.setEllipsize(android.text.TextUtils.TruncateAt.END);

			view = new LinearLayout(context);
			view.setOrientation(LinearLayout.VERTICAL);
			Theme.padding(view, 0, 4, 0, 8);

			final LinearLayout head = new LinearLayout(context);
			head.setOrientation(LinearLayout.HORIZONTAL);
			head.setGravity(Gravity.CENTER_VERTICAL);
			Theme.padding(head, 6, 0, 6, 0);
			head.addView(sheet.stepper(context, "◀◀", "Previous", action + "_Decrement"));
			final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
					0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
			titleParams.leftMargin = titleParams.rightMargin = Theme.dp(6);
			head.addView(title, titleParams);
			head.addView(sheet.stepper(context, "▶▶", "Next", action + "_Increment"));
			view.addView(head, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));

			for (int i = 0; i < LINES; ++i)
			{
				lines[i] = Theme.text(context, "", 13, Theme.TEXT_CHIP, false);
				lines[i].setSingleLine(true);
				lines[i].setEllipsize(android.text.TextUtils.TruncateAt.END);
				Theme.padding(lines[i], 22, 4, 18, 4);
				lines[i].setVisibility(View.GONE);
				view.addView(lines[i]);
			}
			view.addView(Widgets.hairline(context));
		}

		void update(JSONObject card, boolean visible)
		{
			view.setVisibility(visible ? View.VISIBLE : View.GONE);
			if (!visible)
				return;
			title.setText(card.optString(titleKey));
			final JSONArray text = card.optJSONArray(linesKey);
			for (int i = 0; i < LINES; ++i)
			{
				final boolean has = text != null && i < text.length();
				lines[i].setVisibility(has ? View.VISIBLE : View.GONE);
				if (has)
					lines[i].setText(text.optString(i));
			}
		}
	}

	OcularPanelSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "ocularpanel", "Oculars", !overlay.isTablet(), true);

		eyepiece = new Block(this, context, "eyepieceTitle", "eyepieceLines",
		                     "actionShow_Ocular");
		sensor = new Block(this, context, "sensorTitle", "sensorLines", "actionShow_CCD");
		telescope = new Block(this, context, "telescopeTitle", "telescopeLines",
		                      "actionShow_Telescope");
		lens = new Block(this, context, "lensTitle", "lensLines", "actionShow_Lens");

		body.addView(eyepiece.view);
		body.addView(sensor.view);
		body.addView(telescope.view);
		body.addView(lens.view);
		body.addView(Widgets.navigationRow(context,
				T.t("Configure &Oculars").replace("&", ""), null,
				v -> overlay.open(new OcularsSheet(context, overlay))));

		onState(NativeBridge.lastState());
	}

	private TextView stepper(Context context, String glyph, String label, final String action)
	{
		final TextView key = Theme.text(context, glyph, 15, Theme.ACCENT_LINK, false);
		key.setGravity(Gravity.CENTER);
		key.setContentDescription(T.t(label));
		key.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 10), 10));
		key.setOnClickListener(v -> NativeBridge.send("action", action));
		key.setLayoutParams(new LinearLayout.LayoutParams(
				Theme.dp(Theme.TOUCH), Theme.dp(Theme.TOUCH)));
		return key;
	}

	@Override
	void onState(JSONObject state)
	{
		final JSONObject oculars = state == null ? null : state.optJSONObject("oculars");
		final JSONObject card = oculars == null ? null : oculars.optJSONObject("card");
		if (card == null)
			return;

		final boolean imaging = oculars.optBoolean("sensor");
		final boolean binocular = !imaging && oculars.optBoolean("binocular");
		eyepiece.update(card, !imaging);
		sensor.update(card, imaging);
		telescope.update(card, !binocular);
		lens.update(card, !binocular);
	}
}
