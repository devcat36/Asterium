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
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * The bar that appears when something on the sky is selected: what it is, and
 * the two things people do next - centre on it, or read the rest.
 *
 * Deliberately one line tall. The desktop's information panel is a wall of
 * twenty fields; on a phone that is most of the sky, covering the very object
 * it is describing. The rest is in InfoSheet, which the tab on top of this card
 * is the handle for: tap it, or pull it up and the sheet comes with the finger.
 */
final class InfoCard extends LinearLayout
{
	/** The tab standing above the card, and the chevron on it. */
	private static final int TAB_DP = 14;

	private final Overlay overlay;
	private final TextView nameText;
	private final TextView subText;
	private final ImageView centreButton;
	private final ImageView ocularButton;
	private boolean tracking = false;
	private final android.graphics.drawable.Drawable tab;
	private final android.graphics.Paint chevron = new android.graphics.Paint(
			android.graphics.Paint.ANTI_ALIAS_FLAG);
	private boolean centred = false;
	private boolean tabShown = true;
	/** The sheet being pulled up, while a finger is still on it. */
	private InfoSheet dragging;
	private float dragStartY;
	private boolean flung;
	private boolean flungDown;
	private boolean dismissing;

	InfoCard(Context context, Overlay overlay)
	{
		super(context);
		this.overlay = overlay;
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);
		tab = Theme.box(Theme.PANEL_SHEER, 7, 0x59FDD886);
		setBackground(new android.graphics.drawable.InsetDrawable(
				Theme.pressable(Theme.box(Theme.PANEL_SHEER, 14, 0x59FDD886), 14), 0, Theme.dp(TAB_DP), 0, 0));
		Theme.padding(this, 14, TAB_DP + 10, 10, 10);
		setVisibility(GONE);
		chevron.setStyle(android.graphics.Paint.Style.STROKE);
		chevron.setStrokeWidth(Theme.dp(1.5f));
		chevron.setStrokeCap(android.graphics.Paint.Cap.ROUND);
		chevron.setColor(0xCCAAADA4);
		setClickable(true);
		setContentDescription(T.t("Show all information"));
		setOnClickListener(v -> overlay.openById("info"));

		final GestureDetector fling = new GestureDetector(context,
				new GestureDetector.SimpleOnGestureListener()
		{
			@Override
			public boolean onFling(MotionEvent down, MotionEvent up, float velocityX, float velocityY)
			{
				flung = velocityY < 0 && Math.abs(velocityY) > Math.abs(velocityX);
				flungDown = velocityY > 0 && Math.abs(velocityY) > Math.abs(velocityX);
				return false;
			}
		});
		final int slop = ViewConfiguration.get(context).getScaledTouchSlop();
		setOnTouchListener((v, event) ->
		{
			fling.onTouchEvent(event);
			switch (event.getActionMasked())
			{
				case MotionEvent.ACTION_DOWN:
					animate().cancel();
					dragStartY = event.getRawY();
					flung = false;
					flungDown = false;
					dismissing = false;
					return false;
				case MotionEvent.ACTION_MOVE:
					if (dragging == null && (dismissing || event.getRawY() - dragStartY > slop))
					{
						setPressed(false);
						dismissing = true;
						followDown(event.getRawY() - dragStartY);
						return true;
					}
					if (dragging == null && dragStartY - event.getRawY() > slop
							&& !overlay.isOpen("info"))
					{
						setPressed(false);
						dragging = new InfoSheet(getContext(), overlay);
						overlay.openDragging(dragging);
					}
					if (dragging == null)
						return false;
					dragging.dragTo(event.getRawY());
					return true;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					if (dismissing)
					{
						dismissing = false;
						settleDown(event.getActionMasked() == MotionEvent.ACTION_UP
								&& (flungDown || event.getRawY() - dragStartY > getHeight() * 0.4f));
						return true;
					}
					if (dragging == null)
						return false;
					dragging.settle(flung);
					dragging = null;
					return true;
				default:
					return false;
			}
		});

		final LinearLayout titles = new LinearLayout(context);
		titles.setOrientation(VERTICAL);

