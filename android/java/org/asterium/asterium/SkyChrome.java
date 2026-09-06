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

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

final class SkyChrome extends FrameLayout
{
	private final Overlay overlay;

	private final LinearLayout statusBar;
	private final TextView placeText;
	private final TextView statsText;
	private final ImageView gear;
	private final LinearLayout statusColumn;

	private final LinearLayout windowRail;
	private final View[] railButtons;
	private final LinearLayout placeRow;
	private int railRows = 0;
	private int railSize = 0;
	private final LinearLayout bottom;
	private final GradientDrawable bottomWash;
	private final InfoCard infoCard;
	private final HorizontalScrollView toolbarScroll;
	private final LinearLayout toolbar;
	private final LinearLayout transport;
	private final LinearLayout bottomRow;
	private Rect edges = new Rect();
	private boolean sideBySide = false;
	private final TextView clockText;
	private final TextView rateText;
	private final ImageView rewindKey;
	private final ImageView playKey;
	private final ImageView nowKey;
	private final ImageView forwardKey;
	private final FrameLayout keyPages;
	private final View[] keyPageViews;
	private final LinearLayout pageDots;
	private int keyPage = 0;
	private int dragPeer = -1;

	private boolean expanded = false;
	private static final long IDLE_HIDE_MS = 10_000;
	private static final long FADE_MS = 260;
	private final Runnable autoHide = new Runnable()
	{
		@Override
		public void run()
		{
			if (overlay.topSheetId() != null)
				postDelayed(this, IDLE_HIDE_MS);
			else
				setExpanded(false);
		}
	};
	private boolean realtimeRate = false;
	private boolean toolbarBuilt = false;
	private final SkyPointer pointer;
	private final ImageView pointerToggle;
	private final FinderView finder;
	private boolean finding = false;
	private boolean lastTracking = false;
	private int lastFindNudge = -1;
	private View pointerCell;
	private ValueAnimator washFade;
	private float touchX, touchY;
	private boolean touchJudged = false;
	private final int touchSlop;
	private final boolean tablet;

	private static final int RAIL_GAP = 12;

	private static final int GEAR_TOP = 25;

	private static final float POINTER_SIZE = Theme.TOUCH * 0.9f;

	private static final int BOTTOM_GAP = 8;

	private static final String[][] WINDOWS = {
		{ "location",  "bbtLocation",  "Location"  },
		{ "time",      "bbtTime",      "Date/time" },
		{ "view",      "bbtSky",       "View"      },
		{ "search",    "bbtSearch",    "Search"    },
		{ "obslist",   "btObsList",    "Observing list" },
		{ "config",    "bbtSettings",  "Config"    },
		{ "astrocalc", "bbtAstroCalc", "AstroCalc" },
		{ "help",      "bbtHelp",      "Help"      },
	};

