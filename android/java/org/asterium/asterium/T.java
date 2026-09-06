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
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class T
{
	private static final String PREFS = "asterium.i18n";
	private static final String KEY = "strings";

	private static Map<String, String> strings = Collections.emptyMap();
	private static Context appContext;

	private T() {}

	static String t(String value)
	{
		if (value == null)
			return null;
		final String hit = strings.get(value);
		return hit != null ? hit : value;
	}

	static void init(Context context)
	{
		appContext = context.getApplicationContext();
		apply(prefs().getString(KEY, null));
	}

	static void refresh(final Runnable done)
	{
		NativeBridge.request("i18n", "", payload ->
		{
			final JSONObject table = payload.optJSONObject(KEY);
			if (table != null)
			{
				final String json = table.toString();
				apply(json);
				prefs().edit().putString(KEY, json).apply();
			}
			done.run();
		});
	}

	private static SharedPreferences prefs()
	{
		return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
	}

	private static void apply(String json)
	{
		if (json == null || json.isEmpty())
			return;
		try
		{
			final JSONObject table = new JSONObject(json);
			final Map<String, String> next = new HashMap<>(table.length() * 2);
			for (Iterator<String> keys = table.keys(); keys.hasNext(); )
			{
				final String key = keys.next();
				next.put(key, table.optString(key));
			}
			strings = next;
		}
		catch (Exception e)
		{
			strings = Collections.emptyMap();
		}
	}
}
