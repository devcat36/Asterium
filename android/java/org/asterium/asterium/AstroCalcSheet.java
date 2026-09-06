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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AstroCalcSheet extends Sheet
{
	private static final String[] PAGES = {
		"Positions", "Ephemeris", "RTS", "Phenomena", "Graphs", "WUT", "PC", "Eclipses", "Almanac"
	};

	private static final int STEP_SUN_AT_ALTITUDE = 41;
	private static final int STEP_OPPOSITION = 42;

	private final LinearLayout tabs;
	private final FrameLayout host;
	private final TextView summary;
	private LinearLayout buttons;

	private final AstroCalcPage[] pages = new AstroCalcPage[PAGES.length];
	private int page = 0;

	private boolean heliocentric = false;

	private JSONObject seen = new JSONObject();
	private final List<JSONObject> rows = new ArrayList<>();
	private BaseAdapter adapter;
	private int expanded = -1;
	private TextView categoryValue;
	private View horizontalRow;

	private TextView seenHeading;
	private ListView seenList;
	private ValueBinding seenMag;

	private JSONObject hec = new JSONObject();
	private HecPlot plot;
	private LinearLayout hecRows;
	private View minorRow, cometsRow;
	private ValueBinding hecMag;
	private String marked = "";

	private JSONObject eph = new JSONObject();
	private final List<JSONObject> ephRows = new ArrayList<>();
	private BaseAdapter ephAdapter;
	private int ephExpanded = -1;

	private String start = "";
	private TextView bodyValue, secondBodyValue, stepValue, startDateValue, startTimeValue;
	private android.widget.EditText durationField;
	private LinearLayout unitChips;
	private View nakedEyeRow, ephHorizontalRow, boundlessRow, oppositionRow;
	private TextView oppositionValue;
	private TextView ephHeading;
	private ListView ephList;
	private boolean scrollToNextEphemeris;
	private ValueBinding sunAltitude;
	private LinearLayout crossingChips;
	private View markersRow, lineRow, datesRow, magnitudesRow;

	private final android.widget.CompoundButton.OnCheckedChangeListener horizontalListener =
			(button, checked) -> set("flag_horizontal_coordinates", checked ? "true" : "false");
	private final android.widget.CompoundButton.OnCheckedChangeListener minorListener =
			(button, checked) -> set("flag_hec_minor_planets", checked ? "true" : "false");
	private final android.widget.CompoundButton.OnCheckedChangeListener cometsListener =
			(button, checked) -> set("flag_hec_bright_comets", checked ? "true" : "false");
	private final android.widget.CompoundButton.OnCheckedChangeListener nakedEyeListener =
			(button, checked) -> setEphemeris("ephemeris_nakedeye_planets", checked ? "true" : "false");
	private final android.widget.CompoundButton.OnCheckedChangeListener ephHorizontalListener =
			(button, checked) -> setEphemeris("flag_ephemeris_horizontal_coordinates", checked ? "true" : "false");
	private final android.widget.CompoundButton.OnCheckedChangeListener boundlessListener =
			(button, checked) -> setEphemeris("flag_ephemeris_ignore_date_test", checked ? "true" : "false");

	AstroCalcSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "astrocalc", "Astronomical calculations");

		tabs = addTabRail();
		buildTabs();

		host = new FrameLayout(context);
		setBodyView(host);

		final LinearLayout footer = addFooter();
		summary = Theme.text(context, "", 11, Theme.TEXT_DIM, false);
		summary.setLineSpacing(Theme.dp(2), 1f);
		footer.addView(summary, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		buttons = footer;

		showPage(recallTab(PAGES.length));
	}

	private TextView addButton(String label, boolean primary, final Runnable action)
	{
		final TextView button = primary
				? Widgets.primaryButton(getContext(), label, v -> action.run())
				: Widgets.secondaryButton(getContext(), label, v -> action.run());
		Theme.padding(button, 14, 0, 14, 0);
		button.setSingleLine(true);
		button.setEllipsize(android.text.TextUtils.TruncateAt.END);
		button.setMaxWidth(Theme.dp(132));
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = Theme.dp(8);
		buttons.addView(button, params);
		return button;
	}

	private AstroCalcPage page(int index)
	{
		if (pages[index] == null)
			pages[index] = AstroCalcPages.create(index);
		return pages[index];
	}

	private final AstroCalcPage.Host pageHost = new AstroCalcPage.Host()
	{
		@Override public Context context() { return getContext(); }
		@Override public Overlay overlay() { return AstroCalcSheet.this.overlay(); }
		@Override public Sheet sheet() { return AstroCalcSheet.this; }
		@Override public void setSummary(String text) { summary.setText(T.t(text == null ? "" : text)); }
		@Override public TextView addButton(String label, boolean primary, Runnable action)
		{
			return AstroCalcSheet.this.addButton(label, primary, action);
		}
	};

	private void buildTabs()
	{
		tabs.removeAllViews();
		for (int i = 0; i < PAGES.length; ++i)
		{
			final int index = i;
			tabs.addView(Widgets.tabChip(getContext(), PAGES[i], i == page, v -> showPage(index)));
		}
	}

	private void showPage(int index)
	{
		page = index;
		rememberTab(index);
		buildTabs();
		host.removeAllViews();
		expanded = -1;
		ephExpanded = -1;

		while (buttons.getChildCount() > 1)
			buttons.removeViewAt(buttons.getChildCount() - 1);
		summary.setText(T.t(""));

		if (index > 1)
		{
			final AstroCalcPage built = page(index);
			built.attach(pageHost);
			host.addView(built.build());
			built.onShow();
			return;
		}

		if (index == 1)
		{
			addButton("Clear", false, () -> { NativeBridge.send("ephemeris.clear"); refresh(); });
			addButton("Calculate", true, this::calculate);
			host.addView(buildEphemerisPage());
		}
		else
		{
			addButton("Update", false, this::refresh);
			host.addView(heliocentric ? buildHecPage() : buildSeenPage());
		}
		refresh();
	}

	private void refresh()
	{
		if (page == 1)
			NativeBridge.request("ephemeris", "", payload -> { readEphemeris(payload); showEphemeris(); });
		else if (page != 0)
			return;
		else if (heliocentric)
			NativeBridge.request("positions.hec", "", payload -> { hec = payload; showHec(); });
		else
			NativeBridge.request("positions", "", payload -> { seen = payload; showSeen(); });
	}

	private void readEphemeris(JSONObject payload)
	{
		eph = payload;

		if (start.isEmpty())
			start = payload.optString("start");
	}

	private void calculate()
	{
		summary.setText(T.t("Calculating…"));
		scrollToNextEphemeris = true;
		NativeBridge.request("ephemeris.generate", start,
				payload -> { readEphemeris(payload); showEphemeris(); });
	}

	private void set(String key, String value)
	{
		NativeBridge.send("positions.set", key + "=" + value);
		refresh();
	}

	private View subTabs()
	{
		final LinearLayout row = new LinearLayout(getContext());
		row.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(row, 16, 12, 16, 4);
		row.addView(Widgets.filterChip(getContext(), "Seen now", !heliocentric, v ->
		{
			if (!heliocentric)
				return;
			heliocentric = false;
			showPage(0);
		}));
		row.addView(Widgets.filterChip(getContext(), "Major planets", heliocentric, v ->
		{
			if (heliocentric)
				return;
			heliocentric = true;
			showPage(0);
		}));
		return row;
	}

	private View buildSeenPage()
	{
		final Context context = getContext();
		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);
		column.addView(subTabs());

		adapter = new BaseAdapter()
		{
			@Override public int getCount() { return rows.size(); }
			@Override public Object getItem(int position) { return rows.get(position); }
			@Override public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View recycled, ViewGroup parent)
			{
				final DetailRow row = recycled instanceof DetailRow
						? (DetailRow) recycled : new DetailRow(parent.getContext());
				final JSONObject entry = rows.get(position);
				row.bind(entry.optString("name"), entry.optString("type"), entry.optString("mag"),
						entry.optString("lng") + "  " + entry.optString("lat"),
						position == expanded, () -> toggle(position),
						into -> fillPositionDetail(into, entry, row));
				return row;
			}
		};

		final ListView list = new ListView(context);

		list.addHeaderView(seenFilters(), null, false);
		seenHeading = Widgets.section(context, "Results");
		seenHeading.setVisibility(GONE);
		list.addHeaderView(seenHeading, null, false);
		list.setAdapter(adapter);
		seenList = list;
		list.setDivider(new android.graphics.drawable.ColorDrawable(Theme.HAIRLINE));
		list.setDividerHeight(Math.max(1, Theme.dp(1)));
		list.setDrawSelectorOnTop(false);
		list.setSelector(new android.graphics.drawable.ColorDrawable(0));
		column.addView(list, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
		return column;
	}

	private void toggle(int position)
	{
		expanded = (expanded == position) ? -1 : position;
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}

	private View seenFilters()
	{
		final Context context = getContext();
		final LinearLayout group = new LinearLayout(context);
		group.setOrientation(LinearLayout.VERTICAL);

		categoryValue = Theme.text(context, "", 13, Theme.ACCENT, false);
		group.addView(Widgets.pickerRow(context, "Category", categoryValue, v -> pickCategory()));

		seenMag = magRow(context, "Up to mag.", 25.0, "celestial_magnitude_limit",
				() -> seen.optDouble("mag", 6.0));
		group.addView(seenMag.view);

		horizontalRow = Widgets.switchRow(context, "Horizontal coordinates",
				"Azimuth and altitude rather than right ascension and declination", false,
				horizontalListener);
		group.addView(horizontalRow);
		group.addView(Widgets.hairline(context));
		return group;
	}

	private ValueBinding magRow(Context context, String label, final double max, final String key,
	                            final ValueSource current)
	{
		return sliderRow(context, label, 0., max, current, v -> set(key, v));
	}

	private interface Commit { void with(String value); }

	private ValueBinding sliderRow(Context context, String label, final double min, final double max,
	                               final ValueSource current, final Commit commit)
	{
		final LinearLayout group = new LinearLayout(context);
		group.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(group, 16, 10, 16, 6);

		final LinearLayout head = new LinearLayout(context);
		head.setOrientation(LinearLayout.HORIZONTAL);
		head.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		final TextView value = Theme.text(context, "", 14, Theme.ACCENT, true);
		head.addView(value);
		group.addView(head);

		final SeekBar bar = Widgets.slider(context, (int) Math.round((max - min) * 10.), 0);
		bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			public void onProgressChanged(SeekBar seek, int progress, boolean fromUser)
			{
				value.setText(T.t(String.format(Locale.getDefault(), "%.1f", min + progress / 10.)));
			}
			public void onStartTrackingTouch(SeekBar seek) {}
			public void onStopTrackingTouch(SeekBar seek)
			{
				commit.with(String.format(Locale.US, "%.2f", min + seek.getProgress() / 10.));
			}
		});
		group.addView(bar);
		return new ValueBinding(group, bar, value, current, min);
	}

	private interface ValueSource { double get(); }

	private static final class ValueBinding
	{
		final View view;
		final SeekBar bar;
		final TextView value;
		final ValueSource source;
		final double min;

		ValueBinding(View view, SeekBar bar, TextView value, ValueSource source, double min)
		{
			this.view = view;
			this.bar = bar;
			this.value = value;
			this.source = source;
			this.min = min;
		}

		void apply()
		{
			final double setting = source.get();
			bar.setProgress((int) Math.round((setting - min) * 10.));
			value.setText(T.t(String.format(Locale.getDefault(), "%.1f", setting)));
		}
	}

	private void showSeen()
	{
		if (page != 0 || heliocentric)
			return;
		rows.clear();
		final JSONArray list = seen.optJSONArray("positions");
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject row = list.optJSONObject(i);
			if (row != null)
				rows.add(row);
		}
		expanded = -1;
		if (adapter != null)
			adapter.notifyDataSetChanged();
		if (seenHeading != null)
			seenHeading.setVisibility(rows.isEmpty() ? GONE : VISIBLE);

		if (categoryValue != null)
			categoryValue.setText(T.t(categoryName(seen.optString("category", "200"))));
		applySwitch(horizontalRow, seen.optBoolean("horizontal", false), horizontalListener);
		if (seenMag != null)
			seenMag.apply();

		summary.setText(T.t(rows.isEmpty()
				? "Nothing in this category is above the horizon.\n" + when(seen)
				: rows.size() + (rows.size() == 1 ? " object\n" : " objects\n") + when(seen)));
	}

	private String categoryName(String id)
	{
		final JSONArray list = seen.optJSONArray("categories");
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject item = list.optJSONObject(i);
			if (item != null && id.equals(item.optString("id")))
				return item.optString("name");
		}
		return id;
	}

	private void pickCategory()
	{
		final JSONArray list = seen.optJSONArray("categories");
		final List<String> ids = new ArrayList<>(), labels = new ArrayList<>();
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject item = list.optJSONObject(i);
			if (item == null)
				continue;
			ids.add(item.optString("id"));
			labels.add(item.optString("name"));
		}
		final PickerSheet picker = new PickerSheet(getContext(), overlay(), "Category",
				seen.optString("category", "200"), id -> set("celestial_category", id));
		overlay().open(picker);
		picker.setItems(ids, labels);
	}

	private interface Detailer
	{
		void fill(LinearLayout into);
	}

	private final class DetailRow extends LinearLayout
	{
		private final TextView name, type, mag, coords;
		private final LinearLayout detail;

		DetailRow(Context context)
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
			name = Theme.text(context, "", 14, Theme.TEXT, false);
			name.setSingleLine(true);
			name.setEllipsize(android.text.TextUtils.TruncateAt.END);
			type = Theme.text(context, "", 10, Theme.TEXT_DIM, false);
			type.setSingleLine(true);
			titles.addView(name);
			titles.addView(type);

			head.addView(titles, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

			final LinearLayout numbers = new LinearLayout(context);
			numbers.setOrientation(VERTICAL);
			mag = Theme.text(context, "", 14, Theme.ACCENT, true);
			mag.setGravity(Gravity.END);
			coords = Theme.text(context, "", 10, Theme.TEXT_DIM, true);
			coords.setGravity(Gravity.END);
			numbers.addView(mag);
			numbers.addView(coords);
			final LayoutParams numberParams = new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			numberParams.leftMargin = Theme.dp(10);
			head.addView(numbers, numberParams);
			addView(head);

			detail = new LinearLayout(context);
			detail.setOrientation(VERTICAL);
			detail.setBackgroundColor(Theme.FILL_FAINT);
			Theme.padding(detail, 0, 4, 0, 10);
			addView(detail);
		}

		void bind(String title, String sub, String value, String note, boolean open,
		          final Runnable click, Detailer detailer)
		{
			name.setText(T.t(title));
			type.setText(T.t(sub));
			mag.setText(T.t(value));
			coords.setText(T.t(note));
			setBackground(Theme.pressable(Theme.box(open ? Theme.FILL_SOFT : 0, 0), 0));
			setOnClickListener(v -> click.run());

			detail.setVisibility(open ? VISIBLE : GONE);
			detail.removeAllViews();
			if (open)
				detailer.fill(detail);
		}

		void action(LinearLayout into, String label, final Runnable run)
		{
			final TextView button = Widgets.primaryButton(getContext(), label, v -> run.run());
			final LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(Theme.dp(18), Theme.dp(8), Theme.dp(18), 0);
			into.addView(button, params);
		}
	}

	private void fillPositionDetail(LinearLayout into, final JSONObject entry, DetailRow row)
	{
		final Context context = getContext();
		into.addView(Widgets.valueRow(context, seen.optString("lngLabel"), entry.optString("lng")));
		into.addView(Widgets.valueRow(context, seen.optString("latLabel"), entry.optString("lat")));
		final String sizeLabel = seen.optString("sizeLabel");
		if (!sizeLabel.isEmpty())
			into.addView(Widgets.valueRow(context, sizeLabel, entry.optString("size")));
		into.addView(Widgets.valueRow(context, seen.optString("extraLabel"), entry.optString("extra")));
		into.addView(Widgets.valueRow(context, "Transit", entry.optString("transit")));
		into.addView(Widgets.valueRow(context, "Elev.", entry.optString("elev")));
		into.addView(Widgets.valueRow(context, "Elong.", entry.optString("elong")));
		row.action(into, "Show in sky", () ->
		{
			NativeBridge.send("select", entry.optString("select"));
			overlay().close(AstroCalcSheet.this);
		});
	}

	private View buildHecPage()
	{
		final Context context = getContext();
		final ScrollView scroll = new ScrollView(context);
		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);
		scroll.addView(column);

		column.addView(subTabs());
		column.addView(Widgets.note(context, "Heliocentric ecliptic positions of the major planets."));

		plot = new HecPlot(context);
		final LinearLayout.LayoutParams plotParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		plotParams.setMargins(Theme.dp(12), Theme.dp(4), Theme.dp(12), Theme.dp(8));
		column.addView(plot, plotParams);

		hecRows = new LinearLayout(context);
		hecRows.setOrientation(LinearLayout.VERTICAL);
		column.addView(hecRows);

		column.addView(Widgets.hairline(context));
		minorRow = Widgets.switchRow(context, "Include selected minor planets",
				"Pluto, Ceres, Pallas, Juno and Vesta", false, minorListener);
		column.addView(minorRow);

		cometsRow = Widgets.switchRow(context, "Include bright comets", null, false, cometsListener);
		column.addView(cometsRow);
		hecMag = magRow(context, "Comets up to mag.", 15.0, "hec_magnitude_limit",
				() -> hec.optDouble("mag", 9.0));
		column.addView(hecMag.view);
		return scroll;
	}

	private void showHec()
	{
		if (page != 0 || !heliocentric)
			return;
		final Context context = getContext();
		final JSONArray list = hec.optJSONArray("hec");
		final boolean minorPlanets = hec.optBoolean("minorPlanets", false);
		if (plot != null)
			plot.setData(list, minorPlanets, marked);
		if (hecRows == null)
			return;

		hecRows.removeAllViews();
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject entry = list.optJSONObject(i);
			if (entry != null)
				hecRows.addView(hecRow(context, entry));
		}
		applySwitch(minorRow, minorPlanets, minorListener);
		applySwitch(cometsRow, hec.optBoolean("brightComets", false), cometsListener);
		if (hecMag != null)
			hecMag.apply();

		summary.setText(String.format(T.t("%d bodies"), list == null ? 0 : list.length())
				+ "\n" + when(hec));
	}

	private View hecRow(Context context, final JSONObject entry)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(44));
		Theme.padding(row, 16, 6, 16, 6);
		final boolean isMarked = entry.optString("name").equals(marked);
		row.setBackground(Theme.pressable(Theme.box(isMarked ? Theme.FILL_SOFT : 0, 8), 8));
		row.setOnClickListener(v ->
		{
			marked = entry.optString("name");
			NativeBridge.send("select", entry.optString("select"));
			showHec();
		});

		final TextView symbol = Theme.text(context, entry.optString("symbol"), 15,
		                                   isMarked ? Theme.ACCENT : Theme.TEXT_DIM, false);
		symbol.setGravity(Gravity.CENTER);
		row.addView(symbol, new LinearLayout.LayoutParams(Theme.dp(24), LinearLayout.LayoutParams.WRAP_CONTENT));

		final TextView name = Theme.text(context, entry.optString("name"), 14,
		                                 isMarked ? Theme.ACCENT : Theme.TEXT, false);
		name.setSingleLine(true);
		name.setEllipsize(android.text.TextUtils.TruncateAt.END);
		final LinearLayout.LayoutParams nameParams =
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		nameParams.leftMargin = Theme.dp(8);
		row.addView(name, nameParams);

		final LinearLayout numbers = new LinearLayout(context);
		numbers.setOrientation(LinearLayout.VERTICAL);
		final TextView distance = Theme.text(context, entry.optString("dist"), 13, Theme.TEXT, true);
		distance.setGravity(Gravity.END);
		final TextView coords = Theme.text(context,
				entry.optString("lat") + "  " + entry.optString("lng"), 10, Theme.TEXT_DIM, true);
		coords.setGravity(Gravity.END);
		numbers.addView(distance);
		numbers.addView(coords);
		row.addView(numbers);
		return row;
	}

	private View buildEphemerisPage()
	{
		final Context context = getContext();
		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);

		ephAdapter = new BaseAdapter()
		{
			@Override public int getCount() { return ephRows.size(); }
			@Override public Object getItem(int position) { return ephRows.get(position); }
			@Override public long getItemId(int position) { return position; }

			@Override
			public View getView(int position, View recycled, ViewGroup parent)
			{
				final DetailRow row = recycled instanceof DetailRow
						? (DetailRow) recycled : new DetailRow(parent.getContext());
				final JSONObject entry = ephRows.get(position);
				row.bind(entry.optString("when"), entry.optString("name"), entry.optString("mag"),
						entry.optString("lng") + "  " + entry.optString("lat"),
						position == ephExpanded, () -> toggleEphemeris(position),
						into -> fillEphemerisDetail(into, entry, row));
				return row;
			}
		};

		final ListView list = new ListView(context);
		list.addHeaderView(ephemerisControls(), null, false);
		ephHeading = Widgets.section(context, "Results");
		ephHeading.setVisibility(GONE);
		list.addHeaderView(ephHeading, null, false);
		list.setAdapter(ephAdapter);
		ephList = list;
		list.setDivider(new android.graphics.drawable.ColorDrawable(Theme.HAIRLINE));
		list.setDividerHeight(Math.max(1, Theme.dp(1)));
		list.setDrawSelectorOnTop(false);
		list.setSelector(new android.graphics.drawable.ColorDrawable(0));
		column.addView(list, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
		return column;
	}

	private void toggleEphemeris(int position)
	{
		ephExpanded = (ephExpanded == position) ? -1 : position;
		if (ephAdapter != null)
			ephAdapter.notifyDataSetChanged();
	}

	private View ephemerisControls()
	{
		final Context context = getContext();
		final LinearLayout group = new LinearLayout(context);
		group.setOrientation(LinearLayout.VERTICAL);

		group.addView(Widgets.section(context, "Celestial body"));
		bodyValue = Theme.text(context, "", 13, Theme.ACCENT, false);
		group.addView(Widgets.pickerRow(context, "Object", bodyValue,
				v -> pickBody("ephemeris_celestial_body", "Object", eph.optString("body"), false)));
		secondBodyValue = Theme.text(context, "", 13, Theme.ACCENT, false);
		group.addView(Widgets.pickerRow(context, "And also", secondBodyValue,
				v -> pickBody("ephemeris_second_celestial_body", "Second object",
						eph.optString("secondBody"), true)));
		nakedEyeRow = Widgets.switchRow(context, "All naked-eye planets",
				"Mercury, Venus, Mars, Jupiter and Saturn at once. Earth only.", false, nakedEyeListener);
		group.addView(nakedEyeRow);

		group.addView(Widgets.section(context, "Time span"));
		startDateValue = Theme.text(context, "", 13, Theme.ACCENT, false);
		group.addView(Widgets.pickerRow(context, "Starting", startDateValue, v -> pickStartDate()));
		startTimeValue = Theme.text(context, "", 13, Theme.ACCENT, false);
		group.addView(Widgets.pickerRow(context, "At", startTimeValue, v -> pickStartTime()));

		final LinearLayout duration = new LinearLayout(context);
		duration.setOrientation(LinearLayout.HORIZONTAL);
		duration.setGravity(Gravity.CENTER_VERTICAL);
		Theme.padding(duration, 16, 8, 16, 4);
		duration.addView(Theme.text(context, "Lasting", 14, Theme.TEXT_CHIP, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		durationField = Widgets.field(context, "Duration", "1", true);
		durationField.setGravity(Gravity.END);
		durationField.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
		durationField.setOnFocusChangeListener((v, focused) -> { if (!focused) commitDuration(); });
		durationField.setOnEditorActionListener((v, action, event) -> { commitDuration(); return false; });
		duration.addView(durationField, new LinearLayout.LayoutParams(Theme.dp(96), Theme.dp(48)));
		group.addView(duration);

		unitChips = new LinearLayout(context);
		unitChips.setOrientation(LinearLayout.HORIZONTAL);
		final HorizontalScrollView unitScroll = new HorizontalScrollView(context);
		unitScroll.setHorizontalScrollBarEnabled(false);
		Theme.padding(unitChips, 16, 4, 16, 10);
		unitScroll.addView(unitChips);
		group.addView(unitScroll);

		stepValue = Theme.text(context, "", 13, Theme.ACCENT, false);
		group.addView(Widgets.pickerRow(context, "Every", stepValue, v -> pickStep()));

		sunAltitude = sliderRow(context, "Sun altitude", -18., 0.,
				() -> eph.optDouble("sunAltitude", -10.0),
				v -> setEphemeris("ephemeris_sun_altitude", v));
		group.addView(sunAltitude.view);
		crossingChips = new LinearLayout(context);
		crossingChips.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(crossingChips, 16, 0, 16, 10);
		group.addView(crossingChips);

		oppositionValue = Theme.text(context, "", 13, Theme.ACCENT, false);
		oppositionRow = Widgets.pickerRow(context, "Opposition of", oppositionValue,
				v -> pickBody("ephemeris_opposition_planet", "Opposition of",
						eph.optString("oppositionPlanet"), false));
		group.addView(oppositionRow);

		group.addView(Widgets.section(context, "Table"));
		ephHorizontalRow = Widgets.switchRow(context, "Horizontal coordinates",
				"Azimuth and altitude rather than right ascension and declination", false,
				ephHorizontalListener);
		group.addView(ephHorizontalRow);
		boundlessRow = Widgets.switchRow(context, "Boundless",
				"Allow results outside the recommended time range of the orbital elements",
				false, boundlessListener);
		group.addView(boundlessRow);

		group.addView(Widgets.section(context, "On the sky"));
		markersRow = Widgets.switchRow(context, "Markers", null, false,
				property("SolarSystem.ephemerisMarkersDisplayed"));
		group.addView(markersRow);
		lineRow = Widgets.switchRow(context, "Connecting line", null, false,
				property("SolarSystem.ephemerisLineDisplayed"));
		group.addView(lineRow);
		datesRow = Widgets.switchRow(context, "Dates", null, false,
				property("SolarSystem.ephemerisDatesDisplayed"));
		group.addView(datesRow);
		magnitudesRow = Widgets.switchRow(context, "Magnitudes", null, false,
				property("SolarSystem.ephemerisMagnitudesDisplayed"));
		group.addView(magnitudesRow);
		group.addView(Widgets.hairline(context));
		return group;
	}

	private android.widget.CompoundButton.OnCheckedChangeListener property(final String id)
	{
		return (button, checked) -> NativeBridge.send("prop.set", id + "=" + (checked ? "true" : "false"));
	}

	private void commitDuration()
	{
		final double typed = Widgets.number(durationField);
		if (Double.isNaN(typed) || typed < 1.)
			return;
		final int wanted = (int) Math.round(typed);
		if (wanted == eph.optInt("duration", 1))
			return;
		setEphemeris("ephemeris_time_duration", String.valueOf(wanted));
	}

	private void showEphemeris()
	{
		if (page != 1)
			return;
		ephRows.clear();
		final JSONArray list = eph.optJSONArray("rows");
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject row = list.optJSONObject(i);
			if (row != null)
				ephRows.add(row);
		}
		ephExpanded = -1;
		if (ephAdapter != null)
			ephAdapter.notifyDataSetChanged();
		if (ephHeading != null)
			ephHeading.setVisibility(ephRows.isEmpty() ? GONE : VISIBLE);
		if (scrollToNextEphemeris && !ephRows.isEmpty() && ephList != null)
		{
			final ListView table = ephList;
			table.post(() -> table.setSelectionFromTop(1, 0));
		}
		scrollToNextEphemeris = false;

		final boolean nakedEye = eph.optBoolean("nakedEye", false);
		if (bodyValue != null)
			bodyValue.setText(T.t(nakedEye ? "—" : bodyName(eph.optString("body"))));
		if (secondBodyValue != null)
			secondBodyValue.setText(T.t(nakedEye ? "—" : bodyName(eph.optString("secondBody"))));
		if (stepValue != null)
			stepValue.setText(T.t(listName(eph.optJSONArray("steps"), String.valueOf(eph.optInt("step", 6)))));
		if (startDateValue != null)
			startDateValue.setText(T.t(start.length() >= 10 ? start.substring(0, 10) : start));
		if (startTimeValue != null)
			startTimeValue.setText(T.t(start.length() >= 16 ? start.substring(11, 16) : ""));
		if (durationField != null && !durationField.hasFocus())
			durationField.setText(T.t(String.valueOf(eph.optInt("duration", 1))));
		buildUnitChips();

		final int step = eph.optInt("step", 6);
		final boolean atSunAltitude = step == STEP_SUN_AT_ALTITUDE;
		final boolean atOpposition = step == STEP_OPPOSITION;
		if (sunAltitude != null)
		{
			sunAltitude.view.setVisibility(atSunAltitude ? VISIBLE : GONE);
			if (atSunAltitude)
				sunAltitude.apply();
		}
		if (crossingChips != null)
		{
			crossingChips.setVisibility(atSunAltitude ? VISIBLE : GONE);
			if (atSunAltitude)
				buildCrossingChips();
		}
		if (oppositionRow != null)
		{
			oppositionRow.setVisibility(atOpposition ? VISIBLE : GONE);
			if (atOpposition)
				oppositionValue.setText(T.t(bodyName(eph.optString("oppositionPlanet"))));
		}

		enable(bodyValue, !nakedEye);
		enable(secondBodyValue, !nakedEye);
		if (nakedEyeRow != null)
			nakedEyeRow.setVisibility(eph.optBoolean("nakedEyeAllowed", true) ? VISIBLE : GONE);

		applySwitch(nakedEyeRow, nakedEye, nakedEyeListener);
		applySwitch(ephHorizontalRow, eph.optBoolean("horizontal", false), ephHorizontalListener);
		applySwitch(boundlessRow, eph.optBoolean("ignoreDateTest", true), boundlessListener);
		applySwitch(markersRow, eph.optBoolean("showMarkers", false),
				property("SolarSystem.ephemerisMarkersDisplayed"));
		applySwitch(lineRow, eph.optBoolean("showLine", false),
				property("SolarSystem.ephemerisLineDisplayed"));
		applySwitch(datesRow, eph.optBoolean("showDates", false),
				property("SolarSystem.ephemerisDatesDisplayed"));
		applySwitch(magnitudesRow, eph.optBoolean("showMagnitudes", false),
				property("SolarSystem.ephemerisMagnitudesDisplayed"));

		summary.setText(T.t(ephRows.isEmpty()
				? "Nothing computed yet."
				: ephRows.size() + (ephRows.size() == 1 ? " position" : " positions")));
	}

	private static void enable(TextView value, boolean on)
	{
		if (value == null)
			return;
		final View row = (View) value.getParent();
		row.setEnabled(on);
		row.setAlpha(on ? 1f : 0.4f);
	}

	private void buildCrossingChips()
	{
		crossingChips.removeAllViews();
		final boolean evening = eph.optBoolean("sunAltEvening", true);
		crossingChips.addView(Widgets.filterChip(getContext(), "Evening", evening,
				v -> setEphemeris("ephemeris_sun_altitude_evening", "0")));
		crossingChips.addView(Widgets.filterChip(getContext(), "Morning", !evening,
				v -> setEphemeris("ephemeris_sun_altitude_evening", "1")));
	}

	private void buildUnitChips()
	{
		if (unitChips == null)
			return;
		unitChips.removeAllViews();
		final JSONArray units = eph.optJSONArray("units");
		final String current = String.valueOf(eph.optInt("unit", 5));
		for (int i = 0; units != null && i < units.length(); ++i)
		{
			final JSONObject unit = units.optJSONObject(i);
			if (unit == null)
				continue;
			final String id = unit.optString("id");
			unitChips.addView(Widgets.filterChip(getContext(), unit.optString("name"),
					id.equals(current), v -> setEphemeris("ephemeris_time_unit", id)));
		}
	}

	private String bodyName(String id)
	{
		if (id == null || id.isEmpty() || "none".equals(id))
			return "—";
		return listName(eph.optJSONArray("bodies"), id);
	}

	private static String listName(JSONArray list, String id)
	{
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject item = list.optJSONObject(i);
			if (item != null && id.equals(item.optString("id")))
				return item.optString("name");
		}
		return id;
	}

	private void pickBody(final String key, String title, String current, final boolean withNone)
	{
		final JSONArray list = eph.optJSONArray("bodies");
		final List<String> ids = new ArrayList<>(), labels = new ArrayList<>();
		if (withNone)
		{
			ids.add("none");
			labels.add("—");
		}
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject item = list.optJSONObject(i);
			if (item == null)
				continue;
			ids.add(item.optString("id"));
			labels.add(item.optString("name"));
		}
		final PickerSheet picker = new PickerSheet(getContext(), overlay(), title, current,
				id -> setEphemeris(key, id));
		overlay().open(picker);
		picker.setItems(ids, labels);
	}

	private void pickStep()
	{
		final JSONArray list = eph.optJSONArray("steps");
		final List<String> ids = new ArrayList<>(), labels = new ArrayList<>();
		for (int i = 0; list != null && i < list.length(); ++i)
		{
			final JSONObject item = list.optJSONObject(i);
			if (item == null)
				continue;
			ids.add(item.optString("id"));
			labels.add(item.optString("name"));
		}
		final PickerSheet picker = new PickerSheet(getContext(), overlay(), "Time step",
				String.valueOf(eph.optInt("step", 6)), id -> setEphemeris("ephemeris_time_step", id));
		overlay().open(picker);
		picker.setItems(ids, labels);
	}

	private void pickStartDate()
	{
		final java.util.Calendar at = startCalendar();
		new android.app.DatePickerDialog(getContext(), (view, year, month, day) ->
		{
			start = String.format(Locale.US, "%04d-%02d-%02d%s", year, month + 1, day,
					start.length() >= 16 ? start.substring(10) : " 00:00");
			showEphemeris();
		}, at.get(java.util.Calendar.YEAR), at.get(java.util.Calendar.MONTH),
		   at.get(java.util.Calendar.DAY_OF_MONTH)).show();
	}

	private void pickStartTime()
	{
		final java.util.Calendar at = startCalendar();
		new android.app.TimePickerDialog(getContext(), (view, hour, minute) ->
		{
			start = (start.length() >= 10 ? start.substring(0, 10) : "2000-01-01")
					+ String.format(Locale.US, " %02d:%02d", hour, minute);
			showEphemeris();
		}, at.get(java.util.Calendar.HOUR_OF_DAY), at.get(java.util.Calendar.MINUTE), true).show();
	}

	private java.util.Calendar startCalendar()
	{
		final java.util.Calendar at = java.util.Calendar.getInstance();
		try
		{
			at.set(Integer.parseInt(start.substring(0, 4)),
					Integer.parseInt(start.substring(5, 7)) - 1,
					Integer.parseInt(start.substring(8, 10)),
					Integer.parseInt(start.substring(11, 13)),
					Integer.parseInt(start.substring(14, 16)));
		}
		catch (RuntimeException e)
		{
		}
		return at;
	}

	private void setEphemeris(String key, String value)
	{
		NativeBridge.send("ephemeris.set", key + "=" + value);
		refresh();
	}

	private void fillEphemerisDetail(LinearLayout into, final JSONObject entry, DetailRow row)
	{
		final Context context = getContext();
		into.addView(Widgets.valueRow(context, "Date and time", entry.optString("when")));
		into.addView(Widgets.valueRow(context, eph.optString("lngLabel"), entry.optString("lng")));
		into.addView(Widgets.valueRow(context, eph.optString("latLabel"), entry.optString("lat")));
		into.addView(Widgets.valueRow(context, "Mag.", entry.optString("mag")));
		into.addView(Widgets.valueRow(context, "Phase", entry.optString("phase")));
		into.addView(Widgets.valueRow(context, eph.optString("distLabel"), entry.optString("dist")));
		into.addView(Widgets.valueRow(context, "Elong.", entry.optString("elong")));
		row.action(into, "Go to this moment", () ->
		{
			NativeBridge.send("ephemeris.goto", String.valueOf(entry.optInt("index", -1)));
			overlay().close(AstroCalcSheet.this);
		});
	}

	private String when(JSONObject payload)
	{
		final String at = payload.optString("when");
		return at.isEmpty() ? "" : "Positions on " + at;
	}

	private static void applySwitch(View row, boolean checked,
	                                android.widget.CompoundButton.OnCheckedChangeListener listener)
	{
		Widgets.restate(row == null ? null : Widgets.switchIn(row), checked, listener);
	}

	private static final class HecPlot extends View
	{
		private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
		private JSONArray data;
		private boolean minorPlanets;
		private String marked = "";

		HecPlot(Context context)
		{
			super(context);
			grid.setStyle(Paint.Style.STROKE);
			grid.setStrokeWidth(Math.max(1, Theme.dp(1)));
			grid.setColor(0x40FFFFFF);
			label.setColor(Theme.TEXT_DIM);
			label.setTextSize(Theme.dp(9));
			label.setTypeface(Theme.sans());
			dot.setStyle(Paint.Style.FILL);
		}

		void setData(JSONArray data, boolean minorPlanets, String marked)
		{
			this.data = data;
			this.minorPlanets = minorPlanets;
			this.marked = marked == null ? "" : marked;
			invalidate();
		}

		@Override
		protected void onMeasure(int widthSpec, int heightSpec)
		{
			final int size = MeasureSpec.getSize(widthSpec);
			setMeasuredDimension(size, size);
		}

		private float radius(double au, float outer)
		{
			final double span = Math.log(minorPlanets ? 52. : 32.) + 1.5;
			final double f = (Math.log(au) + 1.5) / span;
			return (float) Math.max(0., Math.min(1., f)) * outer;
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			final float cx = getWidth() / 2f;
			final float cy = getHeight() / 2f;
			final float outer = Math.min(cx, cy) - Theme.dp(20);
			if (outer <= 0f)
				return;

			final double[] rings = minorPlanets
					? new double[] { 0.5, 1, 2, 5, 10, 20, 40 }
					: new double[] { 0.5, 1, 2, 5, 10, 20, 30 };
			for (double au : rings)
			{
				final float r = radius(au, outer);
				canvas.drawCircle(cx, cy, r, grid);
				label.setTextAlign(Paint.Align.LEFT);
				canvas.drawText(trim(au), cx + Theme.dp(3), cy - r + Theme.dp(4), label);
			}
			canvas.drawCircle(cx, cy, outer, grid);

			label.setTextAlign(Paint.Align.CENTER);
			for (int deg = 0; deg < 360; deg += 30)
			{
				final double a = Math.toRadians(deg);
				final float sx = (float) (cx - Math.sin(a) * outer);
				final float sy = (float) (cy - Math.cos(a) * outer);
				canvas.drawLine(cx, cy, sx, sy, grid);
				final float lx = (float) (cx - Math.sin(a) * (outer + Theme.dp(11)));
				final float ly = (float) (cy - Math.cos(a) * (outer + Theme.dp(11))) + Theme.dp(3);
				canvas.drawText(deg + "°", lx, ly, label);
			}

			for (int i = 0; data != null && i < data.length(); ++i)
			{
				final JSONObject entry = data.optJSONObject(i);
				if (entry == null)
					continue;
				final double au = entry.optDouble("distAU", Double.NaN);
				if (Double.isNaN(au) || au <= 0.)
					continue;
				final double a = Math.toRadians(entry.optDouble("lonDeg", 0.));
				final float r = radius(au, outer);
				final float x = (float) (cx - Math.sin(a) * r);
				final float y = (float) (cy - Math.cos(a) * r);
				final boolean isMarked = entry.optString("name").equals(marked);
				dot.setColor(isMarked ? 0xFF7CFC7C : 0xFF3FD8E8);
				canvas.drawCircle(x, y, Theme.dp(isMarked ? 6 : 4), dot);
				if (isMarked)
				{
					label.setColor(0xFF7CFC7C);

					final boolean high = y < cy - outer * 0.75f;
					canvas.drawText(entry.optString("name"), x,
					                high ? y + Theme.dp(16) : y - Theme.dp(10), label);
					label.setColor(Theme.TEXT_DIM);
				}
			}

			dot.setColor(0xFFFFD34D);
			canvas.drawCircle(cx, cy, Theme.dp(5), dot);
			dot.setColor(0xFFE04A2F);
			dot.setStyle(Paint.Style.STROKE);
			dot.setStrokeWidth(Math.max(1, Theme.dp(1)));
			canvas.drawCircle(cx, cy, Theme.dp(5), dot);
			dot.setStyle(Paint.Style.FILL);
		}

		private static String trim(double au)
		{
			return au < 1. ? String.valueOf(au) : String.valueOf((int) au);
		}
	}
}
