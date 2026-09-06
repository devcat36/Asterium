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

final class OcularsSheet extends PropertySheet
{
	private static final String[][] PAGES = {
		{ "General",    "" },
		{ "Eyepieces",  "" },
		{ "Telescopes", "" },
		{ "Lenses",     "" },
		{ "Sensors",    "" },
		{ "About",      "" },
	};

	private static final String[][] CONTRIBUTORS = {
		{ "Bogdan Marinov",          ""                           },
		{ "Alexander Wolf",          ""                           },
		{ "Georg Zotti",             ""                           },
		{ "Rumen G. Bogdanovski",    ""                           },
		{ "Pawel Stolowski",         "Barlow lens feature"        },
		{ "Matt Hughes",             "Sensor crop overlay feature"},
		{ "Dhia Moakhar",            "Pixel grid feature"         },
	};

	private int pendingScroll = 0;

	OcularsSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "oculars", "Oculars", PAGES);
	}

	@Override
	void buildCuratedPage(LinearLayout into, String pageName)
	{
		if ("Eyepieces".equals(pageName))
			buildChooser(into, "eyepieces", "ocular", "Eyepiece", "New eyepiece");
		else if ("Telescopes".equals(pageName))
			buildChooser(into, "telescopes", "telescope", "Telescope", "New telescope");
		else if ("Lenses".equals(pageName))
			buildChooser(into, "lenses", "lens", "Lens", "New lens");
		else if ("Sensors".equals(pageName))
			buildChooser(into, "sensors", "ccd", "Sensor", "New sensor");
		else if ("About".equals(pageName))
			buildAbout(into);
		else
			buildGeneral(into);
	}

	private void buildGeneral(LinearLayout into)
	{
		final Context context = getContext();
		into.addView(Widgets.section(context, "Ocular view"));
		addSwitch(into, "Scale image circle",
		          "Show FOV circles relative to the apparent FOV of the widest-angle ocular",
		          "Oculars.flagScaleImageCircle");
		addSwitch(into, "Enable only if an object is selected", null,
		          "Oculars.flagRequireSelection");
		addSwitch(into, "Hide grids and lines when enabled", null,
		          "Oculars.flagHideGridsLines");
		addSwitch(into, "Auto-limit stellar magnitude",
		          "Apply limit for stellar magnitude based on telescope aperture",
		          "Oculars.flagAutoLimitMagnitude");
		addSwitch(into, "Restore FOV to initial values", null, "Oculars.flagInitFOVUsage");
		addSwitch(into, "Restore direction to initial values", null,
		          "Oculars.flagInitDirectionUsage");
		addSwitch(into, "Show FOV outline", "Show border circle",
		          "Oculars.flagShowContour");
		addSwitch(into, "Show compass rose",
		          "Show cardinal directions in equatorial coordinate system",
		          "Oculars.flagShowCardinals");
		addSwitch(into, "Align crosshair",
		          "Align the crosshair in equatorial coordinate system",
		          "Oculars.flagAlignCrosshair");
		final LinearLayout mask = addGroup(into, "Use semi-transparent mask",
		                                   "In ocular mode, allow some visibility outside ocular circle.",
		                                   "Oculars.flagSemiTransparency");
		addSlider(mask, "Opacity of semi-transparent mask", "Oculars.transparencyMask",
		          10., 90., 0);

		into.addView(Widgets.section(context, "Sensor view"));
		addSwitch(into, "Use degrees and minutes for FOV of CCD", null,
		          "Oculars.flagDMSDegrees");
		addSwitch(into, "Use horizontal coordinates instead equatorial coordinates", null,
		          "Oculars.flagHorizontalCoordinates");
		addSwitch(into, "Enable automatic switch of mount type", null,
		          "Oculars.flagAutosetMountForCCD");
		addSwitch(into, "Enable autozoom when switching CCD", null,
		          "Oculars.flagScalingFOVForCCD");
		addSwitch(into, "Show max exposure time for moving objects", null,
		          "Oculars.flagMaxExposureTimeForCCD");
		addSwitch(into, "Show angular limits of the off-axis guide", null,
		          "Oculars.flagShowOAGLimits");
		final LinearLayout crop = addGroup(into, "Show sensor crop overlay",
				"Enable a rectangular overlay showing the effective size of a number of pixels.",
				"Oculars.flagShowCcdCropOverlay");
		addSwitch(crop, "Show pixel grid",
		          "Enable pixel grid of sensor binned pixels within sensor crop overlay.",
		          "Oculars.flagShowCcdCropOverlayPixelGrid");
		addSlider(crop, "Horizontal size (width)", "Oculars.ccdCropOverlayHSize", 1., 6000., 0);
		addSlider(crop, "Vertical size (height)", "Oculars.ccdCropOverlayVSize", 1., 6000., 0);
		final LinearLayout focuser = addGroup(into, "Show focuser overlay", null,
		                                      "Oculars.flagShowFocuserOverlay");
		addSwitch(focuser, "1.25\"", null, "Oculars.flagUseSmallFocuserOverlay");
		addSwitch(focuser, "2\"", null, "Oculars.flagUseMediumFocuserOverlay");
		addSwitch(focuser, "3.3\"", null, "Oculars.flagUseLargeFocuserOverlay");

		into.addView(Widgets.section(context, "Telrad sight"));
		addSwitch(into, "Enable autozoom when switching Telrad", null,
		          "Oculars.flagScalingFOVForTelrad");

		into.addView(Widgets.section(context, "Colors"));
		addColor(into, "Line Color", "Oculars.lineColor");
		addColor(into, "Reticle Color", "Oculars.reticleColor");
		addColor(into, "Color for focuser overlay line", "Oculars.focuserColor");
	}

	private void buildChooser(LinearLayout into, final String listKey, final String group,
	                          final String noun, final String addLabel)
	{
		final Context context = getContext();
		NativeBridge.request("oculars", "", payload ->
		{
			if (!payload.optBoolean("loaded"))
			{
				into.addView(Widgets.note(context,
						"The Oculars plug-in did not load on this build."));
				return;
			}
			final JSONArray entries = payload.optJSONArray(listKey);
			final int count = entries == null ? 0 : entries.length();
			for (int i = 0; i < count; ++i)
			{
				final JSONObject entry = entries.optJSONObject(i);
				if (entry == null)
					continue;
				into.addView(entryRow(context, entry.optString("name"),
						entry.optString("detail"), group, noun, i,
						entry.optJSONObject("fields"), count > 1 || "lens".equals(group)));
				into.addView(Widgets.hairline(context));
			}
			if (count == 0)
				into.addView(Widgets.note(context, "Nothing of this kind is defined."));
			final TextView add = Widgets.secondaryButton(context, addLabel,
					v -> edit(group, noun, -1, null));
			final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(48));
			params.leftMargin = params.rightMargin = Theme.dp(16);
			params.topMargin = params.bottomMargin = Theme.dp(12);
			add.setLayoutParams(params);
			into.addView(add);
			restoreScroll(pendingScroll);
			pendingScroll = 0;
		});
	}

	private void edit(String group, String noun, int index, JSONObject fields)
	{
		overlay().open(new EquipmentSheet(getContext(), overlay(), group, noun, index, fields,
				this::reloadPage));
	}

	private View entryRow(Context context, String name, String detail,
	                      final String group, final String noun, final int index,
	                      final JSONObject fields, final boolean removable)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 18, 8, 8, 8);
		row.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 0), 0));
		row.setContentDescription(T.t(name));
		row.setOnClickListener(v -> edit(group, noun, index, fields));

		final LinearLayout titles = new LinearLayout(context);
		titles.setOrientation(LinearLayout.VERTICAL);
		titles.addView(Theme.text(context, name, 14, Theme.TEXT_CHIP, false));
		if (detail != null && !detail.isEmpty())
			titles.addView(Theme.text(context, detail, 11, Theme.TEXT_DIM, true));
		row.addView(titles, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		if (removable)
			row.addView(removeButton(context, group, index));
		return row;
	}

	private View removeButton(Context context, final String group, final int index)
	{
		final TextView button = Theme.text(context, "✕", 15, Theme.TEXT_FAINT, false);
		button.setGravity(Gravity.CENTER);
		button.setContentDescription(T.t("Delete"));
		button.setBackground(Theme.pressableCircle(Theme.circle(Color.TRANSPARENT,
		                                                        Color.TRANSPARENT)));
		button.setLayoutParams(new LinearLayout.LayoutParams(
				Theme.dp(Theme.TOUCH), Theme.dp(Theme.TOUCH)));
		button.setOnClickListener(v ->
		{
			try
			{
				final JSONObject request = new JSONObject();
				request.put("group", group);
				request.put("index", index);
				NativeBridge.send("oculars.remove", request.toString());
			}
			catch (org.json.JSONException ignored)
			{
				return;
			}
			pendingScroll = scrollOffset();
			reloadPage();
		});
		return button;
	}

	private void buildAbout(LinearLayout into)
	{
		final Context context = getContext();
		NativeBridge.request("oculars.about", "", payload ->
		{
			into.addView(Widgets.section(context, "Oculars Plug-in"));
			into.addView(Widgets.valueRow(context, "Version", payload.optString("version")));
			into.addView(Widgets.hairline(context));
			into.addView(Widgets.valueRow(context, "License", payload.optString("license")));
			into.addView(Widgets.hairline(context));
			into.addView(Widgets.valueRow(context, "Author", payload.optString("author")));
			into.addView(Widgets.hairline(context));

			into.addView(Widgets.section(context, "Contributors"));
			for (String[] person : CONTRIBUTORS)
			{
				into.addView(Widgets.valueRow(context, person[0],
						person[1].isEmpty() ? "" : T.t(person[1])));
				into.addView(Widgets.hairline(context));
			}

			into.addView(Widgets.note(context,
					"The full text of the licence is on the Help screen, under Licences."));
		});
	}
}