		nameText = Theme.text(context, "", 16, Theme.ACCENT, false);
		nameText.setSingleLine(true);
		nameText.setEllipsize(android.text.TextUtils.TruncateAt.END);
		titles.addView(nameText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		subText = Theme.text(context, "", 11, Theme.TEXT_DIM, false);
		subText.setSingleLine(true);
		subText.setEllipsize(android.text.TextUtils.TruncateAt.END);
		titles.addView(subText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		addView(titles, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

		ocularButton = roundButton(context, "btOcular", "Ocular view", false, v ->
		{
			overlay.setPointing(false);
			NativeBridge.set("Oculars.enableOcular", true);
		});
		ocularButton.setVisibility(GONE);
		addView(ocularButton);

		centreButton = roundButton(context, "btGotoSelectedObject", "Centre on object", false, v ->
		{
			if (overlay.isPointing())
			{
				overlay.setFinding(!overlay.isFinding());
				refreshCentre();
			}
			else
				NativeBridge.send("center");
		});
		addView(centreButton);
	}

	private void followDown(float dy)
	{
		final float shift = Math.max(0f, dy);
		setTranslationY(shift);
		setAlpha(Math.max(0f, 1f - shift / Math.max(1, getHeight())));
	}

	private void settleDown(boolean away)
	{
		if (away)
			animate().translationY(getHeight()).alpha(0f).setDuration(180)
			         .withEndAction(() -> NativeBridge.send("unselect")).start();
		else
			animate().translationY(0f).alpha(1f).setDuration(140).start();
	}

	/**
	 * The handle: a tab standing out of the top of the card with a chevron on
	 * it, saying which way the card goes.
	 *
	 * Drawn here rather than added as a view - it is a mark on the card, not
	 * something to hit on its own, and the whole card is already the target.
	 * The tab is drawn taller than its strip and clipped to it, so its bottom
	 * edge and corners fall away and it reads as standing out of the card
	 * rather than as a second box resting on top of one.
	 */
	@Override
	protected void dispatchDraw(android.graphics.Canvas canvas)
	{
		super.dispatchDraw(canvas);
		if (!tabShown)
			return;
		final int strip = Theme.dp(TAB_DP);
		final int width = Theme.dp(64);
		final int left = (getWidth() - width) / 2;
		canvas.save();
		canvas.clipRect(0, 0, getWidth(), strip);
		tab.setBounds(left, 0, left + width, strip + Theme.dp(10));
		tab.draw(canvas);
		canvas.restore();

		final float cx = getWidth() / 2f;
		final float apex = strip * 0.38f;
		final float arm = Theme.dp(5);
		canvas.drawLine(cx - arm, apex + arm * 0.6f, cx, apex, chevron);
		canvas.drawLine(cx, apex, cx + arm, apex + arm * 0.6f, chevron);
	}

	int tabHeight()
	{
		return getVisibility() == VISIBLE && tabShown ? Theme.dp(TAB_DP) : 0;
	}

	void setTabShown(boolean value)
	{
		if (value == tabShown)
			return;
		tabShown = value;
		final int strip = value ? Theme.dp(TAB_DP) : 0;
		setBackground(new android.graphics.drawable.InsetDrawable(
				Theme.pressable(Theme.box(Theme.PANEL_SHEER, 14, 0x59FDD886), 14), 0, strip, 0, 0));
		Theme.padding(this, 14, (value ? TAB_DP : 0) + 10, 10, 10);
	}

	void retranslate()
	{
		setContentDescription(T.t("Show all information"));
		centreButton.setContentDescription(T.t("Centre on object"));
		ocularButton.setContentDescription(T.t("Ocular view"));
	}

	private ImageView roundButton(Context context, String iconStem, String label, boolean accent,
	                              OnClickListener action)
	{
		final ImageView button = new ImageView(context);
		button.setScaleType(ImageView.ScaleType.FIT_CENTER);
		button.setContentDescription(T.t(label));
		button.setTag(iconStem);
		Theme.padding(button, 11, 11, 11, 11);
		button.setOnClickListener(action);
		final LayoutParams params = new LayoutParams(Theme.dp(Theme.TOUCH), Theme.dp(Theme.TOUCH));
		params.leftMargin = Theme.dp(6);
		button.setLayoutParams(params);
		styleRound(button, accent);
		return button;
	}

	private void styleRound(ImageView button, boolean on)
	{
		button.setImageDrawable(Theme.icon(getContext(), (String) button.getTag(), on));
		button.setAlpha(on ? 1f : 0.8f);
		button.setBackground(Theme.pressableCircle(on
				? Theme.circleAccent()
				: Theme.circle(Theme.FILL_SOFT, Theme.HAIRLINE)));
	}

	void onState(JSONObject state)
	{
		final JSONObject selection = state.optJSONObject("sel");
		if (selection == null)
		{
			setVisibility(GONE);
			setTranslationY(0f);
			setAlpha(1f);
			return;
		}
		setVisibility(VISIBLE);
		nameText.setText(T.t(selection.optString("name")));
		final String magnitude = selection.optString("mag");
		final String type = selection.optString("sub");
		final String mag = magnitude.isEmpty() ? "" : String.format(T.t("mag %s"), magnitude);
		subText.setText(mag.isEmpty() || type.isEmpty() ? mag + type : mag + "  ·  " + type);
		ocularButton.setVisibility(state.optJSONObject("oculars") == null ? GONE : VISIBLE);
		tracking = state.optBoolean("tracking");
		refreshCentre();
	}

	void refreshCentre()
	{
		final boolean lit = overlay.isPointing() ? overlay.isFinding() : tracking;
		if (lit == centred)
			return;
		centred = lit;
		styleRound(centreButton, lit);
	}
}
