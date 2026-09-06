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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

final class OcularBar extends FrameLayout
{
	private static final int EDGE = 12;

	private static final String[][] MODES = {
		{ "on",     "Oculars.enableOcular", "btOcular", "Ocular view"        },
		{ "sensor", "Oculars.enableCCD",    "btSensor", "Image sensor frame" },
		{ "telrad", "Oculars.enableTelrad", "btTelrad", "Telrad sight"       },
	};

	private final LinearLayout bar;
	private final LinearLayout handle;
	private final TextView handleText;
	private final TextView exit;
	private final ImageView[] modes = new ImageView[MODES.length];
	private final boolean[] lit = new boolean[MODES.length];
	private final ImageView crosshairs;
	private final ImageView settings;
	private boolean crosshairsLit = false;
	private final Overlay overlay;
	private Sheet dragging;
	private float dragStartY;
	private boolean flung;
	private Rect edges = new Rect();
	private boolean placedWide = false;
	private boolean placed = false;

	OcularBar(Context context, Overlay overlay)
	{
		super(context);
		this.overlay = overlay;
		setClickable(false);
		setVisibility(GONE);

		bar = new LinearLayout(context);
		bar.setOrientation(LinearLayout.HORIZONTAL);
		bar.setGravity(Gravity.CENTER_VERTICAL);
		bar.setBackground(Theme.box(0xE61F1F1F, 26, Theme.PANEL_EDGE));
		Theme.padding(bar, 6, 6, 6, 6);
		bar.setClickable(true);

		exit = Theme.text(context, "✕", 19, Theme.TEXT_DIM, false);
		exit.setGravity(Gravity.CENTER);
		exit.setContentDescription(T.t("Leave the eyepiece view"));
		exit.setBackground(plate());
		exit.setOnClickListener(v -> leave());
		bar.addView(exit, cell());

		for (int i = 0; i < MODES.length; ++i)
		{
			final int index = i;
			modes[i] = glyph(context, MODES[i][2], MODES[i][3],
					v -> NativeBridge.set(MODES[index][1], !lit[index]));
			bar.addView(modes[i]);
		}

		bar.addView(divider(context));

		crosshairs = glyph(context, "btCrosshairs", "Show crosshairs",
				v -> NativeBridge.set("Oculars.enableCrosshairs", !crosshairsLit));
		bar.addView(crosshairs);

		settings = new ImageView(context);
		settings.setImageDrawable(Theme.layers(Theme.TEXT));
		settings.setScaleType(ImageView.ScaleType.FIT_CENTER);
		settings.setContentDescription(T.t("Oculars"));
		Theme.padding(settings, 11, 11, 11, 11);
		settings.setAlpha(0.7f);
		settings.setBackground(plate());
		settings.setOnClickListener(v -> overlay.openById("oculars"));
		bar.addView(settings, cell());

		addView(bar, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
		                              Gravity.TOP | Gravity.CENTER_HORIZONTAL));

