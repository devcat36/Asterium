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
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

class Sheet extends LinearLayout
{
	private static final float CARD_SHARE = 0.74f;

	private final String id;
	private final Overlay overlay;
	private final boolean overSky;
	private final boolean hugContent;
	private boolean docked = false;
	private int maxHeight = 0;
	private float minRatio = 0f;
	private final View statusSpacer;
	private final LinearLayout header;
	private final TextView titleView;
	private HorizontalScrollView tabRail;
	private LinearLayout tabs;
	protected final LinearLayout body;
	private ScrollView bodyScroll;
	private LinearLayout footer;
	private final FrameLayout content;
	private final DragDown drag;

	Sheet(Context context, Overlay overlay, String id, String title)
	{
		this(context, overlay, id, title, false);
	}

	Sheet(Context context, Overlay overlay, String id, String title, boolean overSky)
	{
		this(context, overlay, id, title, overSky, false);
	}

	Sheet(Context context, Overlay overlay, String id, String title, boolean overSky,
	      boolean hugContent)
	{
		super(context);
		this.id = id;
		this.overlay = overlay;
		this.overSky = overSky;
		this.hugContent = hugContent;
		setOrientation(VERTICAL);
		setBackgroundColor(overSky ? android.graphics.Color.TRANSPARENT : Theme.SHEET);
		setClickable(!overSky);

		statusSpacer = new View(context);
		addView(statusSpacer, new LayoutParams(LayoutParams.MATCH_PARENT, 0));

		header = new LinearLayout(context);
		header.setOrientation(HORIZONTAL);
		header.setGravity(Gravity.CENTER_VERTICAL);
		header.setBackground(new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { Theme.HEADER_TOP, Theme.HEADER_BOT }));
		header.setMinimumHeight(Theme.dp(Theme.TOUCH));
		Theme.padding(header, 16, 6, 4, 6);

