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
import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Deque;

final class Overlay extends FrameLayout implements NativeBridge.StateListener
{
	private final SkyChrome chrome;
	private final OcularBar ocularBar;
	private final FrameLayout sheetHost;
	private final Deque<Sheet> sheets = new ArrayDeque<>();
	private static final int MENU_WIDTH = 392;
	private static final int CARD_WIDTH = 360;
	private static final int DOCK_TOP = 48;
	private static final int DOCK_EDGE = 12;
	private static final int MENU_BOTTOM = 96;
	private static final int CARD_BOTTOM = 168;
	private static final int OCULAR_BOTTOM = 80;
	private static final float MENU_MIN_RATIO = 1.5f;
	private Rect insets = new Rect(0, 0, 0, 0);
	private boolean insetsApplied = false;
	private boolean night = false;
	private boolean skyWasHidden;
	private boolean inOcularView = false;

	Overlay(Context context)
	{
		super(context);
		setClickable(false);
		setFocusable(false);
		setVisibility(INVISIBLE);

		chrome = new SkyChrome(context, this);
		addView(chrome, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		ocularBar = new OcularBar(context, this);
		addView(ocularBar, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		sheetHost = new FrameLayout(context);
		sheetHost.setVisibility(GONE);
		addView(sheetHost, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		NativeBridge.setListener(this);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		if (changed)
		{
			if (getContext() instanceof AsteriumActivity)
				((AsteriumActivity) getContext()).applySystemBars();
			applyInsets();
		}
		ocularBar.place();
	}

	@Override
	public WindowInsets onApplyWindowInsets(WindowInsets applied)
	{
		if (getContext() instanceof AsteriumActivity)
			((AsteriumActivity) getContext()).applySystemBars();
		applyInsets();
		return super.onApplyWindowInsets(applied);
	}

	private void applyInsets()
	{
		final WindowInsets applied = getRootWindowInsets();
		if (applied == null)
			return;
		final Rect own = ownInsets(applied);
		if (insetsApplied && own.equals(insets))
			return;
		insetsApplied = true;
		insets = own;
		chrome.setInsets(own);
		ocularBar.setInsets(own);
		for (Sheet sheet : sheets)
			sheet.setInsets(own);
	}

	private Rect ownInsets(WindowInsets applied)
	{
		final Rect bars = systemBars(applied);
		final View root = getRootView();
		final int[] mine = new int[2];
		final int[] whole = new int[2];
		getLocationOnScreen(mine);
		root.getLocationOnScreen(whole);
		bars.left = Math.max(0, bars.left - (mine[0] - whole[0]));
		bars.top = Math.max(0, bars.top - (mine[1] - whole[1]));
		if (getWidth() > 0)
			bars.right = Math.max(0, bars.right
					- ((whole[0] + root.getWidth()) - (mine[0] + getWidth())));
		if (getHeight() > 0)
			bars.bottom = Math.max(0, bars.bottom
					- ((whole[1] + root.getHeight()) - (mine[1] + getHeight())));
		return bars;
	}

	private static Rect systemBars(WindowInsets applied)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
		{
			final android.graphics.Insets bars = applied.getInsets(WindowInsets.Type.systemBars());
			final android.graphics.Insets cutout =
					applied.getInsets(WindowInsets.Type.displayCutout());
			return new Rect(bars.left, Math.max(bars.top, cutout.top),
			                bars.right, Math.max(bars.bottom, cutout.bottom));
		}
		return new Rect(applied.getSystemWindowInsetLeft(), applied.getSystemWindowInsetTop(),
		                applied.getSystemWindowInsetRight(), applied.getSystemWindowInsetBottom());
	}

	boolean isTablet()
	{
		return getResources().getConfiguration().smallestScreenWidthDp >= Theme.TABLET_WIDTH;
	}

	void open(Sheet sheet)
	{
		add(sheet);
		updateChromeVisibility();
		sheet.slideIn();
	}

	void openDragging(Sheet sheet)
	{
		sheet.beginDrag();
		add(sheet);
	}

	void dragSettled()
	{
		updateChromeVisibility();
	}

	private void add(Sheet sheet)
	{
		if (isTablet())
			sheet.setDocked(true);
		sheet.setInsets(insets);
		sheets.addLast(sheet);
		sheetHost.addView(sheet, sheetLayout(sheet));
		sheetHost.setVisibility(VISIBLE);
		sheet.onShown();
	}

	private LayoutParams sheetLayout(Sheet sheet)
	{
		if (!isTablet())
		{
			return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}
		final boolean card = docksOverSky(sheet.id());
		final boolean hug = sheet.hugsContent();
		final LayoutParams params = new LayoutParams(
				Theme.dp(card ? CARD_WIDTH : MENU_WIDTH),
				hug ? LayoutParams.WRAP_CONTENT : LayoutParams.MATCH_PARENT,
				card ? android.view.Gravity.END | android.view.Gravity.BOTTOM
				     : android.view.Gravity.START | android.view.Gravity.TOP);
		if (card)
			params.rightMargin = insets.right + Theme.dp(DOCK_EDGE);
		else
			params.leftMargin = insets.left + Theme.dp(DOCK_EDGE) + Theme.dp(Theme.RAIL_BUTTON + 28);
		params.topMargin = insets.top + Theme.dp(DOCK_TOP);
		params.bottomMargin = insets.bottom + Theme.dp(dockBottom(sheet.id()));
		sheet.setDockLimits(maxDockHeight(card), card || hug ? 0f : MENU_MIN_RATIO);
		return params;
	}

	private static boolean docksOverSky(String id)
	{
		return "info".equals(id) || "ocularpanel".equals(id);
	}

	private static int dockBottom(String id)
	{
		if ("ocularpanel".equals(id))
			return OCULAR_BOTTOM;
		return docksOverSky(id) ? CARD_BOTTOM : MENU_BOTTOM;
	}

	private int maxDockHeight(boolean card)
	{
		return card ? Theme.dp(CARD_WIDTH * 3 / 2) : Theme.dp(MENU_WIDTH * 2);
	}

	private Rect dockRect(boolean card)
	{
		int width = Theme.dp(card ? CARD_WIDTH : MENU_WIDTH);
		final int top = insets.top + Theme.dp(DOCK_TOP);
		final int floor = getHeight() - insets.bottom
				- Theme.dp(card ? CARD_BOTTOM : MENU_BOTTOM);
		final int height = Math.min(Math.max(floor - top, 0), maxDockHeight(card));
		if (!card && height < width * MENU_MIN_RATIO)
			width = Math.round(height / MENU_MIN_RATIO);
		if (card)
		{
			final int right = getWidth() - insets.right - Theme.dp(DOCK_EDGE);
			return new Rect(right - width, floor - height, right, floor);
		}
		final int left = insets.left + Theme.dp(DOCK_EDGE) + Theme.dp(Theme.RAIL_BUTTON + 28);
		return new Rect(left, top, left + width, top + height);
	}

	private boolean docksOverlap()
	{
		return Rect.intersects(dockRect(false), dockRect(true));
	}

	void close(Sheet sheet)
	{
		if (!sheets.remove(sheet))
			return;
		final android.view.inputmethod.InputMethodManager ime =
				(android.view.inputmethod.InputMethodManager)
						getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (ime != null)
			ime.hideSoftInputFromWindow(getWindowToken(), 0);
		sheetHost.removeView(sheet);
		if (sheets.isEmpty())
			sheetHost.setVisibility(GONE);
		else
			sheets.peekLast().onReturn();
		updateChromeVisibility();
	}

	boolean handleBack()
	{
		final Sheet top = sheets.peekLast();
		if (top != null)
		{
			if (!top.onBack())
				close(top);
			return true;
		}
		if (inOcularView)
		{
			ocularBar.leave();
			return true;
		}
		if (chrome.collapse())
			return true;
		final JSONObject state = NativeBridge.lastState();
		if (state != null && state.optJSONObject("sel") != null)
		{
			NativeBridge.send("unselect");
			return true;
		}
		return false;
	}

	boolean canPoint()
	{
		return chrome.canPoint();
	}

	boolean isPointing()
	{
		return chrome.isPointing();
	}

	void setPointing(boolean value)
	{
		chrome.setPointing(value);
	}

	boolean isFinding()
	{
		return chrome.isFinding();
	}

	void setFinding(boolean value)
	{
		chrome.setFinding(value);
	}

	void rebuildToolbar()
	{
		chrome.rebuildToolbar();
	}

	String topSheetId()
	{
		final Sheet top = sheets.peekLast();
		return top == null ? null : top.id();
	}

	private String topRailSheetId()
	{
		final java.util.Iterator<Sheet> down = sheets.descendingIterator();
		while (down.hasNext())
		{
			final Sheet sheet = down.next();
			if (!docksOverSky(sheet.id()))
				return sheet.id();
		}
		return null;
	}

	private void updateChromeVisibility()
	{
		final boolean covered = !sheets.isEmpty() && !isTablet();
		chrome.setVisibility(covered || inOcularView ? GONE : VISIBLE);
		ocularBar.setVisibility(inOcularView && !covered ? VISIBLE : GONE);
		final String opaque = topRailSheetId();
		chrome.setActiveSheet(opaque);
		final boolean skyHidden = !isTablet() && opaque != null;
		if (skyHidden != skyWasHidden)
		{
			skyWasHidden = skyHidden;
			NativeBridge.send("view.covered", skyHidden ? "1" : "0");
		}
	}

	void openById(String id)
	{
		final Sheet already = sheetById(id);
		if (already != null)
		{
			close(already);
			return;
		}
		makeRoomFor(id);
		final Sheet sheet = Sheets.create(getContext(), this, id);
		if (sheet != null)
			open(sheet);
	}

	boolean isOpen(String id)
	{
		return sheetById(id) != null;
	}

	private Sheet sheetById(String id)
	{
		for (Sheet sheet : sheets)
			if (sheet.id().equals(id))
				return sheet;
		return null;
	}

	private void makeRoomFor(String id)
	{
		if (!isTablet())
		{
			closeAll();
			return;
		}
		final boolean card = docksOverSky(id);
		final boolean overlap = docksOverlap();
		for (Sheet sheet : new java.util.ArrayList<>(sheets))
			if (docksOverSky(sheet.id()) == card || overlap)
				close(sheet);
	}

	void closeAll()
	{
		while (!sheets.isEmpty())
			close(sheets.peekLast());
	}

	void onTouchObserved(MotionEvent event)
	{
		chrome.onTouchObserved(event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event)
	{
		final boolean handled = super.dispatchTouchEvent(event);
		if (handled && event.getActionMasked() == MotionEvent.ACTION_DOWN)
			chrome.onTouchClaimed();
		return handled;
	}

	@Override
	public void onState(JSONObject state)
	{
		final JSONObject oculars = state.optJSONObject("oculars");
		final boolean instrument = OcularBar.active(oculars);
		ocularBar.onState(oculars);
		if (instrument != inOcularView)
		{
			inOcularView = instrument;
			if (instrument)
				chrome.setPointing(false);
			updateChromeVisibility();
		}

		chrome.onState(state);
		for (Sheet sheet : new java.util.ArrayList<>(sheets))
			sheet.onState(state);

		final JSONObject toggles = state.optJSONObject("toggles");
		if (toggles == null)
			return;
		if (toggles.has("actionShow_Night_Mode"))
			setNight(toggles.optBoolean("actionShow_Night_Mode"));
		if (toggles.has("actionSet_Full_Screen_Global")
				&& getContext() instanceof AsteriumActivity)
			((AsteriumActivity) getContext()).setFullScreen(
					toggles.optBoolean("actionSet_Full_Screen_Global"));
	}

	private void setNight(boolean value)
	{
		if (value == night)
			return;
		night = value;
		if (!value)
		{
			setLayerType(LAYER_TYPE_NONE, null);
			return;
		}
		final android.graphics.Paint paint = new android.graphics.Paint();
		paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(new float[] {
				0.30f, 0.59f, 0.11f, 0f, 0f,
				0f,    0f,    0f,    0f, 0f,
				0f,    0f,    0f,    0f, 0f,
				0f,    0f,    0f,    1f, 0f }));
		setLayerType(LAYER_TYPE_HARDWARE, paint);
	}

	@Override
	public void onEngineReady()
	{
		T.refresh(() ->
		{
			chrome.retranslate();
			ocularBar.retranslate();
			setVisibility(VISIBLE);
			chrome.onEngineReady();
		});
		if (getContext() instanceof AsteriumActivity)
			((AsteriumActivity) getContext()).locateOnStart();
	}

	void relanguage()
	{
		T.refresh(() ->
		{
			final String top = topSheetId();
			closeAll();
			chrome.retranslate();
			ocularBar.retranslate();
			chrome.rebuildToolbar();
			if (top != null)
				openById(top);
		});
	}
}