		handle = new LinearLayout(context);
		handle.setOrientation(LinearLayout.HORIZONTAL);
		handle.setGravity(Gravity.CENTER_VERTICAL);
		handle.setBackground(Theme.pressable(Theme.box(0xE61F1F1F, 22, Theme.PANEL_EDGE), 22));
		Theme.padding(handle, 14, 8, 16, 8);
		handle.setMinimumHeight(Theme.dp(Theme.TOUCH));
		handle.setContentDescription(T.t("Oculars"));
		handle.setOnClickListener(v -> overlay.openById("ocularpanel"));
		handle.addView(Theme.text(context, "▲", 10, Theme.ACCENT, false));
		handleText = Theme.text(context, "", 13, Theme.TEXT_CHIP, false);
		handleText.setSingleLine(true);
		handleText.setEllipsize(android.text.TextUtils.TruncateAt.END);
		handleText.setMaxWidth(Theme.dp(190));
		final LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		textParams.leftMargin = Theme.dp(9);
		handle.addView(handleText, textParams);
		attachPull(context);
		addView(handle, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
		                                 Gravity.BOTTOM | Gravity.END));
	}

	private void attachPull(Context context)
	{
		final GestureDetector fling = new GestureDetector(context,
				new GestureDetector.SimpleOnGestureListener()
		{
			@Override
			public boolean onFling(MotionEvent down, MotionEvent up, float velocityX, float velocityY)
			{
				flung = velocityY < 0 && Math.abs(velocityY) > Math.abs(velocityX);
				return false;
			}
		});
		final int slop = ViewConfiguration.get(context).getScaledTouchSlop();
		handle.setOnTouchListener((v, event) ->
		{
			fling.onTouchEvent(event);
			switch (event.getActionMasked())
			{
				case MotionEvent.ACTION_DOWN:
					dragStartY = event.getRawY();
					flung = false;
					return false;
				case MotionEvent.ACTION_MOVE:
					if (dragging == null && dragStartY - event.getRawY() > slop
							&& !overlay.isOpen("ocularpanel"))
					{
						handle.setPressed(false);
						dragging = new OcularPanelSheet(getContext(), overlay);
						overlay.openDragging(dragging);
					}
					if (dragging == null)
						return false;
					dragging.dragTo(event.getRawY());
					return true;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					if (dragging == null)
						return false;
					dragging.settle(flung);
					dragging = null;
					return true;
				default:
					return false;
			}
		});
	}

	private ImageView glyph(Context context, String stem, String label, OnClickListener action)
	{
		final ImageView button = new ImageView(context);
		button.setScaleType(ImageView.ScaleType.FIT_CENTER);
		button.setContentDescription(T.t(label));
		button.setTag(stem);
		Theme.padding(button, 10, 10, 10, 10);
		button.setBackground(plate());
		button.setOnClickListener(action);
		button.setLayoutParams(cell());
		paint(button, false);
		return button;
	}

	private void paint(ImageView button, boolean on)
	{
		button.setImageDrawable(Theme.icon(getContext(), (String) button.getTag(), on));
		button.setAlpha(on ? 1f : 0.55f);
	}

	private android.graphics.drawable.Drawable plate()
	{
		return Theme.pressableCircle(Theme.circle(Color.TRANSPARENT, Color.TRANSPARENT));
	}

	private static LinearLayout.LayoutParams cell()
	{
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				Theme.dp(Theme.TOUCH), Theme.dp(Theme.TOUCH));
		params.leftMargin = params.rightMargin = Theme.dp(1);
		return params;
	}

	private View divider(Context context)
	{
		final View rule = new View(context);
		rule.setBackgroundColor(0x26FFFFFF);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				Math.max(1, Theme.dp(1)), Theme.dp(22));
		params.leftMargin = params.rightMargin = Theme.dp(5);
		rule.setLayoutParams(params);
		return rule;
	}

	void leave()
	{
		for (int i = 0; i < MODES.length; ++i)
			if (lit[i])
				NativeBridge.set(MODES[i][1], false);
	}

	void setInsets(Rect insets)
	{
		edges = new Rect(insets);
		placed = false;
		place();
	}

	void place()
	{
		final boolean wide = getResources().getConfiguration().orientation
				== Configuration.ORIENTATION_LANDSCAPE;
		if (placed && wide == placedWide)
			return;
		placed = true;
		placedWide = wide;

		final LayoutParams barParams = (LayoutParams) bar.getLayoutParams();
		barParams.gravity = Gravity.TOP | (wide ? Gravity.END : Gravity.CENTER_HORIZONTAL);
		barParams.topMargin = (edges.top > 0 ? edges.top : Theme.dp(10)) + Theme.dp(8);
		barParams.rightMargin = wide ? edges.right + Theme.dp(EDGE) : 0;
		bar.setLayoutParams(barParams);

		final LayoutParams handleParams = (LayoutParams) handle.getLayoutParams();
		handleParams.rightMargin = edges.right + Theme.dp(EDGE);
		handleParams.bottomMargin = edges.bottom + Theme.dp(EDGE);
		handle.setLayoutParams(handleParams);
	}

	static boolean active(JSONObject oculars)
	{
		if (oculars == null)
			return false;
		for (String[] mode : MODES)
			if (oculars.optBoolean(mode[0]))
				return true;
		return false;
	}

	void onState(JSONObject oculars)
	{
		if (oculars == null)
			return;
		for (int i = 0; i < MODES.length; ++i)
		{
			final boolean on = oculars.optBoolean(MODES[i][0]);
			if (on == lit[i])
				continue;
			lit[i] = on;
			paint(modes[i], on);
		}
		crosshairs.setVisibility(lit[0] ? VISIBLE : GONE);
		handle.setVisibility(lit[0] || lit[1] ? VISIBLE : GONE);
		if (lit[0] || lit[1])
		{
			final String label = T.t(oculars.optString(lit[1] ? "sensorName" : "eyepiece"));
			if (!label.contentEquals(handleText.getText()))
				handleText.setText(label);
		}

		final boolean marked = oculars.optBoolean("crosshairs");
		if (marked == crosshairsLit)
			return;
		crosshairsLit = marked;
		paint(crosshairs, marked);
	}

	void retranslate()
	{
		exit.setContentDescription(T.t("Leave the eyepiece view"));
		for (int i = 0; i < MODES.length; ++i)
			modes[i].setContentDescription(T.t(MODES[i][3]));
		crosshairs.setContentDescription(T.t("Show crosshairs"));
		settings.setContentDescription(T.t("Oculars"));
		handle.setContentDescription(T.t("Oculars"));
	}
}
