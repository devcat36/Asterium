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
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class GrsSheet extends Sheet
{
	private static final String JUPOS = "https://jupos.hier-im-netz.de/rGrs.htm";

	private final EditText longitude;
	private final EditText drift;
	private final TextView epoch;
	private double epochJd = 0.;

	GrsSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "grs", "Great Red Spot details");

		body.addView(Widgets.note(context,
				"The Great Red Spot drifts in System II longitude. Enter a recent measurement "
				+ "and the drift rate to carry it forward from."));

		body.addView(Widgets.section(context, "Custom settings for position of GRS"));

		longitude = Widgets.field(context, "Longitude of GRS", "216", true);
		body.addView(labelled(context, "Longitude of GRS",
		                      "Jovigraphic longitude of the spot in System II, in degrees",
		                      longitude));

		drift = Widgets.field(context, "Annual drift", "15.21875", true);
		body.addView(labelled(context, "Annual drift",
		                      "Degrees per year the longitude moves by", drift));

		epoch = Theme.text(context, "", 13, Theme.ACCENT, true);
		final LinearLayout epochBox = new LinearLayout(context);
		epochBox.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(epochBox, 16, 10, 16, 4);
		final TextView caption = Theme.text(context, "DATE AND TIME (UTC)", 10, Theme.TEXT_FAINT, true);
		caption.setLetterSpacing(0.12f);
		epochBox.addView(caption);
		Theme.padding(epoch, 0, 6, 0, 10);
		epochBox.addView(epoch);
		epochBox.addView(Widgets.secondaryButton(context, "Use the date now shown in the sky",
				v -> setEpoch(currentSkyJd())),
				new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(48)));
		body.addView(epochBox);

		body.addView(Widgets.navigationRow(context, "View recent GRS measurements",
				"Opens the JUPOS project in a browser", v -> openJupos()));

		final LinearLayout footer = addFooter();
		footer.addView(Widgets.primaryButton(context, "Apply", v -> apply()),
				new LinearLayout.LayoutParams(0, Theme.dp(48), 1f));

		NativeBridge.request("props", "SolarSystem", this::fill);
	}

	private static android.view.View labelled(Context context, String name, String sub, EditText field)
	{
		final LinearLayout box = new LinearLayout(context);
		box.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(box, 16, 8, 16, 8);
		final TextView caption = Theme.text(context, name, 10, Theme.TEXT_FAINT, true);
		caption.setAllCaps(true);
		caption.setLetterSpacing(0.12f);
		box.addView(caption);
		final TextView hint = Theme.text(context, sub, 11, Theme.TEXT_DIM, false);
		Theme.padding(hint, 0, 4, 0, 8);
		box.addView(hint);
		box.addView(field, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(52)));
		return box;
	}

	private void fill(JSONObject payload)
	{
		final JSONArray list = payload.optJSONArray("props");
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject property = list.optJSONObject(i);
			if (property == null)
				continue;
			final String id = property.optString("id");
			if (id.equals("SolarSystem.grsLongitude"))
				longitude.setText(T.t(String.valueOf(property.optInt("value"))));
			else if (id.equals("SolarSystem.grsDrift"))
				drift.setText(T.t(String.format(Locale.US, "%.5f", property.optDouble("value"))));
			else if (id.equals("SolarSystem.grsJD"))
				setEpoch(property.optDouble("value"));
		}
	}

	private void setEpoch(double jd)
	{
		if (jd <= 0.)
			return;
		epochJd = jd;
		epoch.setText(T.t(asDate(jd)));
	}

	private static String asDate(double jd)
	{
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(new Date(Math.round((jd - 2440587.5) * 86400000.)));
	}

	private static double currentSkyJd()
	{
		final JSONObject state = NativeBridge.lastState();
		return state == null ? 0. : state.optDouble("jd", 0.);
	}

	private void apply()
	{
		final double lon = Widgets.number(longitude);
		if (!Double.isNaN(lon))
		{
			NativeBridge.send("prop.set", "SolarSystem.grsLongitude="
					+ (int) Math.round(Math.max(0., Math.min(359., lon))));
		}
		final double rate = Widgets.number(drift);
		if (!Double.isNaN(rate))
		{
			NativeBridge.send("prop.set", "SolarSystem.grsDrift="
					+ String.format(Locale.US, "%.5f", Math.max(-90., Math.min(90., rate))));
		}
		if (epochJd > 0.)
			NativeBridge.send("prop.set", "SolarSystem.grsJD=" + String.format(Locale.US, "%.6f", epochJd));
		overlay().close(this);
	}

	private void openJupos()
	{
		try
		{
			final Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(JUPOS));
			browse.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getContext().startActivity(browse);
		}
		catch (Exception e)
		{
			android.util.Log.w("Asterium", "no browser for " + JUPOS, e);
		}
	}
}
