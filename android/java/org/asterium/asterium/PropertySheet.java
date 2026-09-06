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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

abstract class PropertySheet extends Sheet
{
	private final String[][] pages;
	private final LinearLayout tabs;
	private final LinearLayout pageBody;
	private int page = 0;

	private final Map<String, JSONObject> values = new HashMap<>();

	PropertySheet(Context context, Overlay overlay, String id, String title, String[][] pages)
	{
		super(context, overlay, id, title);
		this.pages = pages;
		tabs = pages.length > 1 ? addTabRail() : null;
		pageBody = new LinearLayout(context);
		pageBody.setOrientation(LinearLayout.VERTICAL);
		body.addView(pageBody);
		buildTabs();
		showPage(recallTab(pages.length));
	}

	private void buildTabs()
	{
		if (tabs == null)
			return;
		tabs.removeAllViews();
		for (int i = 0; i < pages.length; ++i)
		{
			final int index = i;
			tabs.addView(Widgets.tabChip(getContext(), pages[i][0], i == page,
			                             v -> showPage(index)));
		}
	}

	private void showPage(final int index)
	{
		page = index;
		rememberTab(index);
		buildTabs();
		pageBody.removeAllViews();
		values.clear();

		final String module = pages[index][1];
		if (module.isEmpty())
		{
			NativeBridge.request("props", "", payload ->
			{
				final JSONArray list = payload.optJSONArray("props");
				for (int i = 0; list != null && i < list.length(); ++i)
				{
					final JSONObject property = list.optJSONObject(i);
					if (property != null)
						values.put(property.optString("id"), property);
				}
				buildCuratedPage(pageBody, pages[index][0]);
			});
			return;
		}
		pageBody.addView(Widgets.section(getContext(), pages[index][0] + " settings"));
		NativeBridge.request("props", module, payload ->
		{
			final JSONArray list = payload.optJSONArray("props");
			if (list == null || list.length() == 0)
			{
				pageBody.addView(Widgets.note(getContext(),
						"This part of the engine exposes no settings on this build."));
				return;
			}
			for (int i = 0; i < list.length(); ++i)
			{
				final JSONObject property = list.optJSONObject(i);
				if (property != null)
					pageBody.addView(rowFor(property));
			}
		});
	}

	abstract void buildCuratedPage(LinearLayout into, String pageName);

	protected void reloadPage()
	{
		showPage(page);
	}

	private View rowFor(final JSONObject property)
	{
		final Context context = getContext();
		final String id = property.optString("id");
		final String label = humanise(property.optString("label"));
		final String type = property.optString("type");

		if (property.optBoolean("readOnly"))
			return Widgets.valueRow(context, label, property.optString("value"));

		if ("bool".equals(type))
		{
			return Widgets.switchRow(context, label, null, property.optBoolean("value"),
					(button, checked) -> NativeBridge.send("prop.set", id + "=" + checked));
		}
		if ("color".equals(type))
			return colorRow(context, label, id, ColorPicker.parse(property.optString("value")));
		if ("int".equals(type) || "double".equals(type))
		{
			final double current = property.optDouble("value", 0.);
			final double span = Math.max(1., Math.abs(current) * 4.);
			final boolean whole = "int".equals(type);
			final Scale scale = linear(whole ? 0. : -span, span, whole ? 0 : 2);
			return sliderRow(context, label, scale, current,
					value -> NativeBridge.send("prop.set", id + "=" + scale.wire(value)));
		}
		return Widgets.valueRow(context, label, property.optString("value"));
	}