		titleView = Theme.text(context, title, 17, Theme.TEXT_TITLE, false);
		titleView.setSingleLine(true);
		titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
		header.addView(titleView, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

		final TextView close = Theme.text(context, "✕", 20, Theme.TEXT_DIM, false);
		close.setGravity(Gravity.CENTER);
		close.setContentDescription(T.t("Close"));
		close.setBackground(Theme.pressableCircle(Theme.circle(0, 0)));
		close.setOnClickListener(v -> overlay.close(this));
		header.addView(close, new LayoutParams(Theme.dp(Theme.TOUCH), Theme.dp(Theme.TOUCH)));
		addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		final View rule = new View(context);
		rule.setBackgroundColor(0xFF000000);
		addView(rule, new LayoutParams(LayoutParams.MATCH_PARENT, Math.max(1, Theme.dp(1))));

		if (overSky)
		{
			header.setVisibility(GONE);
			rule.setVisibility(GONE);
			final View skyGap = new View(context);
			skyGap.setOnClickListener(v -> overlay.close(this));
			skyGap.setContentDescription(T.t("Close"));
			addView(skyGap, new LayoutParams(LayoutParams.MATCH_PARENT, 0,
			                                 hugContent ? 1f : 0.26f));
		}

		drag = new DragDown(context);
		content = new FrameLayout(context)
		{
			private float downY;
			private boolean claimed;

			@Override
			public boolean onInterceptTouchEvent(android.view.MotionEvent event)
			{
				if (overSky)
				{
					if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN)
					{
						downY = event.getRawY();
						claimed = false;
					}
					else if (event.getActionMasked() == android.view.MotionEvent.ACTION_MOVE
							&& !claimed && !drag.active && bodyScroll != null
							&& bodyScroll.getScrollY() == 0
							&& event.getRawY() - downY > drag.slop)
					{
						claimed = true;
						drag.begin(downY);
						return true;
					}
				}
				return super.onInterceptTouchEvent(event);
			}

			@Override
			public boolean onTouchEvent(android.view.MotionEvent event)
			{
				return claimed ? drag.onTouch(event) : super.onTouchEvent(event);
			}

			@Override
			protected void onMeasure(int widthSpec, int heightSpec)
			{
				super.onMeasure(widthSpec, heightSpec);
				if (!hugContent)
					return;
				final int cap = Math.round(
						getResources().getDisplayMetrics().heightPixels * CARD_SHARE);
				if (getMeasuredHeight() <= cap)
					return;
				super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(cap, MeasureSpec.EXACTLY));
			}
		};
		if (overSky)
		{
			final GradientDrawable card = new GradientDrawable();
			card.setColor(0xF01F1F1F);
			card.setCornerRadii(new float[] {
					Theme.dp(20), Theme.dp(20), Theme.dp(20), Theme.dp(20), 0, 0, 0, 0 });
			content.setBackground(card);
			content.setClickable(true);
		}
		addView(content, hugContent
				? new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
				: new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

		bodyScroll = new ScrollView(context);
		bodyScroll.setFillViewport(true);
		body = new LinearLayout(context);
		body.setOrientation(VERTICAL);
		bodyScroll.addView(body, new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		content.addView(bodyScroll, new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		if (overSky)
		{
			bodyScroll.setPadding(0, Theme.dp(28), 0, 0);
			content.addView(buildGrip(context), new FrameLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, Theme.dp(28), Gravity.TOP));
		}
	}

	private View buildGrip(Context context)
	{
		final FrameLayout handle = new FrameLayout(context);
		handle.setContentDescription(T.t("Close"));

		final View bar = new View(context);
		bar.setBackground(Theme.box(0x80AAADA4, 2));
		final FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
				Theme.dp(44), Theme.dp(4), Gravity.CENTER_HORIZONTAL | Gravity.TOP);
		barParams.topMargin = Theme.dp(8);
		handle.addView(bar, barParams);

		handle.setOnTouchListener((view, event) -> drag.onTouch(event));
		return handle;
	}

	private final class DragDown
	{
		final int slop;
		boolean active;
		private final float dismissAt = Theme.dp(96);
		private float startY;
		private boolean dragged;

		DragDown(Context context)
		{
			slop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
		}

		void begin(float rawY)
		{
			startY = rawY;
			dragged = true;
			active = true;
		}

		boolean onTouch(android.view.MotionEvent event)
		{
			final float dy = event.getRawY() - startY;
			switch (event.getActionMasked())
			{
				case android.view.MotionEvent.ACTION_DOWN:
					startY = event.getRawY();
					dragged = false;
					active = true;
					return true;
				case android.view.MotionEvent.ACTION_MOVE:
					if (dy > slop)
						dragged = true;
					content.setTranslationY(Math.max(0f, dy));
					return true;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					active = false;
					if (!dragged || dy > dismissAt)
					{
						content.setTranslationY(0f);
						overlay.close(Sheet.this);
					}
					else
					{
						content.animate().translationY(0f).setDuration(140).start();
					}
					return true;
				default:
					return false;
			}
		}
	}

	void slideIn()
	{
		if (!overSky)
			return;
		below();
		content.animate().translationY(0f).setDuration(220)
				.setInterpolator(new android.view.animation.DecelerateInterpolator())
				.start();
	}

	private void below()
	{
		content.setTranslationY(getResources().getDisplayMetrics().heightPixels);
	}

	void beginDrag()
	{
		if (overSky)
			below();
	}

	void dragTo(float rawY)
	{
		if (!overSky || content.getHeight() == 0)
			return;
		final int[] where = new int[2];
		content.getLocationOnScreen(where);
		final float restingTop = where[1] - content.getTranslationY();
		content.setTranslationY(Math.max(0f, rawY - restingTop));
	}

	void settle(boolean flung)
	{
		if (!overSky)
			return;
		if (flung || content.getTranslationY() < content.getHeight() * 0.65f)
			content.animate().translationY(0f).setDuration(180)
					.setInterpolator(new android.view.animation.DecelerateInterpolator())
					.withEndAction(overlay::dragSettled).start();
		else
			content.animate().translationY(getResources().getDisplayMetrics().heightPixels)
					.setDuration(160).withEndAction(() -> overlay.close(this)).start();
	}

	String id()
	{
		return id;
	}

	private static final java.util.Map<String, Integer> lastTabs = new java.util.HashMap<>();

	protected int recallTab(int count)
	{
		final Integer last = lastTabs.get(id);
		return last == null || last < 0 || last >= count ? 0 : last;
	}

	protected void rememberTab(int index)
	{
		lastTabs.put(id, index);
	}

	void setDocked(boolean docked)
	{
		if (!docked)
			return;
		this.docked = true;
		final GradientDrawable panel = new GradientDrawable();
		panel.setColor(0xF01F1F1F);
		panel.setCornerRadius(Theme.dp(16));
		panel.setStroke(Math.max(1, Theme.dp(1)), Theme.PANEL_EDGE);
		setBackground(panel);
		setClipToOutline(true);
		setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
	}

	boolean hugsContent()
	{
		return hugContent;
	}

	void setDockLimits(int maxHeight, float minHeightPerWidth)
	{
		this.maxHeight = maxHeight;
		this.minRatio = minHeightPerWidth;
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec)
	{
		super.onMeasure(widthSpec, heightSpec);
		final int measuredWidth = getMeasuredWidth();
		final int measuredHeight = getMeasuredHeight();
		int width = measuredWidth;
		int height = measuredHeight;
		if (maxHeight > 0)
			height = Math.min(height, maxHeight);
		if (minRatio > 0f && height < width * minRatio)
			width = Math.round(height / minRatio);
		if (width == measuredWidth && height == measuredHeight)
			return;
		super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
		                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
	}

	Overlay overlay()
	{
		return overlay;
	}

	protected int scrollOffset()
	{
		return bodyScroll == null ? 0 : bodyScroll.getScrollY();
	}

	protected void restoreScroll(final int offset)
	{
		if (bodyScroll == null || offset <= 0)
			return;
		bodyScroll.getViewTreeObserver().addOnGlobalLayoutListener(
				new android.view.ViewTreeObserver.OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				bodyScroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				bodyScroll.scrollTo(0, offset);
			}
		});
	}

	boolean onBack()
	{
		return false;
	}

	void onShown() {}

	void onReturn() {}

	void onState(JSONObject state) {}

	void setInsets(Rect insets)
	{
		final Rect own = docked ? new Rect(0, 0, 0, 0) : insets;
		statusSpacer.getLayoutParams().height = own.top;
		statusSpacer.requestLayout();
		setPadding(own.left, 0, own.right, 0);
		if (footer != null)
			footer.setPadding(Theme.dp(16), Theme.dp(12), Theme.dp(16), own.bottom + Theme.dp(12));
		else
			body.setPadding(0, 0, 0, own.bottom + Theme.dp(16));
	}

	protected LinearLayout addTabRail()
	{
		tabRail = new HorizontalScrollView(getContext());
		tabRail.setHorizontalScrollBarEnabled(false);
		tabs = new LinearLayout(getContext());
		tabs.setOrientation(HORIZONTAL);
		Theme.padding(tabs, 16, 12, 16, 12);
		tabRail.addView(tabs);
		addView(tabRail, 3, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		final View rule = new View(getContext());
		rule.setBackgroundColor(Theme.HAIRLINE);
		addView(rule, 4, new LayoutParams(LayoutParams.MATCH_PARENT, Math.max(1, Theme.dp(1))));
		return tabs;
	}

	protected LinearLayout addFooter()
	{
		footer = new LinearLayout(getContext());
		footer.setOrientation(HORIZONTAL);
		footer.setGravity(Gravity.CENTER_VERTICAL);
		footer.setBackgroundColor(0xF5141415);
		footer.setClickable(true);
		Theme.padding(footer, 16, 12, 16, 12);
		final View rule = new View(getContext());
		rule.setBackgroundColor(0x0FFFFFFF);
		addView(rule, new LayoutParams(LayoutParams.MATCH_PARENT, Math.max(1, Theme.dp(1))));
		addView(footer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		return footer;
	}

	protected void setBodyView(View view)
	{
		content.removeAllViews();
		content.addView(view, new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}
}
