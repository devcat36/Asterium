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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class ObsListSheet extends Sheet
{
	private static final String HIGHLIGHT_ACTION = "actionShow_ObsList_Highlight";

	private final TextView listName;
	private final TextView countNote;
	private final LinearLayout rows;
	private final TextView emptyNote;
	private final LinearLayout editor;
	private final EditText nameField;
	private final TextView renameButton;
	private final TextView newButton;
	private final TextView deleteButton;
	private final TextView saveSelected;
	private final TextView highlightChip;

	private JSONObject data = new JSONObject();
	private String selectedName = "";
	private boolean selectionSaved;
	private boolean hasSelection;
	private boolean highlighted;
	private boolean confirmingDelete;
	private boolean naming;
	private boolean namingNewList;

	ObsListSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "obslist", "Observing list");

		listName = Theme.text(context, "", 14, Theme.ACCENT, false);
		body.addView(Widgets.pickerRow(context, "List", listName, v -> pickList()));

		final LinearLayout buttons = new LinearLayout(context);
		buttons.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(buttons, 16, 4, 16, 12);
		renameButton = Widgets.secondaryButton(context, "Rename", v -> startNaming(false));
		buttons.addView(renameButton, weighted(0));
		newButton = Widgets.secondaryButton(context, "New list", v -> startNaming(true));
		buttons.addView(newButton, weighted(8));
		deleteButton = Widgets.secondaryButton(context, "Delete", v -> deleteList());
		buttons.addView(deleteButton, weighted(8));
		body.addView(buttons);

		editor = new LinearLayout(context);
		editor.setOrientation(LinearLayout.HORIZONTAL);
		editor.setGravity(Gravity.CENTER_VERTICAL);
		editor.setVisibility(GONE);
		Theme.padding(editor, 16, 0, 16, 12);
		nameField = Widgets.field(context, "List name", "List name", false);
		nameField.setOnEditorActionListener((v, action, event) ->
		{
			commitName();
			return true;
		});
		editor.addView(nameField, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		final TextView confirm = Widgets.primaryButton(context, "Save", v -> commitName());
		final LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
				Theme.dp(84), Theme.dp(52));
		confirmParams.leftMargin = Theme.dp(8);
		editor.addView(confirm, confirmParams);
		body.addView(editor);

		body.addView(Widgets.hairline(context));

		countNote = Widgets.section(context, "");
		body.addView(countNote);

		rows = new LinearLayout(context);
		rows.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(rows, 8, 0, 8, 0);
		body.addView(rows);

		emptyNote = Widgets.note(context, "Nothing in this list yet. Pick something out of the sky "
		                                  + "and tap the star on its card to keep it here.");
		Theme.padding(emptyNote, 16, 18, 16, 14);
		body.addView(emptyNote);

		final LinearLayout footer = addFooter();
		saveSelected = Widgets.primaryButton(context, "Nothing selected",
		                                     v -> send("obslist.save", ""));
		saveSelected.setEnabled(false);
		saveSelected.setAlpha(0.45f);
		footer.addView(saveSelected, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		highlightChip = Widgets.toggleChip(context, "Highlight", false,
		                                   v -> send("obslist.highlight", highlighted ? "0" : "1"));
		final LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, Theme.dp(48));
		chipParams.leftMargin = Theme.dp(10);
		footer.addView(highlightChip, chipParams);

		refresh();
	}

	private LinearLayout.LayoutParams weighted(int leftMarginDp)
	{
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		params.leftMargin = Theme.dp(leftMarginDp);
		return params;
	}

	@Override
	void onReturn()
	{
		refresh();
	}

	@Override
	void onState(JSONObject state)
	{
		final JSONObject toggles = state.optJSONObject("toggles");
		if (toggles != null && toggles.has(HIGHLIGHT_ACTION))
			setHighlighted(toggles.optBoolean(HIGHLIGHT_ACTION));

		final JSONObject selection = state.optJSONObject("sel");
		final boolean present = selection != null;
		final String name = present ? selection.optString("name") : "";
		final boolean saved = present && selection.optBoolean("saved");
		if (present == hasSelection && name.equals(selectedName) && saved == selectionSaved)
			return;
		final boolean membershipChanged = saved != selectionSaved && name.equals(selectedName);
		hasSelection = present;
		selectedName = name;
		selectionSaved = saved;
		if (membershipChanged)
			refresh();
		saveSelected.setText(!present ? T.t("Nothing selected")
		                     : String.format(T.t(saved ? "Remove %s" : "Add %s"), name));
		saveSelected.setEnabled(present);
		saveSelected.setAlpha(present ? 1f : 0.45f);
	}

	private void setHighlighted(boolean on)
	{
		if (on == highlighted)
			return;
		highlighted = on;
		Widgets.setChipOn(highlightChip, on);
	}

	private void send(String verb, String arg)
	{
		NativeBridge.send(verb, arg);
		refresh();
	}

	private void refresh()
	{
		NativeBridge.request("obslist", "", payload ->
		{
			data = payload;
			rebuild();
		});
	}

	private void rebuild()
	{
		final Context context = getContext();
		confirmingDelete = false;
		deleteButton.setText(T.t("Delete"));
		listName.setText(T.t(data.optString("name")));
		setHighlighted(data.optBoolean("highlighted"));

		rows.removeAllViews();
		final JSONArray objects = data.optJSONArray("objects");
		final int count = objects == null ? 0 : objects.length();
		countNote.setText(count + " " + T.t(count == 1 ? "OBJECT" : "OBJECTS"));
		countNote.setVisibility(count == 0 ? GONE : VISIBLE);
		emptyNote.setVisibility(count == 0 ? VISIBLE : GONE);
		for (int i = 0; i < count; ++i)
		{
			final JSONObject item = objects.optJSONObject(i);
			if (item != null)
				rows.addView(row(context, item));
		}
	}

	private View row(Context context, final JSONObject item)
	{
		final String designation = item.optString("designation");
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(row, 12, 6, 4, 6);
		row.setBackground(Theme.pressable(Theme.box(0, 10), 10));
		row.setOnClickListener(v ->
		{
			NativeBridge.send("obslist.goto", designation);
			overlay().close(this);
		});

		final LinearLayout titles = new LinearLayout(context);
		titles.setOrientation(LinearLayout.VERTICAL);
		final TextView name = Theme.text(context, item.optString("name"), 15, Theme.TEXT, false);
		name.setSingleLine(true);
		name.setEllipsize(android.text.TextUtils.TruncateAt.END);
		titles.addView(name);
		final String sub = item.optString("sub");
		if (!sub.isEmpty())
		{
			final TextView subtitle = Theme.text(context, sub, 11, Theme.TEXT_DIM, false);
			subtitle.setSingleLine(true);
			subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
			titles.addView(subtitle);
		}
		row.addView(titles, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		final String magnitude = item.optString("mag");
		row.addView(Theme.text(context, magnitude.isEmpty() ? "" : "mag " + magnitude,
		                       12, Theme.TEXT_DIM, true));

		final TextView remove = Theme.text(context, "✕", 16, Theme.TEXT_DIM, false);
		remove.setGravity(Gravity.CENTER);
		remove.setContentDescription(String.format(T.t("Remove %s"), item.optString("name")));
		remove.setBackground(Theme.pressableCircle(Theme.circle(0, 0)));
		remove.setOnClickListener(v -> send("obslist.remove", designation));
		final LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
				Theme.dp(Theme.TOUCH), Theme.dp(Theme.TOUCH));
		removeParams.leftMargin = Theme.dp(4);
		row.addView(remove, removeParams);
		return row;
	}

	private void pickList()
	{
		final JSONArray lists = data.optJSONArray("lists");
		if (lists == null || lists.length() == 0)
			return;
		final List<String> ids = new ArrayList<>();
		final List<String> names = new ArrayList<>();
		for (int i = 0; i < lists.length(); ++i)
		{
			final JSONObject entry = lists.optJSONObject(i);
			if (entry == null)
				continue;
			ids.add(entry.optString("olud"));
			final int count = entry.optInt("count");
			names.add(entry.optString("name") + "  ·  " + count
			          + (count == 1 ? " object" : " objects"));
		}
		final PickerSheet picker = new PickerSheet(getContext(), overlay(), "Observing lists",
				data.optString("olud"), id -> send("obslist.select", id));
		overlay().open(picker);
		picker.setItems(ids, names);
	}

	private void startNaming(boolean forNewList)
	{
		if (naming && namingNewList == forNewList)
		{
			stopNaming();
			return;
		}
		naming = true;
		namingNewList = forNewList;
		renameButton.setText(T.t(forNewList ? "Rename" : "Cancel"));
		newButton.setText(T.t(forNewList ? "Cancel" : "New list"));
		nameField.setHint(T.t(forNewList ? "Name for the new list" : "List name"));
		nameField.setText(T.t(forNewList ? "" : data.optString("name")));
		nameField.setSelection(nameField.getText().length());
		editor.setVisibility(VISIBLE);
		nameField.requestFocus();
		nameField.post(() ->
		{
			final android.view.inputmethod.InputMethodManager ime =
					(android.view.inputmethod.InputMethodManager)
							getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			if (ime != null)
				ime.showSoftInput(nameField,
				                  android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
		});
	}

	private void stopNaming()
	{
		naming = false;
		editor.setVisibility(GONE);
		renameButton.setText(T.t("Rename"));
		newButton.setText(T.t("New list"));
		nameField.clearFocus();
		final android.view.inputmethod.InputMethodManager ime =
				(android.view.inputmethod.InputMethodManager)
						getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (ime != null && getWindowToken() != null)
			ime.hideSoftInputFromWindow(getWindowToken(), 0);
	}

	private void commitName()
	{
		final String typed = nameField.getText().toString().trim();
		final boolean forNewList = namingNewList;
		stopNaming();
		if (typed.isEmpty())
			return;
		send(forNewList ? "obslist.new" : "obslist.rename", typed);
	}

	private void deleteList()
	{
		final JSONArray lists = data.optJSONArray("lists");
		if (lists != null && lists.length() < 2)
		{
			deleteButton.setText(T.t("Only list"));
			return;
		}
		if (!confirmingDelete)
		{
			confirmingDelete = true;
			deleteButton.setText(T.t("Tap to confirm"));
			return;
		}
		send("obslist.delete", "");
	}

	@Override
	boolean onBack()
	{
		if (!naming)
			return false;
		stopNaming();
		return true;
	}
}