	SkyChrome(Context context, Overlay overlay)
	{
		super(context);
		this.overlay = overlay;
		this.tablet = overlay.isTablet();
		this.pointer = new SkyPointer(context);
		this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		setClickable(false);

		finder = new FinderView(context);
		pointer.setAimListener(finder::setAim);
		addView(finder, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		statusBar = new LinearLayout(context);
		statusBar.setOrientation(LinearLayout.VERTICAL);

		placeRow = new LinearLayout(context);
		placeRow.setOrientation(LinearLayout.HORIZONTAL);
		placeRow.setBaselineAligned(true);
		Theme.padding(placeRow, 4, 8, 4, 4);
		placeRow.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 8), 8));
		placeRow.setOnClickListener(v -> overlay.openById("location"));
		placeRow.setContentDescription(T.t("Location"));
		statusBar.addView(placeRow, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		placeText = Theme.text(context, "…", 11, Theme.TEXT_DIM, true);
		placeText.setMaxLines(1);
		placeText.setEllipsize(android.text.TextUtils.TruncateAt.END);
		placeText.setShadowLayer(Theme.dp(3), 0, Theme.dp(1), 0xC0000000);
		placeRow.addView(placeText, new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

		statsText = Theme.text(context, "", 11, Theme.TEXT_DIM, true);
		statsText.setMaxLines(1);
		statsText.setGravity(Gravity.END);
		statsText.setShadowLayer(Theme.dp(3), 0, Theme.dp(1), 0xC0000000);
		final LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		statsParams.leftMargin = Theme.dp(10);
		placeRow.addView(statsText, statsParams);

		final LayoutParams topParams = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.TOP);
		addView(statusBar, topParams);

		final LinearLayout rightColumn = new LinearLayout(context);
		rightColumn.setOrientation(LinearLayout.VERTICAL);
		rightColumn.setGravity(Gravity.END);

		gear = new ImageView(context);
		gear.setImageDrawable(Theme.gear(Theme.TEXT));
		gear.setScaleType(ImageView.ScaleType.FIT_CENTER);
		Theme.padding(gear, 9, 9, 9, 9);
		gear.setContentDescription(T.t("Show controls"));
		gear.setBackground(Theme.pressableCircle(
				Theme.circle(Color.TRANSPARENT, Color.TRANSPARENT)));
		gear.setOnClickListener(v -> setExpanded(!expanded));
		final LinearLayout.LayoutParams gearParams = new LinearLayout.LayoutParams(
				Theme.dp(Theme.TOUCH), Theme.dp(Theme.TOUCH));
		gearParams.gravity = Gravity.END;
		rightColumn.addView(gear, gearParams);

		statusColumn = rightColumn;
		addView(rightColumn, new LayoutParams(
				Theme.dp(Theme.TOUCH), LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.TOP));

		pointerToggle = new ImageView(context);
		pointerToggle.setScaleType(ImageView.ScaleType.FIT_CENTER);
		pointerToggle.setContentDescription(T.t("Move the sky with the phone"));
		final int pointerPad = Theme.dp(POINTER_SIZE * 3f / Theme.TOUCH);
		pointerToggle.setPadding(pointerPad, pointerPad, pointerPad, pointerPad);
		pointerToggle.setOnClickListener(v -> setPointing(!pointer.isOn()));
		pointerToggle.setBackground(Theme.pressableCircle(
				Theme.circle(Color.TRANSPARENT, Color.TRANSPARENT)));
		pointerToggle.setImageDrawable(Theme.icon(context, "gyro", false));
		stylePointerToggle();
		placePointerToggle();
		addView(pointerToggle, new LayoutParams(
				Theme.dp(POINTER_SIZE), Theme.dp(POINTER_SIZE), Gravity.START | Gravity.TOP));

		windowRail = new LinearLayout(context);
		windowRail.setOrientation(LinearLayout.HORIZONTAL);
		windowRail.setBackground(Theme.box(0x991F1F1F, 14, Theme.HAIRLINE));
		Theme.padding(windowRail, 6, 6, 6, 6);
		windowRail.setVisibility(GONE);
		railButtons = new View[WINDOWS.length];
		for (int i = 0; i < WINDOWS.length; ++i)
			railButtons[i] = railButton(context, WINDOWS[i][0], WINDOWS[i][1], WINDOWS[i][2]);
		final LayoutParams railParams = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				(tablet ? Gravity.START : Gravity.END) | Gravity.TOP);
		railParams.rightMargin = railParams.leftMargin = Theme.dp(12);
		addView(windowRail, railParams);

		bottom = new LinearLayout(context);
		bottom.setOrientation(LinearLayout.VERTICAL);
		bottomWash = new GradientDrawable(
				GradientDrawable.Orientation.BOTTOM_TOP,
				new int[] { 0xF504060A, 0xF504060A, 0x0004060A });
		bottomWash.setAlpha(0);
		bottom.setBackground(bottomWash);

		infoCard = new InfoCard(context, overlay);
		final LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		cardParams.leftMargin = cardParams.rightMargin = Theme.dp(12);
		cardParams.bottomMargin = Theme.dp(10);
		bottom.addView(infoCard, cardParams);

		toolbar = new LinearLayout(context);
		toolbar.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(toolbar, 12, 0, 12, 0);
		toolbarScroll = new HorizontalScrollView(context);
		toolbarScroll.setHorizontalScrollBarEnabled(false);
		toolbarScroll.setHorizontalFadingEdgeEnabled(true);
		toolbarScroll.setFadingEdgeLength(Theme.dp(28));
		toolbarScroll.addView(toolbar);
		toolbarScroll.setVisibility(GONE);

		bottomRow = new LinearLayout(context);
		bottomRow.setOrientation(LinearLayout.VERTICAL);
		bottomRow.setGravity(Gravity.BOTTOM);
		bottom.addView(bottomRow, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		final LinearLayout.LayoutParams toolbarParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		toolbarParams.bottomMargin = Theme.dp(10);
		bottomRow.addView(toolbarScroll, toolbarParams);

		transport = new LinearLayout(context);
		transport.setOrientation(LinearLayout.HORIZONTAL);
		transport.setGravity(Gravity.CENTER_VERTICAL);
		transport.setBackground(Theme.box(0x991F1F1F, 14, Theme.HAIRLINE));
		Theme.padding(transport, 10, 8, 10, 8);

		final LinearLayout keys = new LinearLayout(context);
		keys.setOrientation(LinearLayout.HORIZONTAL);
		rewindKey = transportKey(context, "btTimeRewind", "Slow down",
		                         v -> NativeBridge.send("action", "actionDecrease_Time_Speed"));
		playKey = transportKey(context, "btTimeRealtime", "Real time speed", v -> playPause());
		nowKey = transportKey(context, "btTimeNow", "Now",
		                      v -> NativeBridge.send("time.now"));
		forwardKey = transportKey(context, "btTimeForward", "Speed up",
		                          v -> NativeBridge.send("action", "actionIncrease_Time_Speed"));
		keys.addView(rewindKey);
		keys.addView(playKey);
		keys.addView(nowKey);
		keys.addView(forwardKey);

		keyPages = new FrameLayout(context)
		{
			private float downX, downY;
			private boolean swiping = false;

			@Override
			public boolean onInterceptTouchEvent(MotionEvent event)
			{
				switch (event.getActionMasked())
				{
					case MotionEvent.ACTION_DOWN:
						downX = event.getX();
						downY = event.getY();
						swiping = false;
						break;
					case MotionEvent.ACTION_MOVE:
						if (!swiping)
						{
							if (Math.abs(event.getX() - downX) <= touchSlop
									|| Math.abs(event.getX() - downX)
										<= Math.abs(event.getY() - downY))
								break;
							swiping = true;
							downX = event.getX();
							beginKeyDrag();
						}
						dragKeyPages(event.getX() - downX);
						break;
					default:
						break;
				}
				return swiping;
			}

			@Override
			public boolean onTouchEvent(MotionEvent event)
			{
				onInterceptTouchEvent(event);
				final int action = event.getActionMasked();
				if (swiping && (action == MotionEvent.ACTION_UP
						|| action == MotionEvent.ACTION_CANCEL))
				{
					settleKeyPages(action == MotionEvent.ACTION_UP ? event.getX() - downX : 0f);
					swiping = false;
				}
				return true;
			}
		};
		keyPageViews = new View[] {
			keys,
			stepPage(context, new String[] { "−4h", "−1h", "+1h", "+4h" },
			         new int[] { -14400, -3600, 3600, 14400 }),
			stepPage(context, new String[] { "−1w", "−1d", "+1d", "+1w" },
			         new int[] { -604800, -86400, 86400, 604800 }),
		};
		for (int i = 0; i < keyPageViews.length; ++i)
		{
			keyPageViews[i].setVisibility(i == 0 ? VISIBLE : GONE);
			keyPages.addView(keyPageViews[i], new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.WRAP_CONTENT,
					FrameLayout.LayoutParams.WRAP_CONTENT));
		}

		pageDots = new LinearLayout(context);
		pageDots.setOrientation(LinearLayout.HORIZONTAL);
		for (int i = 0; i < keyPageViews.length; ++i)
		{
			final LinearLayout.LayoutParams dotParams =
					new LinearLayout.LayoutParams(Theme.dp(4), Theme.dp(4));
			dotParams.leftMargin = dotParams.rightMargin = Theme.dp(2);
			pageDots.addView(new View(context), dotParams);
		}
		paintDots();
		keyPages.addView(pageDots, new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));

		final LinearLayout clockColumn = new LinearLayout(context);
		clockColumn.setOrientation(LinearLayout.VERTICAL);
		clockColumn.setGravity(Gravity.START);
		clockText = Theme.text(context, "", 13, Theme.TEXT, true);
		rateText = Theme.text(context, "", 10, Theme.TEXT_DIM, true);
		clockColumn.addView(clockText);
		clockColumn.addView(rateText);
		clockColumn.setOnClickListener(v -> overlay.openById("time"));
		clockColumn.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 8), 8));
		Theme.padding(clockColumn, 4, 4, 8, 4);
		final LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		transport.addView(clockColumn, clockParams);
		transport.addView(keyPages);

		final LinearLayout.LayoutParams transportParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		transportParams.leftMargin = transportParams.rightMargin = Theme.dp(12);
		bottomRow.addView(transport, transportParams);

		addView(bottom, new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottomEdge)
	{
		super.onLayout(changed, left, top, right, bottomEdge);
		layoutRail();
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec)
	{
		layoutBottom(MeasureSpec.getSize(widthSpec));
		super.onMeasure(widthSpec, heightSpec);
	}

	private int transportWidth()
	{
		final float clock = clockText.getPaint().measureText("0000-00-00 00:00:00");
		final float rate = rateText.getPaint().measureText("UTC+00:00 · ×-10000");
		return Math.round(Math.max(clock, rate))
				+ 4 * Theme.dp(Theme.TOUCH) + Theme.dp(32);
	}

	private void layoutBottom(int room)
	{
		final int width = transportWidth();
		final boolean wide = tablet
				|| getResources().getConfiguration().orientation
						== Configuration.ORIENTATION_LANDSCAPE
				|| room - edges.left - edges.right >= 2 * width + Theme.dp(BOTTOM_GAP + 24);
		if (wide == sideBySide)
			return;
		sideBySide = wide;
		bottomRow.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

		final LinearLayout.LayoutParams toolbarParams =
				(LinearLayout.LayoutParams) toolbarScroll.getLayoutParams();
		toolbarParams.width = wide ? 0 : LinearLayout.LayoutParams.MATCH_PARENT;
		toolbarParams.height = wide ? LinearLayout.LayoutParams.MATCH_PARENT
		                           : LinearLayout.LayoutParams.WRAP_CONTENT;
		toolbarParams.weight = wide ? 1f : 0f;
		toolbarParams.bottomMargin = wide ? 0 : Theme.dp(10);
		toolbarScroll.setLayoutParams(toolbarParams);

		final LinearLayout.LayoutParams transportParams =
				(LinearLayout.LayoutParams) transport.getLayoutParams();
		transportParams.width = wide ? width : LinearLayout.LayoutParams.MATCH_PARENT;
		transportParams.height = wide ? LinearLayout.LayoutParams.MATCH_PARENT
		                             : LinearLayout.LayoutParams.WRAP_CONTENT;
		transportParams.leftMargin = Theme.dp(wide ? BOTTOM_GAP : 12);
		transport.setLayoutParams(transportParams);

		final LinearLayout.LayoutParams cardParams =
				(LinearLayout.LayoutParams) infoCard.getLayoutParams();
		cardParams.width = wide ? width : LinearLayout.LayoutParams.MATCH_PARENT;
		cardParams.gravity = wide ? Gravity.END : Gravity.START;
		cardParams.leftMargin = wide ? 0 : Theme.dp(12);
		infoCard.setLayoutParams(cardParams);
		infoCard.setTabShown(!wide);

		final LayoutParams railParams = (LayoutParams) windowRail.getLayoutParams();
		railParams.gravity = (railOnLeft() ? Gravity.START : Gravity.END) | Gravity.TOP;
		windowRail.setLayoutParams(railParams);
		railRows = 0;
		placePointerToggle();
		placeWash(wide);
	}

	private void placePointerToggle()
	{
		pointerToggle.setVisibility(
				pointer.available() && !(railOnLeft() && expanded) ? VISIBLE : GONE);
	}

	private void stylePointerToggle()
	{
		pointerToggle.setAlpha(pointer.isOn() ? 1f : 0.45f);
	}

	private void placeWash(boolean wide)
	{
		bottom.setPadding(edges.left, Theme.dp(10), edges.right,
		                  wide ? 0 : edges.bottom + Theme.dp(8));
		bottomRow.setPadding(0, 0, 0, wide ? edges.bottom + Theme.dp(8) : 0);
		final View bare = wide ? bottom : bottomRow;
		final View washed = wide ? bottomRow : bottom;
		bare.setBackground(null);
		washed.setBackground(bottomWash);
	}

	private boolean railOnLeft()
	{
		return tablet || sideBySide;
	}

	private void layoutRail()
	{
		final int gap = Theme.dp(RAIL_GAP);
		final boolean left = railOnLeft();
		final int from = (left ? statusBar.getBottom() : statusColumn.getBottom()) + gap;
		final int floor = left ? bottom.getTop() + bottomRow.getTop()
		                      : bottom.getTop() + bottom.getPaddingTop() + infoCard.tabHeight();
		final int band = floor - gap - from;
		final int room = band - Theme.dp(12) + Theme.dp(4);
		final int cell = Theme.dp(Theme.TOUCH + 4);
		fillRail(Math.max(1, Math.min(WINDOWS.length, room / cell)), cell - Theme.dp(4));
		windowRail.setTranslationY(from + Math.max(0, band - windowRail.getHeight()) / 2f);
	}

	private void fillRail(int rows, int size)
	{
		if (rows == railRows && size == railSize)
			return;
		railRows = rows;
		railSize = size;
		windowRail.removeAllViews();
		LinearLayout column = null;
		for (int i = 0; i < railButtons.length; ++i)
		{
			if (i % rows == 0)
			{
				column = new LinearLayout(getContext());
				column.setOrientation(LinearLayout.VERTICAL);
				final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.leftMargin = params.rightMargin = Theme.dp(2);
				windowRail.addView(column, railOnLeft() ? windowRail.getChildCount() : 0, params);
			}
			final View button = railButtons[i];
			if (button.getParent() != null)
				((LinearLayout) button.getParent()).removeView(button);
			final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
			params.bottomMargin = Theme.dp(4);
			column.addView(button, params);
		}
	}

	private View railButton(Context context, final String id, String iconStem, String label)
	{
		final ImageView button = new ImageView(context);
		button.setImageDrawable(Theme.icon(context, iconStem, false));
		button.setScaleType(ImageView.ScaleType.FIT_CENTER);
		button.setContentDescription(T.t(label));
		button.setTag(id);
		Theme.padding(button, 9, 9, 9, 9);
		button.setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 10), 10));
		button.setOnClickListener(v -> overlay.openById(id));
		return button;
	}

	private LinearLayout stepPage(Context context, String[] labels, int[] seconds)
	{
		final LinearLayout page = new LinearLayout(context);
		page.setOrientation(LinearLayout.HORIZONTAL);
		for (int i = 0; i < labels.length; ++i)
		{
			final int step = seconds[i];
			final TextView key = Theme.text(context, labels[i], 14, Theme.TEXT_CHIP, false);
			key.setGravity(Gravity.CENTER);
			key.setLetterSpacing(0.02f);
			key.setSingleLine(true);
			key.setBackground(keyBackground());
			key.setContentDescription(T.t(labels[i]));
			key.setOnClickListener(v -> NativeBridge.send("time.step", String.valueOf(step)));
			page.addView(key, new LinearLayout.LayoutParams(
					Theme.dp(Theme.TOUCH), Theme.dp(Theme.TOUCH)));
		}
		return page;
	}

	private Drawable keyBackground()
	{
		return new InsetDrawable(Theme.pressable(Theme.box(Color.TRANSPARENT, 9), 9),
		                        Theme.dp(2), Theme.dp(8), Theme.dp(2), Theme.dp(8));
	}

	private void paintDots()
	{
		for (int i = 0; i < pageDots.getChildCount(); ++i)
		{
			final View dot = pageDots.getChildAt(i);
			dot.setBackground(Theme.box(i == keyPage ? Theme.ACCENT : Theme.TEXT, 2));
			dot.setAlpha(i == keyPage ? 0.95f : 0.28f);
		}
	}

	private void beginKeyDrag()
	{
		for (int i = 0; i < keyPageViews.length; ++i)
		{
			keyPageViews[i].animate().cancel();
			keyPageViews[i].setTranslationX(0f);
			keyPageViews[i].setVisibility(i == keyPage ? VISIBLE : GONE);
		}
		dragPeer = -1;
	}

	private void dragKeyPages(float dx)
	{
		final int width = keyPages.getWidth();
		if (width == 0)
			return;
		final int peer = (keyPage + (dx < 0f ? 1 : -1) + keyPageViews.length) % keyPageViews.length;
		if (peer != dragPeer)
		{
			parkKeyPage(dragPeer);
			dragPeer = peer;
			keyPageViews[peer].setVisibility(VISIBLE);
		}
		keyPageViews[keyPage].setTranslationX(dx);
		keyPageViews[peer].setTranslationX(dx + (dx < 0f ? width : -width));
	}

	private void settleKeyPages(float dx)
	{
		if (dragPeer < 0)
			return;
		final int width = keyPages.getWidth();
		final int leaving = keyPage;
		final int peer = dragPeer;
		dragPeer = -1;
		if (Math.abs(dx) > width / 4f)
		{
			keyPageViews[leaving].animate().translationX(dx < 0f ? -width : width)
			                     .setDuration(160).withEndAction(() -> parkKeyPage(leaving));
			keyPageViews[peer].animate().translationX(0f).setDuration(160);
			keyPage = peer;
			paintDots();
		}
		else
		{
			keyPageViews[leaving].animate().translationX(0f).setDuration(160);
			keyPageViews[peer].animate().translationX(dx < 0f ? width : -width)
			                  .setDuration(160).withEndAction(() -> parkKeyPage(peer));
		}
	}

	private void parkKeyPage(int index)
	{
		if (index < 0 || index == keyPage)
			return;
		keyPageViews[index].setVisibility(GONE);
		keyPageViews[index].setTranslationX(0f);
	}

	private ImageView transportKey(Context context, String iconStem, String label,
	                               OnClickListener action)
	{
		final ImageView button = new ImageView(context);
		button.setScaleType(ImageView.ScaleType.FIT_CENTER);
		button.setContentDescription(T.t(label));
		Theme.padding(button, 11, 11, 11, 11);
		button.setOnClickListener(action);
		button.setLayoutParams(new LinearLayout.LayoutParams(
				Theme.dp(Theme.TOUCH), Theme.dp(Theme.TOUCH)));
		styleTransportKey(button, iconStem, false);
		return button;
	}

	private void styleTransportKey(ImageView key, String iconStem, boolean on)
	{
		key.setImageDrawable(Theme.icon(getContext(), iconStem, on));
		key.setAlpha(on ? 1f : 0.6f);
		key.setBackground(keyBackground());
	}

	private void playPause()
	{
		NativeBridge.send("time.rate", realtimeRate ? "0" : "1");
	}

	private View toolbarButton(Context context, final String action, String iconStem, String label,
	                           boolean on)
	{
		final LinearLayout cell = new LinearLayout(context);
		cell.setOrientation(LinearLayout.VERTICAL);
		cell.setGravity(Gravity.CENTER);
		Theme.padding(cell, 0, 7, 0, 7);

		final ImageView glyph = new ImageView(context);
		glyph.setScaleType(ImageView.ScaleType.FIT_CENTER);
		glyph.setLayoutParams(new LinearLayout.LayoutParams(Theme.dp(26), Theme.dp(26)));
		cell.addView(glyph);

		final TextView caption = Theme.text(context, label, 9, Theme.TEXT_MUTED, false);
		caption.setGravity(Gravity.CENTER_HORIZONTAL);
		caption.setMaxLines(1);
		caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
		final LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		captionParams.topMargin = Theme.dp(4);
		cell.addView(caption, captionParams);

		cell.setTag(new Object[] { action, iconStem, glyph, caption });
		cell.setOnClickListener(v -> NativeBridge.send("action", action));

		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				Theme.dp(Theme.TOOL_BUTTON), LinearLayout.LayoutParams.MATCH_PARENT);
		params.rightMargin = Theme.dp(6);
		cell.setLayoutParams(params);
		styleToolbarButton(cell, on);
		return cell;
	}

	private void styleToolbarButton(View cell, boolean on)
	{
		final Object[] parts = (Object[]) cell.getTag();
		final ImageView glyph = (ImageView) parts[2];
		final TextView caption = (TextView) parts[3];
		glyph.setImageDrawable(Theme.icon(getContext(), (String) parts[1], on));
		glyph.setAlpha(on ? 1f : 0.7f);
		caption.setTextColor(on ? Theme.ACCENT : Theme.TEXT_MUTED);
		cell.setBackground(Theme.pressable(
				on ? Theme.accentSoft(12) : Theme.box(Theme.FILL_FAINT, 12, Theme.HAIRLINE), 12));
	}

	void setInsets(Rect insets)
	{
		final int top = insets.top > 0 ? insets.top : Theme.dp(10);
		final LayoutParams statusParams = (LayoutParams) statusColumn.getLayoutParams();
		statusParams.topMargin = top + Theme.dp(GEAR_TOP);
		statusParams.rightMargin = insets.right + Theme.dp(12);
		statusColumn.setLayoutParams(statusParams);

		final LayoutParams topParams = (LayoutParams) statusBar.getLayoutParams();
		topParams.topMargin = top;
		topParams.leftMargin = insets.left + Theme.dp(12);
		topParams.rightMargin = insets.right + Theme.dp(12);
		statusBar.setLayoutParams(topParams);

		final LayoutParams railParams = (LayoutParams) windowRail.getLayoutParams();
		railParams.rightMargin = insets.right + Theme.dp(12);
		railParams.leftMargin = insets.left + Theme.dp(12);
		windowRail.setLayoutParams(railParams);

		final LayoutParams toggleParams = (LayoutParams) pointerToggle.getLayoutParams();
		toggleParams.topMargin = top + Theme.dp(GEAR_TOP)
				+ (Theme.dp(Theme.TOUCH) - Theme.dp(POINTER_SIZE)) / 2;
		toggleParams.leftMargin = insets.left + Theme.dp(12);
		pointerToggle.setLayoutParams(toggleParams);

		edges = new Rect(insets);
		placeWash(sideBySide);
	}

	void setExpanded(boolean value)
	{
		expanded = value;
		if (!tablet)
		{
			removeCallbacks(autoHide);
			if (value)
				postDelayed(autoHide, IDLE_HIDE_MS);
		}
		fade(windowRail, value);
		fade(toolbarScroll, value);
		fade(transport, value);
		fadeWash(value);
		gear.animate().alpha(value ? 1f : 0.45f).setDuration(FADE_MS);
		gear.setContentDescription(T.t(value ? "Hide controls" : "Show controls"));
		placePointerToggle();
		if (value && !toolbarBuilt)
			buildToolbar();
	}

	private void fade(View view, boolean value)
	{
		view.animate().cancel();
		if (value)
		{
			view.setAlpha(0f);
			view.setVisibility(VISIBLE);
			view.animate().alpha(1f).setDuration(FADE_MS);
		}
		else if (view.getVisibility() == VISIBLE)
		{
			view.animate().alpha(0f).setDuration(FADE_MS).withEndAction(() ->
			{
				view.setVisibility(GONE);
				view.setAlpha(1f);
			});
		}
	}

	private void fadeWash(boolean value)
	{
		if (washFade != null)
			washFade.cancel();
		washFade = ValueAnimator.ofInt(bottomWash.getAlpha(), value ? 255 : 0);
		washFade.setDuration(FADE_MS);
		washFade.addUpdateListener(a -> bottomWash.setAlpha((int) a.getAnimatedValue()));
		washFade.start();
	}

	void setActiveSheet(String id)
	{
		for (int i = 0; i < railButtons.length; ++i)
		{
			final View button = railButtons[i];
			final boolean active = button.getTag().equals(id);
			button.setBackground(Theme.pressable(
					active ? Theme.accentSoft(10) : Theme.box(Color.TRANSPARENT, 10), 10));
			((ImageView) button).setAlpha(active ? 1f : 0.72f);
			((ImageView) button).setImageDrawable(
					Theme.icon(getContext(), WINDOWS[i][1], active));
		}
	}

	void onEngineReady()
	{
		setExpanded(true);
	}

	void retranslate()
	{
		placeRow.setContentDescription(T.t("Location"));
		gear.setContentDescription(T.t(expanded ? "Hide controls" : "Show controls"));
		for (int i = 0; i < railButtons.length; ++i)
			railButtons[i].setContentDescription(T.t(WINDOWS[i][2]));
		rewindKey.setContentDescription(T.t("Slow down"));
		nowKey.setContentDescription(T.t("Now"));
		forwardKey.setContentDescription(T.t("Speed up"));
		pointerToggle.setContentDescription(T.t("Move the sky with the phone"));
		infoCard.retranslate();
	}

	private void buildToolbar()
	{
		NativeBridge.request("toolbar", "", payload ->
		{
			final JSONArray items = payload.optJSONArray("items");
			if (items == null || items.length() == 0)
				return;
			toolbar.removeAllViews();
			if (pointer.available())
			{
				pointerCell = toolbarButton(getContext(), "pointer", "gyro",
				                            "Gyro", pointer.isOn());
				pointerCell.setContentDescription(T.t("Move the sky with the phone"));
				pointerCell.setOnClickListener(v -> setPointing(!pointer.isOn()));
				toolbar.addView(pointerCell);
			}
			for (int i = 0; i < items.length(); ++i)
			{
				final JSONObject item = items.optJSONObject(i);
				if (item == null)
					continue;
				final View cell = toolbarButton(getContext(),
						item.optString("action"), "bt" + item.optString("icon"),
						item.optString("label"), item.optBoolean("on"));
				cell.setContentDescription(T.t(item.optString("description")));
				toolbar.addView(cell);
			}
			toolbarBuilt = true;
		});
	}

	void setPointing(boolean value)
	{
		final boolean resume = value && !pointer.isOn() && lastTracking;
		pointer.setEnabled(value);
		if (pointerCell != null)
			styleToolbarButton(pointerCell, pointer.isOn());
		stylePointerToggle();
		if (!pointer.isOn())
			setFinding(false);
		else if (resume)
			setFinding(true);
		infoCard.refreshCentre();
	}

	void setFinding(boolean value)
	{
		if (value == finding)
			return;
		finding = value;
		finder.setActive(value);
		infoCard.refreshCentre();
	}

	boolean isFinding()
	{
		return finding;
	}

	boolean isPointing()
	{
		return pointer.isOn();
	}

	boolean canPoint()
	{
		return pointer.available();
	}

	void rebuildToolbar()
	{
		toolbarBuilt = false;
		buildToolbar();
	}

	void onTouchObserved(MotionEvent event)
	{
		if (!pointer.isOn())
			return;
		switch (event.getActionMasked())
		{
			case MotionEvent.ACTION_DOWN:
				touchX = event.getX();
				touchY = event.getY();
				touchJudged = false;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				touchJudged = true;
				break;
			case MotionEvent.ACTION_MOVE:
				if (touchJudged)
					break;
				if (Math.hypot(event.getX() - touchX, event.getY() - touchY) < touchSlop)
					break;
				touchJudged = true;
				setPointing(false);
				break;
			default:
				break;
		}
	}

	void onTouchClaimed()
	{
		touchJudged = true;
		if (!tablet && expanded)
		{
			removeCallbacks(autoHide);
			postDelayed(autoHide, IDLE_HIDE_MS);
		}
	}

	void onState(JSONObject state)
	{
		pointer.setObserver(state.optDouble("lat", Double.NaN),
		                    state.optDouble("lon", Double.NaN));

		final String fps = state.optString("fps");
		placeText.setText(state.optString("place"));
		statsText.setText("FOV " + state.optString("fov")
				+ (fps.isEmpty() ? "" : "  " + fps + " FPS"));

		clockText.setText(state.optString("clock"));
		rateText.setText(state.optString("tz") + " · " + state.optString("rateText"));

		final double rate = state.optDouble("rate", 0.);
		final boolean stopped = rate == 0.;
		realtimeRate = state.optBoolean("realtimeRate");
		styleTransportKey(playKey, stopped ? "btTimePause" : "btTimeRealtime",
		                  stopped || realtimeRate);
		playKey.setContentDescription(T.t(stopped ? "Paused - start"
		                                          : realtimeRate ? "Pause" : "Real time speed"));
		styleTransportKey(rewindKey, "btTimeRewind", rate < 0.);
		styleTransportKey(forwardKey, "btTimeForward", rate > 0. && !realtimeRate);
		styleTransportKey(nowKey, "btTimeNow", realtimeRate && state.optBoolean("isNow"));

		final JSONObject selection = state.optJSONObject("sel");
		final boolean tracking = state.optBoolean("tracking");
		final int findNudge = state.optInt("find", -1);
		if (selection == null)
		{
			setFinding(false);
			finder.clearTarget();
		}
		else
		{
			if (pointer.isOn()
					&& (tracking || (lastFindNudge >= 0 && findNudge != lastFindNudge)))
				setFinding(true);
			final JSONArray enu = selection.optJSONArray("enu");
			if (enu != null && enu.length() >= 3)
				finder.setTarget(enu.optDouble(0), enu.optDouble(1), enu.optDouble(2));
		}
		lastFindNudge = findNudge;
		lastTracking = tracking;
		finder.setFov(state.optDouble("fov", 60.));

		infoCard.onState(state);

		final JSONObject toggles = state.optJSONObject("toggles");
		if (toggles != null)
		{
			for (int i = 0; i < toolbar.getChildCount(); ++i)
			{
				final View cell = toolbar.getChildAt(i);
				final Object[] parts = (Object[]) cell.getTag();
				final String action = (String) parts[0];
				if (toggles.has(action))
					styleToolbarButton(cell, toggles.optBoolean(action));
			}
		}
	}
}
