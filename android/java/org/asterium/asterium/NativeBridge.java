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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class NativeBridge
{
	private static final String TAG = "Asterium";

	public interface StateListener
	{
		void onState(JSONObject state);
		void onEngineReady();
	}

	public interface Reply
	{
		void onReply(JSONObject payload);
	}

	private NativeBridge() {}

	private static native void nativeSend(int token, String verb, String arg);

	private static final Handler ui = new Handler(Looper.getMainLooper());
	private static final AtomicInteger nextToken = new AtomicInteger(1);
	private static final Map<Integer, Reply> pending = new HashMap<>();
	private static StateListener listener;
	private static JSONObject lastState;
	private static boolean engineReady;

	static void setListener(StateListener value)
	{
		listener = value;
		if (value == null)
			return;
		if (engineReady)
			value.onEngineReady();
		if (lastState != null)
			value.onState(lastState);
	}

	static JSONObject lastState()
	{
		return lastState;
	}

	static void send(String verb, String arg)
	{
		if (!engineReady)
			return;
		try
		{
			nativeSend(0, verb, arg == null ? "" : arg);
		}
		catch (UnsatisfiedLinkError e)
		{
			Log.d(TAG, "bridge not registered yet, dropping " + verb);
		}
	}

	static void send(String verb)
	{
		send(verb, "");
	}

	static void set(String property, Object value)
	{
		send("prop.set", property + "=" + value);
	}

	static void request(String verb, String arg, Reply reply)
	{
		if (!engineReady)
		{
			reply.onReply(new JSONObject());
			return;
		}
		final int token = nextToken.getAndIncrement();
		pending.put(token, reply);
		try
		{
			nativeSend(token, verb, arg == null ? "" : arg);
		}
		catch (UnsatisfiedLinkError e)
		{
			pending.remove(token);
			reply.onReply(new JSONObject());
		}
	}

	static void onSnapshot(final String json)
	{
		ui.post(new Runnable() { public void run()
		{
			try
			{
				lastState = new JSONObject(json);
			}
			catch (Exception e)
			{
				Log.w(TAG, "unreadable snapshot", e);
				return;
			}
			if (listener != null)
				listener.onState(lastState);
		}});
	}

	static void onReply(final int token, final String json)
	{
		ui.post(new Runnable() { public void run()
		{
			final Reply reply = pending.remove(token);
			if (reply == null)
				return;
			JSONObject payload;
			try
			{
				payload = new JSONObject(json);
			}
			catch (Exception e)
			{
				payload = new JSONObject();
			}
			reply.onReply(payload);
		}});
	}

	static void onEngineReady()
	{
		ui.post(new Runnable() { public void run()
		{
			engineReady = true;
			Log.i(TAG, "engine ready, interface going live");
			if (listener != null)
				listener.onEngineReady();
		}});
	}
}