	private static String humanise(String name)
	{
		String text = name.startsWith("flag") ? name.substring(4) : name;
		final StringBuilder out = new StringBuilder();
		for (int i = 0; i < text.length(); ++i)
		{
			final char c = text.charAt(i);
			if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(text.charAt(i - 1)))
				out.append(' ');
			out.append(i == 0 ? Character.toUpperCase(c) : c);
		}
		return out.toString();
	}

	protected JSONObject value(String propertyId)
	{
		return values.get(propertyId);
	}

	protected interface Toggled
	{
		void changed(boolean on);
	}

	protected interface Chosen
	{
		void set(String id);
	}

	protected View addSwitch(LinearLayout into, String label, String sub, String propertyId)
	{
		return addSwitch(into, label, sub, propertyId, null);
	}

	protected View addSwitch(LinearLayout into, String label, String sub, final String propertyId,
	                         final Toggled onChange)
	{
		final JSONObject property = values.get(propertyId);
		final boolean on = property != null && property.optBoolean("value");
		final View row = Widgets.switchRow(getContext(), label, sub, on,
				(button, checked) ->
		{
			NativeBridge.send("prop.set", propertyId + "=" + checked);
			if (onChange != null)
				onChange.changed(checked);
		});
		into.addView(row);
		into.addView(Widgets.hairline(getContext()));
		if (onChange != null)
			onChange.changed(on);
		return row;
	}

	protected LinearLayout addGroup(LinearLayout into, String label, String sub, String propertyId)
	{
		final Context context = getContext();
		final LinearLayout children = new LinearLayout(context);
		children.setOrientation(LinearLayout.VERTICAL);

		final LinearLayout indented = new LinearLayout(context);
		indented.setOrientation(LinearLayout.HORIZONTAL);
		final View rail = new View(context);
		rail.setBackgroundColor(0x40FDD886);
		final LinearLayout.LayoutParams railParams = new LinearLayout.LayoutParams(
				Math.max(1, Theme.dp(2)), LinearLayout.LayoutParams.MATCH_PARENT);
		railParams.leftMargin = Theme.dp(22);
		railParams.topMargin = railParams.bottomMargin = Theme.dp(6);
		indented.addView(rail, railParams);
		indented.addView(children, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		addSwitch(into, label, sub, propertyId,
				on -> indented.setVisibility(on ? View.VISIBLE : View.GONE));
		into.addView(indented);
		return children;
	}

	protected void addChoice(LinearLayout into, String label, final String propertyId,
	                         final String[] ids, final String[] labels, final Chosen also)
	{
		final JSONObject property = value(propertyId);
		final String current = property == null ? "" : property.optString("value");
		addPick(into, label, current, ids, labels, id ->
		{
			NativeBridge.send("prop.set", propertyId + "=" + id);
			if (also != null)
				also.set(id);
		});
		if (also != null)
			also.set(current);
	}

	protected void addPick(LinearLayout into, String label, String current,
	                       final String[] ids, final String[] labels, final Chosen onPick)
	{
		final Context context = getContext();
		final View row = Widgets.navigationRow(context, label, null, null);
		final TextView shown = (TextView) row.getTag();
		shown.setTextColor(Theme.ACCENT);
		into.addView(row);
		into.addView(Widgets.hairline(context));

		final String[] held = { current == null ? "" : current };
		final Chosen show = id ->
		{
			held[0] = id;
			for (int i = 0; i < ids.length && i < labels.length; ++i)
			{
				if (ids[i].equals(id))
				{
					shown.setText(T.t(labels[i]));
					shown.setVisibility(View.VISIBLE);
				}
			}
		};
		show.set(held[0]);

		row.setOnClickListener(v ->
		{
			final PickerSheet picker = new PickerSheet(context, overlay(), label, held[0],
					id ->
					{
						show.set(id);
						onPick.set(id);
					});
			overlay().open(picker);
			picker.setItems(java.util.Arrays.asList(ids), java.util.Arrays.asList(labels));
		});
	}

	protected void addColor(LinearLayout into, String label, String propertyId)
	{
		final JSONObject property = value(propertyId);
		into.addView(colorRow(getContext(), label, propertyId,
				ColorPicker.parse(property == null ? "" : property.optString("value"))));
		into.addView(Widgets.hairline(getContext()));
	}

	private View colorRow(Context context, final String label, final String propertyId, int color)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 16, 8, 16, 8);
		row.setBackground(Theme.pressable(Theme.box(android.graphics.Color.TRANSPARENT, 0), 0));
		row.setContentDescription(T.t(label));

		row.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final View swatch = new View(context);
		final LinearLayout.LayoutParams swatchParams =
				new LinearLayout.LayoutParams(Theme.dp(36), Theme.dp(24));
		swatchParams.rightMargin = Theme.dp(10);
		row.addView(swatch, swatchParams);
		row.addView(Theme.text(context, "›", 20, Theme.ACCENT_LINK, false));

		final int[] held = { color };
		swatch.setBackground(Theme.box(color, 6, Theme.PANEL_EDGE));
		row.setOnClickListener(v -> ColorPicker.show(context, label, held[0], chosen ->
		{
			held[0] = chosen;
			swatch.setBackground(Theme.box(chosen, 6, Theme.PANEL_EDGE));
			NativeBridge.send("prop.set", propertyId + "=" + ColorPicker.hex(chosen));
		}));
		return row;
	}

	protected static abstract class Scale
	{
		final double min, max;
		final int decimals;

		Scale(double min, double max, int decimals)
		{
			this.min = min;
			this.max = max;
			this.decimals = decimals;
		}

		double value(double position) { return min + (max - min) * position; }

		double position(double value)
		{
			if (max <= min)
				return 0.;
			return Math.max(0., Math.min(1., (value - min) / (max - min)));
		}

		String wire(double value) { return format(value, decimals); }

		String readout(double value) { return format(value, decimals); }
	}

	protected static Scale linear(double min, double max, int decimals)
	{
		return new Scale(min, max, decimals) {};
	}

	interface OnSlide
	{
		void slid(double value);
	}

	protected void addSlider(LinearLayout into, String label, String propertyId,
	                         double min, double max, int decimals)
	{
		addSlider(into, label, propertyId, linear(min, max, decimals));
	}

	protected void addSlider(LinearLayout into, String label, final String propertyId,
	                         final Scale scale)
	{
		final JSONObject property = values.get(propertyId);
		into.addView(sliderRow(getContext(), label, scale,
				property == null ? scale.min : property.optDouble("value", scale.min),
				value -> NativeBridge.send("prop.set", propertyId + "=" + scale.wire(value))));
	}

	static View sliderRow(Context context, String label, final Scale scale, double value,
	                      final OnSlide listener)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(row, 16, 8, 16, 10);

		final LinearLayout heading = new LinearLayout(context);
		heading.setOrientation(LinearLayout.HORIZONTAL);
		heading.setGravity(Gravity.CENTER_VERTICAL);
		heading.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		final TextView readout = Theme.text(context, scale.readout(value), 12, Theme.ACCENT, true);
		readout.setGravity(Gravity.END);
		heading.addView(readout);
		row.addView(heading);

		final int steps = scale.decimals == 0
				? Math.max(1, Math.min(1000, (int) Math.round(scale.max - scale.min)))
				: 1000;
		final SeekBar bar = Widgets.slider(context, steps,
				(int) Math.round(scale.position(value) * steps));
		bar.setContentDescription(T.t(label));
		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				final double now = scale.value(progress / (double) steps);
				readout.setText(T.t(scale.readout(now)));
				if (fromUser)
					listener.slid(now);
			}
			public void onStartTrackingTouch(SeekBar seekBar) {}
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		row.addView(bar);
		return row;
	}

	static String format(double value, int decimals)
	{
		return String.format(java.util.Locale.US, "%." + decimals + "f", value);
	}

}
