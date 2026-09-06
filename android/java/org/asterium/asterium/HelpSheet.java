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
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class HelpSheet extends Sheet
{
	private static final String[] PAGES = { "About", "Credits", "Licences", "Log", "Config" };

	private static final String[][] LICENCES = {
		{ "GPL-3.0",     "COPYING.GPL3" },
		{ "GPL-2.0",     "COPYING" },
		{ "LGPL-3.0",    "COPYING.LGPL3" },
		{ "LGPL-2.1",    "COPYING.LGPL21" },
		{ "Apache-2.0",  "COPYING.APACHE2" },
		{ "SIL OFL 1.1", "data/OFL.txt" },
	};

	private static final String SOURCE = "https://github.com/devcat36/Asterium";

	private final LinearLayout tabs;
	private int page = 0;
	private int licence = 0;

	HelpSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "help", "Help");
		tabs = addTabRail();
		showPage(recallTab(PAGES.length));
	}

	private void showPage(int index)
	{
		page = index;
		rememberTab(index);
		buildTabs();
		body.removeAllViews();
		switch (index)
		{
			case 1:  buildText("credits");  break;
			case 2:  buildLicences();  break;
			case 3:  buildText("log");  break;
			case 4:  buildText("config");  break;
			default: buildAbout();  break;
		}
	}

	private void buildTabs()
	{
		tabs.removeAllViews();
		for (int i = 0; i < PAGES.length; ++i)
		{
			final int index = i;
			tabs.addView(Widgets.tabChip(getContext(), PAGES[i], i == page, v -> showPage(index)));
		}
	}

	private void buildAbout()
	{
		final Context context = getContext();

		body.addView(Widgets.section(context, "Asterium"));
		final TextView version = Theme.text(context, "", 12, Theme.TEXT_DIM, false);
		Theme.padding(version, 16, 4, 16, 14);
		body.addView(version);

		body.addView(Widgets.note(context,
				"A touch-first planetarium forked from Stellarium, whose astronomy engine it "
				+ "uses unchanged. The interface is new; everything that draws the sky is "
				+ "upstream's work."));

		body.addView(Widgets.section(context, "Licence"));
		body.addView(Widgets.note(context,
				"Free software. The source is under the GNU General Public License, version 2 "
				+ "or later; this app is a combined work with libraries that are GPLv3 only, so "
				+ "the app as installed is delivered to you under the GNU General Public "
				+ "License, version 3. The full texts are on the Licences page. The list of "
				+ "changes against upstream Stellarium is in NOTICE.md in the source."));

		body.addView(Widgets.section(context, "Source"));
		body.addView(Widgets.navigationRow(context, "Get the complete source code",
				"github.com/devcat36/Asterium", v -> openSource()));

		body.addView(Widgets.section(context, "Credits"));
		body.addView(Widgets.note(context,
				"Stellarium is by Fabien Chereau and the Stellarium team, with star catalogues "
				+ "from Hipparcos, Tycho-2 and NOMAD, and distances from the Hipparcos new "
				+ "reduction (van Leeuwen 2007)."));

		NativeBridge.request("help", "", payload ->
		{
			if (page != 0)
				return;
			version.setText(String.format(T.t("Version %s  ·  Qt %s"),
								payload.optString("version", "?"), payload.optString("qt", "?")));
		});
	}

	private void openSource()
	{
		try
		{
			final Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE));
			browse.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getContext().startActivity(browse);
		}
		catch (Exception e)
		{
			android.util.Log.w("Asterium", "no browser for " + SOURCE, e);
		}
	}

	private void buildLicences()
	{
		final Context context = getContext();

		buildText("license:" + LICENCES[licence][1]);

		final LinearLayout rail = new LinearLayout(context);
		Theme.padding(rail, 16, 12, 16, 4);
		for (int i = 0; i < LICENCES.length; ++i)
		{
			final int index = i;
			rail.addView(Widgets.tabChip(context, LICENCES[i][0], i == licence, v ->
			{
				licence = index;
				showPage(2);
			}));
		}
		final HorizontalScrollView scroll = new HorizontalScrollView(context);
		scroll.setHorizontalScrollBarEnabled(false);
		scroll.addView(rail);
		body.addView(scroll, 0, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
	}

	private void buildText(final String which)
	{
		final Context context = getContext();

		final TextView path = Theme.text(context, "", 11, Theme.TEXT_FAINT, true);
		Theme.padding(path, 16, 12, 16, 8);
		path.setSingleLine(true);
		path.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
		body.addView(path);

		final TextView content = Theme.text(context, "Reading…", 10, Theme.TEXT_DIM, true);
		content.setLineSpacing(Theme.dp(2), 1f);
		content.setTextIsSelectable(true);
		Theme.padding(content, 16, 0, 16, 16);
		final HorizontalScrollView scroll = new HorizontalScrollView(context);
		scroll.setHorizontalScrollBarEnabled(false);
		scroll.addView(content);
		body.addView(scroll, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		final LinearLayout buttonRow = new LinearLayout(context);
		Theme.padding(buttonRow, 16, 4, 16, 8);
		final View.OnClickListener reload = v -> load(which, path, content);
		buttonRow.addView(Widgets.secondaryButton(context, "Refresh", reload),
				new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(48)));
		body.addView(buttonRow, 0);

		load(which, path, content);
	}

	private void load(final String which, final TextView path, final TextView content)
	{
		final int on = page;
		NativeBridge.request("help", which, payload ->
		{
			if (page != on)
				return;
			path.setText(T.t(payload.optString("path")));
			final String text = payload.optString("text");
			content.setText(text.isEmpty() ? T.t("Nothing to show.") : text);
		});
	}
}
