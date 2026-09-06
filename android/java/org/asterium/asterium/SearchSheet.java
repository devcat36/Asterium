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
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class SearchSheet extends Sheet
{
	private static final String GREEK = "αβγδεζηθικλμνξοπρστυφχψω";
	private static final String[] PAGES = { "Object", "SIMBAD", "Position", "Lists", "Options" };
	private static final int OBJECT = 0, SIMBAD = 1, POSITION = 2, LISTS = 3, OPTIONS = 4;

	private static final String[] SYSTEM_IDS = {
			"equatorialJ2000", "equatorial", "horizontal",
			"galactic", "supergalactic", "ecliptic", "eclipticJ2000" };
	private static final String[] SYSTEM_NAMES = {
			"Equatorial (J2000.0)", "Equatorial", "Horizontal",
			"Galactic", "Supergalactic", "Ecliptic", "Ecliptic (J2000.0)" };

	private static final String[] SERVER_IDS = {
			"https://simbad.u-strasbg.fr/",
			"https://simbad.cfa.harvard.edu/",
			"https://simbad.cds.unistra.fr/" };
	private static final String[] SERVER_NAMES = {
			"University of Strasbourg (France)",
			"Harvard-Smithsonian Center for Astrophysics (USA)",
			"Strasbourg astronomical Data Center (France)" };

	private final LinearLayout tabs;
	private final LinearLayout page;
	private final Handler debounce = new Handler(Looper.getMainLooper());
	private Runnable pending;

	private JSONObject options = new JSONObject();
	private int shown = OBJECT;

	private EditText field;
	private LinearLayout results;
	private TextView emptyNote;
	private int serial = 0;
	private String query = "";

	private EditText xField, yField;

	private final List<String> moduleIds = new ArrayList<>();
	private final List<String> moduleNames = new ArrayList<>();
	private String module = "";
	private String listFilter = "";
	private LinearLayout listRows;
	private TextView listCount;

	SearchSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "search", "Search");
		tabs = addTabRail();
		page = new LinearLayout(context);
		page.setOrientation(LinearLayout.VERTICAL);
		body.addView(page);
		buildTabs();
		NativeBridge.request("search.options", "", payload ->
		{
			options = payload;
			showPage(recallTab(PAGES.length));
		});
	}

	private void buildTabs()
	{
		tabs.removeAllViews();
		for (int i = 0; i < PAGES.length; ++i)
		{
			final int index = i;
			tabs.addView(Widgets.tabChip(getContext(), PAGES[i], i == shown, v -> showPage(index)));
		}
	}

	private void showPage(int index)
	{
		shown = index;
		rememberTab(index);
		++serial;
		if (pending != null)
		{
			debounce.removeCallbacks(pending);
			pending = null;
		}
		buildTabs();
		hideKeyboard();
		page.removeAllViews();
		field = null;
		results = null;
		listRows = null;
		NativeBridge.send("search.marker", index == POSITION ? "1" : "0");
		switch (index)
		{
			case SIMBAD:   buildSimbad();   break;
			case POSITION: buildPosition(); break;
			case LISTS:    buildLists();    break;
			case OPTIONS:  buildOptions();  break;
			default:       buildObject();   break;
		}
	}

	@Override
	void onShown()
	{
		focusField(field);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		NativeBridge.send("search.marker", "0");
	}

	private void hideKeyboard()
	{
		final android.view.inputmethod.InputMethodManager ime =
				(android.view.inputmethod.InputMethodManager)
						getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (ime != null && getWindowToken() != null)
			ime.hideSoftInputFromWindow(getWindowToken(), 0);
		if (field != null)
			field.clearFocus();
	}

	private void focusField(final EditText target)
	{
		if (target == null)
			return;
		target.requestFocus();
		target.post(() ->
		{
			final android.view.inputmethod.InputMethodManager ime =
					(android.view.inputmethod.InputMethodManager)
							getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			if (ime != null)
				ime.showSoftInput(target, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
		});
	}

	private void buildObject()
	{
		final Context context = getContext();

		final LinearLayout fieldRow = new LinearLayout(context);
		fieldRow.setOrientation(LinearLayout.HORIZONTAL);
		fieldRow.setGravity(Gravity.CENTER_VERTICAL);
		fieldRow.setBackground(Theme.box(0xFF1E1E1F, 10, 0xFF000000));
		Theme.padding(fieldRow, 14, 0, 14, 0);

		final ImageView glyph = new ImageView(context);
		glyph.setImageDrawable(Theme.icon(context, "bbtSearch", true));
		glyph.setAlpha(0.9f);
		fieldRow.addView(glyph, new LinearLayout.LayoutParams(Theme.dp(22), Theme.dp(22)));

		field = new EditText(context);
		field.setBackground(null);
		field.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17);
		field.setTextColor(0xFFFFFFFF);
		field.setHintTextColor(Theme.TEXT_FAINT);
		field.setHint(T.t("Star, planet, catalogue number…"));
		field.setTypeface(Theme.sans());
		field.setSingleLine(true);
		field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		field.setText(T.t(query));
		field.setSelection(query.length());
		final LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		fieldParams.leftMargin = Theme.dp(10);
		fieldRow.addView(field, fieldParams);

		final TextView greekKey = Theme.text(context, "α", 18, Theme.TEXT_DIM, false);
		greekKey.setGravity(Gravity.CENTER);
		greekKey.setContentDescription(T.t("Greek letters"));
		greekKey.setBackground(Theme.pressable(Theme.box(0, 8), 8));
		fieldRow.addView(greekKey, new LinearLayout.LayoutParams(Theme.dp(36), Theme.dp(36)));

		final LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(52));
		rowParams.leftMargin = rowParams.rightMargin = Theme.dp(16);
		rowParams.topMargin = Theme.dp(14);
		rowParams.bottomMargin = Theme.dp(10);
		page.addView(fieldRow, rowParams);

		field.addTextChangedListener(new TextWatcher()
		{
			public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
			public void onTextChanged(CharSequence s, int a, int b, int c) {}
			public void afterTextChanged(Editable s)
			{
				if (pending != null)
					debounce.removeCallbacks(pending);
				pending = () -> search(field.getText().toString());
				debounce.postDelayed(pending, 220);
			}
		});

		final TextView keysCaption = Widgets.section(context, "Greek letters for Bayer designations");
		final GridLayout keys = new GridLayout(context);
		final int available = getResources().getConfiguration().screenWidthDp - 32;
		keys.setColumnCount(Math.max(4, available / 50));
		Theme.padding(keys, 16, 0, 16, 6);
		for (int i = 0; i < GREEK.length(); ++i)
		{
			final String letter = String.valueOf(GREEK.charAt(i));
			final TextView key = Theme.text(context, letter, 18, 0xFFC8CBC2, false);
			key.setGravity(Gravity.CENTER);
			key.setBackground(Theme.pressable(Theme.box(Theme.HAIRLINE, 8), 8));
			key.setOnClickListener(v -> field.getText().insert(field.getSelectionStart(), letter));
			final GridLayout.LayoutParams params = new GridLayout.LayoutParams();
			params.width = Theme.dp(Theme.TOUCH);
			params.height = Theme.dp(Theme.TOUCH);
			params.rightMargin = params.bottomMargin = Theme.dp(6);
			keys.addView(key, params);
		}
		keysCaption.setVisibility(GONE);
		keys.setVisibility(GONE);
		page.addView(keysCaption);
		page.addView(keys);
		greekKey.setOnClickListener(v ->
		{
			final boolean show = keys.getVisibility() != VISIBLE;
			keysCaption.setVisibility(show ? VISIBLE : GONE);
			keys.setVisibility(show ? VISIBLE : GONE);
			greekKey.setTextColor(show ? Theme.ACCENT_LINK : Theme.TEXT_DIM);
		});

		results = new LinearLayout(context);
		results.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(results, 8, 0, 8, 0);
		page.addView(results);

		emptyNote = Widgets.note(context, "");
		page.addView(emptyNote);

		search(query);
		focusField(field);
	}

	private void search(String text)
	{
		query = text;
		final int mine = ++serial;
		NativeBridge.request("search", text, payload ->
		{
			if (mine != serial || results == null)
				return;
			results.removeAllViews();
			final JSONArray hits = payload.optJSONArray("results");
			if (hits == null || hits.length() == 0)
			{
				emptyNote.setText(text.trim().isEmpty()
						? T.t("Type a name, a Bayer letter or a catalogue number - "
						      + "Vega, α Lyr, M 31, NGC 224.")
						: String.format(T.t("Nothing found for “%s”."), text));
				emptyNote.setVisibility(VISIBLE);
			}
			else
			{
				emptyNote.setVisibility(GONE);
				results.addView(Widgets.section(getContext(),
						payload.optBoolean("recent") ? "Recent Searches" : "Results"));
				for (int i = 0; i < hits.length(); ++i)
				{
					final JSONObject hit = hits.optJSONObject(i);
					if (hit != null)
						results.addView(resultRow(hit, null));
				}
			}
			askSimbadAbout(text, mine);
		});
	}

	private void askSimbadAbout(final String text, final int mine)
	{
		if (!options.optBoolean("simbad", true) || text.trim().length() < 2)
			return;
		NativeBridge.request("simbad.names", text, payload ->
		{
			final JSONArray found = payload.optJSONArray("results");
			if (mine != serial || results == null || found == null || found.length() == 0)
				return;
			emptyNote.setVisibility(GONE);
			results.addView(Widgets.section(getContext(), "SIMBAD"));
			for (int i = 0; i < found.length(); ++i)
			{
				final JSONObject hit = found.optJSONObject(i);
				if (hit == null)
					continue;
				hit.remove("badge");
				results.addView(resultRow(hit, "simbad.goto"));
			}
		});
	}

	private View resultRow(final JSONObject hit, final String verb)
	{
		final Context context = getContext();
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 12, 6, 12, 6);
		row.setBackground(Theme.pressable(Theme.box(0, 10), 10));

		final TextView badge = Theme.text(context, hit.optString("badge", "◈"), 10,
		                                  Theme.ACCENT_LINK, true);
		badge.setGravity(Gravity.CENTER);
		badge.setBackground(Theme.box(0x66000000, 8));
		row.addView(badge, new LinearLayout.LayoutParams(Theme.dp(36), Theme.dp(36)));

		final LinearLayout titles = new LinearLayout(context);
		titles.setOrientation(LinearLayout.VERTICAL);
		titles.addView(Theme.text(context, hit.optString("name"), 15, Theme.TEXT, false));
		final String sub = hit.optString("sub");
		if (!sub.isEmpty())
			titles.addView(Theme.text(context, sub, 11, Theme.TEXT_DIM, false));
		final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		titleParams.leftMargin = Theme.dp(12);
		row.addView(titles, titleParams);

		row.addView(Theme.text(context, hit.optString("mag"), 12, Theme.TEXT_DIM, true));

		if (verb == null)
		{
			final ImageView star = Widgets.starButton(context, hit.optBoolean("saved"));
			star.setOnClickListener(v ->
			{
				NativeBridge.send("obslist.toggle", hit.optString("name"));
				Widgets.setStarred(star, !Widgets.starred(star));
			});
			final LinearLayout.LayoutParams starParams =
					new LinearLayout.LayoutParams(Theme.dp(40), Theme.dp(40));
			starParams.leftMargin = Theme.dp(6);
			row.addView(star, starParams);
		}

		row.setOnClickListener(v -> goTo(verb == null ? "select" : verb,
				verb == null ? hit.optString("name")
				             : hit.optString("name") + "|" + hit.optString("pos")));
		return row;
	}

	private void goTo(String verb, String arg)
	{
		NativeBridge.send(verb, arg);
		if (options.optBoolean("autoClose", true))
			overlay().close(this);
	}

	private void buildSimbad()
	{
		final Context context = getContext();
		if (!options.optBoolean("simbad", true))
		{
			page.addView(Widgets.note(context, "SIMBAD is switched off. Turn it on under "
			                                   + "Options to look objects up online."));
			return;
		}

		page.addView(Widgets.note(context, "Asks SIMBAD what it holds around the object that is "
		                                   + "selected now, and prints the answer as it comes back."));

		final TextView status = Widgets.note(context, "");
		status.setTextColor(Theme.ACCENT);
		final TextView answer = Theme.text(context, "", 12, Theme.TEXT_CHIP, false);
		answer.setTypeface(Theme.mono());
		answer.setTextIsSelectable(true);
		answer.setLineSpacing(Theme.dp(2), 1f);
		Theme.padding(answer, 16, 4, 16, 12);

		final TextView ask = Widgets.primaryButton(context, "Query SIMBAD about selected object", v ->
		{
			status.setText(T.t("Simbad Lookup: querying"));
			answer.setText(T.t(""));
			NativeBridge.request("simbad.here", "", payload ->
			{
				if (shown != SIMBAD)
					return;
				final String error = payload.optString("error");
				status.setText(error.isEmpty() ? "" : String.format(T.t("Simbad Lookup Error: %s"), error));
				answer.setText(T.t(payload.optString("text")));
			});
		});
		final LinearLayout.LayoutParams askParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		askParams.leftMargin = askParams.rightMargin = Theme.dp(16);
		askParams.bottomMargin = Theme.dp(6);
		page.addView(ask, askParams);
		page.addView(status);
		page.addView(answer);

		page.addView(Widgets.section(context, "Query options"));
		addSwitch(page, "All IDs", "Every name the object is catalogued under", "ids");
		addSwitch(page, "Types", null, "types");
		addSwitch(page, "Spectral Class", null, "spectrum");
		addSwitch(page, "Morph. Descr.", "Morphological description", "morphology");
		addSwitch(page, "Dimensions", null, "dimensions");
		addNumber(page, "Max. search radius", "arcsec", "radius", 1, 3600);
		addNumber(page, "Max. results", null, "results", 1, 20);
	}

	private void buildPosition()
	{
		final Context context = getContext();
		final String system = options.optString("system", "equatorialJ2000");

		final TextView systemName = Theme.text(context, nameOf(system, SYSTEM_IDS, SYSTEM_NAMES),
		                                       14, Theme.ACCENT, false);
		page.addView(Widgets.pickerRow(context, "Coordinate system", systemName, v ->
		{
			final PickerSheet picker = new PickerSheet(context, overlay(), "Coordinate system",
					system, id ->
			{
				set("system", id);
				showPage(POSITION);
			});
			overlay().open(picker);
			picker.setItems(Arrays.asList(SYSTEM_IDS), Arrays.asList(SYSTEM_NAMES));
		}));
		page.addView(Widgets.hairline(context));

		final boolean equatorial = system.startsWith("equatorial");
		final boolean horizontal = system.equals("horizontal");
		final String xName = equatorial ? "Right ascension" : horizontal ? "Azimuth" : "Longitude";
		final String yName = equatorial
				? (options.optBoolean("polarDistance") ? "Polar distance" : "Declination")
				: horizontal ? "Altitude" : "Latitude";

		xField = axisField(xName, equatorial ? "12h30m49.4s" : "271°15'00\"");
		yField = axisField(yName, "+41°16'09\"");
		page.addView(Widgets.note(context, "Degrees, hours and minutes as they are written - "
		                                   + "12h30m49s, +41°16'09\", 187.7 - or plain decimal degrees."));

		final TextView go = Widgets.primaryButton(context, "Go to position", v ->
		{
			NativeBridge.send("search.goto", system + "|" + xField.getText() + "|" + yField.getText());
			if (options.optBoolean("autoClose", true))
				overlay().close(this);
		});
		final LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		goParams.leftMargin = goParams.rightMargin = Theme.dp(16);
		page.addView(go, goParams);

		page.addView(Widgets.note(context, "Note: this tool doesn't apply the refraction correction "
		                                   + "for coordinates. Horizontal coordinates stop the clock, "
		                                   + "or the sky would turn away from the point while you "
		                                   + "arrive at it."));

		NativeBridge.request("search.centre", system, payload ->
		{
			if (shown != POSITION || xField == null)
				return;
			xField.setText(T.t(payload.optString("x")));
			yField.setText(T.t(payload.optString("y")));
		});
	}

	private EditText axisField(String label, String hint)
	{
		final Context context = getContext();
		final TextView caption = Theme.text(context, label, 10, Theme.TEXT_FAINT, true);
		caption.setAllCaps(true);
		caption.setLetterSpacing(0.08f);
		Theme.padding(caption, 18, 8, 16, 4);
		page.addView(caption);

		final EditText input = Widgets.field(context, label, hint, false);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = params.rightMargin = Theme.dp(16);
		page.addView(input, params);
		return input;
	}

	private void buildLists()
	{
		final Context context = getContext();

		final TextView moduleName = Theme.text(context,
				module.isEmpty() ? "Loading…" : nameOf(module, moduleIds, moduleNames),
				14, Theme.ACCENT, false);
		page.addView(Widgets.pickerRow(context, "List", moduleName, v ->
		{
			if (moduleIds.isEmpty())
				return;
			final PickerSheet picker = new PickerSheet(context, overlay(), "List", module,
					id ->
			{
				module = id;
				listFilter = "";
				showPage(LISTS);
			});
			overlay().open(picker);
			picker.setItems(moduleIds, moduleNames);
		}));
		page.addView(Widgets.hairline(context));
		page.addView(Widgets.note(context,
				"Some objects may be found after activation respective plug-ins"));
		addSwitch(page, "Names in English", null, "english", on -> showPage(LISTS));

		final EditText filter = Widgets.field(context, "Search in list", "Search in list...", false);
		filter.setText(T.t(listFilter));
		final LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		filterParams.leftMargin = filterParams.rightMargin = Theme.dp(16);
		filterParams.topMargin = Theme.dp(12);
		page.addView(filter, filterParams);
		filter.addTextChangedListener(new TextWatcher()
		{
			public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
			public void onTextChanged(CharSequence s, int a, int b, int c) {}
			public void afterTextChanged(Editable s)
			{
				listFilter = s.toString();
				if (pending != null)
					debounce.removeCallbacks(pending);
				pending = SearchSheet.this::refreshList;
				debounce.postDelayed(pending, 220);
			}
		});

		listCount = Widgets.note(context, "");
		page.addView(listCount);
		listRows = new LinearLayout(context);
		listRows.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(listRows, 8, 0, 8, 0);
		page.addView(listRows);

		if (moduleIds.isEmpty())
		{
			NativeBridge.request("search.modules", "", payload ->
			{
				final JSONArray list = payload.optJSONArray("modules");
				moduleIds.clear();
				moduleNames.clear();
				for (int i = 0; list != null && i < list.length(); ++i)
				{
					final JSONObject item = list.optJSONObject(i);
					if (item == null)
						continue;
					moduleIds.add(item.optString("id"));
					moduleNames.add(item.optString("name"));
				}
				if (module.isEmpty())
					module = moduleIds.contains("ConstellationMgr") ? "ConstellationMgr"
					         : moduleIds.isEmpty() ? "" : moduleIds.get(0);
				if (shown == LISTS)
				{
					moduleName.setText(T.t(nameOf(module, moduleIds, moduleNames)));
					refreshList();
				}
			});
			return;
		}
		refreshList();
	}

	private void refreshList()
	{
		if (listRows == null || module.isEmpty())
			return;
		NativeBridge.request("search.list", module + "|" + listFilter, payload ->
		{
			if (listRows == null || shown != LISTS)
				return;
			listRows.removeAllViews();
			final JSONArray names = payload.optJSONArray("names");
			final int total = payload.optInt("total");
			final int drawn = names == null ? 0 : names.length();
			listCount.setText(drawn == 0 ? T.t("Nothing in this list matches.")
			                  : drawn < total
			                    ? String.format(T.t("First %d of %d - filter to narrow it."), drawn, total)
			                    : String.format(T.t(total == 1 ? "%d object" : "%d objects"), total));
			for (int i = 0; i < drawn; ++i)
			{
				final String name = names.optString(i);
				final LinearLayout row = new LinearLayout(getContext());
				row.setMinimumHeight(Theme.dp(Theme.ROW));
				row.setGravity(Gravity.CENTER_VERTICAL);
				Theme.padding(row, 12, 6, 12, 6);
				row.setBackground(Theme.pressable(Theme.box(0, 10), 10));
				row.addView(Theme.text(getContext(), name, 15, Theme.TEXT, false));
				row.setOnClickListener(v -> goTo("select", name));
				listRows.addView(row);
			}
		});
	}

	private void buildOptions()
	{
		final Context context = getContext();

		page.addView(Widgets.section(context, "Search options"));
		addSwitch(page, "Use autofill only from the beginning of words", null, "startOfWords");
		addSwitch(page, "Use sorting by string length", null, "lengthSort");
		addSwitch(page, "Lock position when coordinates are used", null, "lockPosition");
		addSwitch(page, "Automatic closing dialog", "When a search result is chosen", "autoClose");
		addSwitch(page, "Show FOV center marker when position is search", null, "fovMarker");

		page.addView(Widgets.section(context, "Use SIMBAD"));
		addSwitch(page, "Use on-line astronomical database SIMBAD", null, "simbad");
		final TextView serverName = Theme.text(context,
				nameOf(options.optString("server"), SERVER_IDS, SERVER_NAMES), 13, Theme.ACCENT, false);
		page.addView(Widgets.pickerRow(context, "Server", serverName, v ->
		{
			final PickerSheet picker = new PickerSheet(context, overlay(), "SIMBAD server",
					options.optString("server"), id ->
			{
				set("server", id);
				serverName.setText(T.t(nameOf(id, SERVER_IDS, SERVER_NAMES)));
			});
			overlay().open(picker);
			picker.setItems(Arrays.asList(SERVER_IDS), Arrays.asList(SERVER_NAMES));
		}));
		page.addView(Widgets.hairline(context));

		page.addView(Widgets.section(context, "Recent Searches"));
		addNumber(page, "Max items to display", null, "recentSize", 1, 100);
		final TextView cleared = Widgets.note(context,
				"Deletes all recent search object data, and the objects SIMBAD contributed.");
		final TextView clear = Widgets.secondaryButton(context, "Clear recent searches", v ->
		{
			NativeBridge.send("search.forget", "");
			cleared.setText(T.t("Cleared."));
		});
		final LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		clearParams.leftMargin = clearParams.rightMargin = Theme.dp(16);
		clearParams.topMargin = Theme.dp(6);
		page.addView(clear, clearParams);
		page.addView(cleared);
	}

	private interface Toggled
	{
		void changed(boolean on);
	}

	private void addSwitch(LinearLayout into, String label, String sub, String key)
	{
		addSwitch(into, label, sub, key, null);
	}

	private void addSwitch(LinearLayout into, String label, String sub, final String key,
	                       final Toggled onChange)
	{
		into.addView(Widgets.switchRow(getContext(), label, sub, options.optBoolean(key),
				(button, checked) ->
		{
			set(key, String.valueOf(checked));
			if (onChange != null)
				onChange.changed(checked);
		}));
		into.addView(Widgets.hairline(getContext()));
	}

	private void addNumber(LinearLayout into, String label, String sub, final String key,
	                       final int min, final int max)
	{
		final Context context = getContext();
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 16, 8, 16, 8);

		final LinearLayout titles = new LinearLayout(context);
		titles.setOrientation(LinearLayout.VERTICAL);
		titles.addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false));
		if (sub != null && !sub.isEmpty())
			titles.addView(Theme.text(context, sub, 11, Theme.TEXT_DIM, false));
		row.addView(titles, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final EditText input = Widgets.field(context, label, "", true);
		input.setText(T.t(String.valueOf(options.optInt(key))));
		input.setGravity(Gravity.CENTER);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				Theme.dp(88), LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = Theme.dp(12);
		row.addView(input, params);

		final Runnable apply = () ->
		{
			final double typed = Widgets.number(input);
			int value = Double.isNaN(typed) ? options.optInt(key) : (int) Math.round(typed);
			value = Math.max(min, Math.min(max, value));
			input.setText(T.t(String.valueOf(value)));
			set(key, String.valueOf(value));
		};
		input.setOnEditorActionListener((v, action, event) ->
		{
			apply.run();
			return false;
		});
		input.setOnFocusChangeListener((v, hasFocus) ->
		{
			if (!hasFocus)
				apply.run();
		});

		into.addView(row);
		into.addView(Widgets.hairline(context));
	}

	private void set(String key, String value)
	{
		NativeBridge.send("search.set", key + "=" + value);
		try
		{
			options.put(key, "true".equals(value) || "false".equals(value)
			                 ? (Object) Boolean.valueOf(value) : (Object) value);
		}
		catch (JSONException ignored)
		{
		}
	}

	private static String nameOf(String id, String[] ids, String[] names)
	{
		for (int i = 0; i < ids.length && i < names.length; ++i)
		{
			if (ids[i].equals(id))
				return names[i];
		}
		return id;
	}

	private static String nameOf(String id, List<String> ids, List<String> names)
	{
		final int at = ids.indexOf(id);
		return at >= 0 && at < names.size() ? names.get(at) : id;
	}
}
