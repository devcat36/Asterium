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
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class EclipsesPage extends AstroCalcPage
{
	private static final String[] LABELS =
			{ "All solar eclipses", "Local solar eclipses", "Lunar eclipses", "Planetary transits" };
	private static final String[] TABS = { "solar", "local", "lunar", "transits" };

	private int tab = 0;
	private JSONObject state = new JSONObject();
	private final List<JSONObject> rows = new ArrayList<>();

	private final Map<Integer, JSONArray> contacts = new HashMap<>();

	private int expanded = -1;
	private int fromYear = 0;
	private int years = 10;

	private LinearLayout tabRail, controls;
	private BaseAdapter adapter;

	@Override
	View build()
	{
		final Context context = context();
		host.addButton("Cleanup", false, () ->
		{
			NativeBridge.send("eclipses.clear", TABS[tab]);
			reset();
			ask("eclipses");
		});
		host.addButton("Calculate", true, this::calculate);

		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);

		tabRail = new LinearLayout(context);
		tabRail.setOrientation(LinearLayout.VERTICAL);
		column.addView(tabRail, fill());
		buildTabs();

		controls = new LinearLayout(context);
		controls.setOrientation(LinearLayout.VERTICAL);

		adapter = new BaseAdapter()
		{
			@Override public int getCount() { return rows.size(); }
			@Override public Object getItem(int position) { return rows.get(position); }
			@Override public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View recycled, ViewGroup parent)
			{
				final EclipseRow row = recycled instanceof EclipseRow
						? (EclipseRow) recycled : new EclipseRow(parent.getContext());
				row.bind(position, rows.get(position));
				return row;
			}
		};

		final ListView list = resultsList(controls, adapter);
		column.addView(list, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
		return column;
	}

	@Override
	void onShow()
	{
		ask("eclipses");
	}

	private void buildTabs()
	{
		final View rail = subTabs(LABELS, tab, index ->
		{
			tab = index;
			reset();
			setActiveTab(tabRail.getChildAt(0), tab);
			ask("eclipses");
		});
		tabRail.removeAllViews();
		tabRail.addView(rail);
	}

	private void reset()
	{
		rows.clear();
		contacts.clear();
		expanded = -1;
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}

	private void ask(String verb)
	{
		NativeBridge.request(verb, TABS[tab], payload -> { state = payload; show(); });
	}

	private void calculate()
	{
		controls.clearFocus();
		host.setSummary("Calculating…");
		reset();
		scrollToNextResult = true;
		NativeBridge.request("eclipses.generate", TABS[tab] + "|" + fromYear + "|" + years,
				payload -> { state = payload; show(); });
	}

	private void show()
	{
		fromYear = state.optInt("fromYear", fromYear);
		years = state.optInt("years", years);

		rows.clear();
		rows.addAll(rowsOf(state, "rows"));
		contacts.clear();
		expanded = -1;
		buildControls();
		if (adapter != null)
			adapter.notifyDataSetChanged();
		showResults(rows.size());

		if (!state.optBoolean("onEarth", true))
			host.setSummary("Eclipses are computed for observers on Earth.");
		else if (rows.isEmpty())
			host.setSummary("No eclipses found. Pick a range and calculate.");
		else

			host.setSummary(rows.size() + (rows.size() == 1 ? " eclipse, " : " eclipses, ")
					+ fromYear + "–" + (fromYear + years));
	}

	private void buildControls()
	{
		final Context context = context();
		controls.removeAllViews();

		controls.addView(Widgets.section(context, "Range"));

		controls.addView(numberRow("From year", fromYear, value -> fromYear = value));
		controls.addView(numberRow("And the next (years)", years, value -> years = value));

		final JSONArray filters = state.optJSONArray("filters");
		if (filters != null && filters.length() > 0)
		{
			controls.addView(Widgets.section(context, "Include"));
			final LinearLayout chips = new LinearLayout(context);
			chips.setOrientation(LinearLayout.HORIZONTAL);
			Theme.padding(chips, 16, 0, 16, 8);
			for (int i = 0; i < filters.length(); ++i)
			{
				final JSONObject filter = filters.optJSONObject(i);
				if (filter == null)
					continue;
				final String key = filter.optString("id");
				final boolean[] on = { filter.optBoolean("on", true) };
				final TextView chip = Widgets.toggleChip(context, filter.optString("name"), on[0], null);
				chip.setOnClickListener(v ->
				{
					on[0] = !on[0];
					Widgets.setChipOn(chip, on[0]);
					NativeBridge.send("eclipses.set", key + "=" + (on[0] ? "true" : "false"));
				});
				final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
						0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
				params.leftMargin = i == 0 ? 0 : Theme.dp(6);
				chips.addView(chip, params);
			}
			controls.addView(chips, fill());
		}

		final String note = state.optString("note");
		if (!note.isEmpty())
			controls.addView(note(note));
		controls.addView(Widgets.hairline(context));
	}

	private final class EclipseRow extends LinearLayout
	{
		private final TextView when, kind, value, note;
		private final LinearLayout detail;

		EclipseRow(Context context)
		{
			super(context);
			setOrientation(VERTICAL);

			final LinearLayout head = new LinearLayout(context);
			head.setOrientation(HORIZONTAL);
			head.setGravity(Gravity.CENTER_VERTICAL);
			head.setMinimumHeight(Theme.dp(52));
			Theme.padding(head, 16, 8, 16, 8);

			final LinearLayout titles = new LinearLayout(context);
			titles.setOrientation(VERTICAL);
			when = Theme.text(context, "", 14, Theme.TEXT, true);
			when.setSingleLine(true);
			kind = Theme.text(context, "", 10, Theme.TEXT_DIM, false);
			kind.setSingleLine(true);
			titles.addView(when);
			titles.addView(kind);

			head.addView(titles, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

			final LinearLayout numbers = new LinearLayout(context);
			numbers.setOrientation(VERTICAL);
			value = Theme.text(context, "", 14, Theme.ACCENT, true);
			value.setGravity(Gravity.END);
			note = Theme.text(context, "", 10, Theme.TEXT_DIM, true);
			note.setGravity(Gravity.END);
			numbers.addView(value);
			numbers.addView(note);
			final LayoutParams numberParams =
					new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			numberParams.leftMargin = Theme.dp(10);
			head.addView(numbers, numberParams);
			addView(head);

			detail = new LinearLayout(context);
			detail.setOrientation(VERTICAL);
			detail.setBackgroundColor(Theme.FILL_FAINT);
			Theme.padding(detail, 0, 4, 0, 10);
			addView(detail);
		}

		void bind(final int position, final JSONObject entry)
		{
			final boolean open = position == expanded;
			when.setText(T.t(entry.optString("when")));
			switch (tab)
			{
				case 1:
					kind.setText(T.t(entry.optString("type")));
					value.setText(T.t(entry.optString("mag")));
					note.setText(String.format(T.t("max %s"), entry.optString("max")));
					break;
				case 2:
					kind.setText(T.t(entry.optString("type") + " · " + entry.optString("visibility")));
					value.setText(T.t(entry.optString("umag")));
					note.setText(String.format(T.t("Saros %s"), entry.optString("saros")));
					break;
				case 3:
					kind.setText(T.t(entry.optString("planet")));
					value.setText(T.t(entry.optString("duration")));
					note.setText(String.format(T.t("mid %s"), entry.optString("mid")));
					break;
				default:
					kind.setText(T.t(entry.optString("type")));
					value.setText(T.t(entry.optString("mag")));
					note.setText(String.format(T.t("Saros %s"), entry.optString("saros")));
					break;
			}

			setBackground(Theme.pressable(Theme.box(open ? Theme.FILL_SOFT : 0, 0), 0));
			setOnClickListener(v -> toggle(position));

			detail.setVisibility(open ? VISIBLE : GONE);
			detail.removeAllViews();
			if (open)
				fillDetail(detail, position, entry);
		}
	}

	private void toggle(int position)
	{
		expanded = (expanded == position) ? -1 : position;
		if (adapter != null)
			adapter.notifyDataSetChanged();

		if (expanded == position && needsContacts() && !contacts.containsKey(position))
			NativeBridge.request("eclipses.circumstances", TABS[tab] + "|" + position, payload ->
			{
				contacts.put(position, payload.optJSONArray("rows"));
				if (adapter != null)
					adapter.notifyDataSetChanged();
			});
	}

	private boolean needsContacts()
	{
		return tab == 0 || tab == 2;
	}

	private void fillDetail(LinearLayout into, int position, final JSONObject entry)
	{
		final Context context = context();
		switch (tab)
		{
			case 1:
				into.addView(Widgets.valueRow(context, "Type", entry.optString("type")));
				into.addView(Widgets.valueRow(context, "Partial eclipse begins", entry.optString("c1")));
				into.addView(Widgets.valueRow(context, "Central eclipse begins", entry.optString("c2")));
				into.addView(Widgets.valueRow(context, "Maximum eclipse", entry.optString("max")));
				into.addView(Widgets.valueRow(context, "Eclipse magnitude", entry.optString("mag")));
				into.addView(Widgets.valueRow(context, "Central eclipse ends", entry.optString("c3")));
				into.addView(Widgets.valueRow(context, "Partial eclipse ends", entry.optString("c4")));
				into.addView(Widgets.valueRow(context, "Duration", entry.optString("duration")));
				break;
			case 2:
				into.addView(Widgets.valueRow(context, "Saros", entry.optString("saros")));
				into.addView(Widgets.valueRow(context, "Type", entry.optString("type")));
				into.addView(Widgets.valueRow(context, "Gamma", entry.optString("gamma")));
				into.addView(Widgets.valueRow(context, "Penumbral magnitude", entry.optString("pmag")));
				into.addView(Widgets.valueRow(context, "Umbral magnitude", entry.optString("umag")));
				into.addView(Widgets.valueRow(context, "Visibility", entry.optString("visibility")));
				into.addView(note(entry.optString("visibilityNote")));
				break;
			case 3:
				into.addView(Widgets.valueRow(context, "Planet", entry.optString("planet")));
				into.addView(Widgets.valueRow(context, "Exterior ingress", entry.optString("c1")));
				into.addView(Widgets.valueRow(context, "Interior ingress", entry.optString("c2")));
				into.addView(Widgets.valueRow(context, "Mid-transit", entry.optString("mid")));
				into.addView(Widgets.valueRow(context, "Angular distance", entry.optString("sep")));
				into.addView(Widgets.valueRow(context, "Interior egress", entry.optString("c3")));
				into.addView(Widgets.valueRow(context, "Exterior egress", entry.optString("c4")));
				into.addView(Widgets.valueRow(context, "Duration", entry.optString("duration")));
				into.addView(Widgets.valueRow(context, "Observable duration", entry.optString("observable")));
				break;
			default:
				into.addView(Widgets.valueRow(context, "Saros", entry.optString("saros")));
				into.addView(Widgets.valueRow(context, "Type", entry.optString("type")));
				into.addView(Widgets.valueRow(context, "Gamma", entry.optString("gamma")));
				into.addView(Widgets.valueRow(context, "Eclipse magnitude", entry.optString("mag")));
				into.addView(Widgets.valueRow(context, "Latitude", entry.optString("lat")));
				into.addView(Widgets.valueRow(context, "Longitude", entry.optString("lng")));
				into.addView(Widgets.valueRow(context, "Sun's altitude", entry.optString("alt")));
				into.addView(Widgets.valueRow(context, "Path width", entry.optString("width")));
				into.addView(Widgets.valueRow(context, "Central duration", entry.optString("duration")));
				break;
		}

		if (needsContacts())
			fillContacts(into, position);

		final String label = tab == 3 ? "Go to this transit" : "Go to this eclipse";
		final TextView button = Widgets.primaryButton(context, label, v ->
		{
			NativeBridge.send("eclipses.goto", entry.optDouble("jd", 0.) + "|" + target(entry));
			host.overlay().close(host.sheet());
		});
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Theme.dp(18), Theme.dp(8), Theme.dp(18), 0);
		into.addView(button, params);
	}

	private String target(JSONObject entry)
	{
		if (tab == 2)
			return "Moon";
		if (tab == 3)
			return entry.optString("select", "Sun");
		return "Sun";
	}

	private void fillContacts(LinearLayout into, int position)
	{
		final Context context = context();
		final JSONArray list = contacts.get(position);
		if (list == null)
		{
			into.addView(note("Working out the contact times…"));
			return;
		}
		for (int i = 0; i < list.length(); ++i)
		{
			final JSONObject contact = list.optJSONObject(i);
			if (contact == null)
				continue;
			into.addView(Widgets.section(context, contact.optString("label")));
			into.addView(Widgets.valueRow(context, "Time", contact.optString("when")));
			if (tab == 2)
			{
				into.addView(Widgets.valueRow(context, "Altitude", contact.optString("alt")));
				into.addView(Widgets.valueRow(context, "Azimuth", contact.optString("az")));
				into.addView(Widgets.valueRow(context, "Moon in zenith at", contact.optString("lat")
						+ "  " + contact.optString("lng")));
				into.addView(Widgets.valueRow(context, "Position angle", contact.optString("pa")));
				into.addView(Widgets.valueRow(context, "Axis distance", contact.optString("dist")));
				if (!contact.optBoolean("visible", true))
					into.addView(note("Below the horizon here."));
			}
			else
			{
				into.addView(Widgets.valueRow(context, "Type", contact.optString("type")));
				into.addView(Widgets.valueRow(context, "Latitude", contact.optString("lat")));
				into.addView(Widgets.valueRow(context, "Longitude", contact.optString("lng")));
				into.addView(Widgets.valueRow(context, "Path width", contact.optString("width")));
				into.addView(Widgets.valueRow(context, "Duration", contact.optString("duration")));
			}
		}
	}
}
