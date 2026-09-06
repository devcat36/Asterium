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
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

final class EquipmentSheet extends Sheet
{
	private static final String[][] EYEPIECE = {
		{ "name",               "Name:",                     "text"   },
		{ "afov",               "aFOV:",                     "number" },
		{ "efl",                "Focal length:",             "number" },
		{ "fieldStop",          "Field stop:",               "number" },
		{ "binoculars",         "Binoculars",                "bool"   },
		{ "permanentCrosshair", "Has permanent cross-hairs", "bool"   },
	};

	private static final String[][] TELESCOPE = {
		{ "name",        "Name:",            "text"   },
		{ "focalLength", "Focal length:",    "number" },
		{ "diameter",    "Diameter:",        "number" },
		{ "hFlip",       "Horizontal flip",  "bool"   },
		{ "vFlip",       "Vertical flip",    "bool"   },
		{ "equatorial",  "Equatorial Mount", "bool"   },
	};

	private static final String[][] LENS = {
		{ "name",      "Name:",       "text"   },
		{ "multipler", "Multiplier:", "number" },
	};

	private static final String[][] SENSOR = {
		{ "name",            "Name:",              "text"   },
		{ "resolutionX",     "Resolution x:",      "number" },
		{ "resolutionY",     "Resolution y:",      "number" },
		{ "chip_width",      "Chip width:",        "number" },
		{ "chip_height",     "Chip height:",       "number" },
		{ "chip_rot_angle",  "Rotation Angle:",    "number" },
		{ "binningX",        "Binning x:",         "number" },
		{ "binningY",        "Binning y:",         "number" },
		{ "has_oag",         "Off-Axis guider",    "bool"   },
		{ "prism_height",    "Prism/CCD height:",  "number" },
		{ "prism_width",     "Prism/CCD width:",   "number" },
		{ "prism_distance",  "Prism/CCD distance:","number" },
		{ "prism_pos_angle", "Position angle:",    "number" },
	};

	private final String group;
	private final int index;
	private final Runnable changed;
	private final Map<String, EditText> texts = new LinkedHashMap<>();
	private final Map<String, boolean[]> flags = new LinkedHashMap<>();

	static String[][] fieldsFor(String group)
	{
		if ("telescope".equals(group))
			return TELESCOPE;
		if ("lens".equals(group))
			return LENS;
		if ("ccd".equals(group))
			return SENSOR;
		return EYEPIECE;
	}

	EquipmentSheet(Context context, Overlay overlay, String group, String title, int index,
	               JSONObject values, Runnable changed)
	{
		super(context, overlay, "equipment", title);
		this.group = group;
		this.index = index;
		this.changed = changed;

		for (String[] spec : fieldsFor(group))
		{
			final String stored = values == null ? "" : values.optString(spec[0]);
			if ("bool".equals(spec[2]))
			{
				final boolean[] held = { "true".equalsIgnoreCase(stored) };
				flags.put(spec[0], held);
				body.addView(Widgets.switchRow(context, spec[1], null, held[0],
						(button, checked) -> held[0] = checked));
				body.addView(Widgets.hairline(context));
			}
			else
			{
				final EditText field = Widgets.field(context, spec[1], spec[1],
				                                     "number".equals(spec[2]));
				field.setText(stored);
				texts.put(spec[0], field);
				body.addView(fieldRow(context, spec[1], field));
			}
		}

		final LinearLayout footer = addFooter();
		footer.addView(Widgets.primaryButton(context, "Save settings", v -> save()),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
	}

	private static android.view.View fieldRow(Context context, String label, EditText field)
	{
		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(column, 16, 10, 16, 6);
		column.addView(Theme.text(context, label, 12, Theme.TEXT_DIM, false));
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.topMargin = Theme.dp(6);
		column.addView(field, params);
		return column;
	}

	private void save()
	{
		final JSONObject fields = new JSONObject();
		try
		{
			for (Map.Entry<String, EditText> entry : texts.entrySet())
				fields.put(entry.getKey(), entry.getValue().getText().toString().trim());
			for (Map.Entry<String, boolean[]> entry : flags.entrySet())
				fields.put(entry.getKey(), entry.getValue()[0] ? "true" : "false");
			final JSONObject request = new JSONObject();
			request.put("group", group);
			request.put("index", index);
			request.put("fields", fields);
			NativeBridge.send("oculars.put", request.toString());
		}
		catch (org.json.JSONException e)
		{
			return;
		}
		done();
	}

	private void done()
	{
		overlay().close(this);
		changed.run();
	}
}
