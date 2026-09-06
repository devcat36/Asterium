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

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

final class ColorPicker
{
	private ColorPicker() {}

	interface Picked
	{
		void set(int color);
	}

	static void show(Context context, String title, int initial, Picked picked)
	{
		final float[] hsv = new float[3];
		Color.colorToHSV(initial, hsv);

		final Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);
		column.setBackground(Theme.box(Theme.PANEL_SOLID, 16, Theme.PANEL_EDGE));
		Theme.padding(column, 18, 16, 18, 14);

		final GradientDrawable preview = Theme.circle(initial, Theme.PANEL_EDGE);
		final LinearLayout heading = new LinearLayout(context);
		heading.setOrientation(LinearLayout.HORIZONTAL);
		heading.setGravity(Gravity.CENTER_VERTICAL);
		heading.addView(Theme.text(context, title, 15, Theme.TEXT, false),
				new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		final View well = new View(context);
		well.setBackground(preview);
		heading.addView(well, new LinearLayout.LayoutParams(Theme.dp(30), Theme.dp(30)));
		column.addView(heading);

		final Field field = new Field(context, hsv);
		final LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(210));
		fieldParams.topMargin = Theme.dp(14);
		column.addView(field, fieldParams);

		final Bar bar = new Bar(context, hsv);
		final LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(30));
		barParams.topMargin = Theme.dp(16);
		column.addView(bar, barParams);

		final LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(46));
		doneParams.topMargin = Theme.dp(16);
		column.addView(Widgets.primaryButton(context, "Done", v -> dialog.dismiss()), doneParams);

		final Runnable changed = () ->
		{
			final int color = Color.HSVToColor(hsv);
			preview.setColor(color);
			field.invalidate();
			bar.invalidate();
			picked.set(color);
		};
		field.onChange = changed;
		bar.onChange = changed;

		dialog.setContentView(column);
		final Window window = dialog.getWindow();
		if (window != null)
		{
			window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			final int screen = context.getResources().getDisplayMetrics().widthPixels;
			window.setLayout(Math.min(Theme.dp(330), screen - Theme.dp(40)),
			                 WindowManager.LayoutParams.WRAP_CONTENT);
		}
		dialog.show();
	}

	private static final class Field extends View
	{
		private final float[] hsv;
		private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
		private float shaderHue = -1f;
		Runnable onChange;

		Field(Context context, float[] hsv)
		{
			super(context);
			this.hsv = hsv;
			ring.setStyle(Paint.Style.STROKE);
			ring.setStrokeWidth(Theme.dp(2));
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldW, int oldH)
		{
			shaderHue = -1f;
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			final float w = getWidth(), h = getHeight();
			if (shaderHue != hsv[0])
			{
				shaderHue = hsv[0];
				paint.setShader(new ComposeShader(
						new LinearGradient(0, 0, w, 0, Color.WHITE,
								Color.HSVToColor(new float[] { hsv[0], 1f, 1f }),
								Shader.TileMode.CLAMP),
						new LinearGradient(0, 0, 0, h, Color.WHITE, Color.BLACK,
								Shader.TileMode.CLAMP),
						PorterDuff.Mode.MULTIPLY));
			}
			final float radius = Theme.dp(10);
			canvas.drawRoundRect(0, 0, w, h, radius, radius, paint);

			final float edge = Theme.dp(11);
			final float x = Math.max(edge, Math.min(w - edge, hsv[1] * w));
			final float y = Math.max(edge, Math.min(h - edge, (1f - hsv[2]) * h));
			ring.setColor(0xCC000000);
			canvas.drawCircle(x, y, Theme.dp(10), ring);
			ring.setColor(0xFFFFFFFF);
			canvas.drawCircle(x, y, Theme.dp(8), ring);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event)
		{
			final android.view.ViewParent parent = getParent();
			if (parent != null)
				parent.requestDisallowInterceptTouchEvent(true);
			hsv[1] = clamp(event.getX() / getWidth());
			hsv[2] = 1f - clamp(event.getY() / getHeight());
			if (onChange != null)
				onChange.run();
			return true;
		}

		@Override
		public boolean performClick()
		{
			return super.performClick();
		}
	}

	private static final class Bar extends View
	{
		private static final int[] SPECTRUM = {
			0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
			0xFF0000FF, 0xFFFF00FF, 0xFFFF0000,
		};

		private final float[] hsv;
		private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
		Runnable onChange;

		Bar(Context context, float[] hsv)
		{
			super(context);
			this.hsv = hsv;
			marker.setStyle(Paint.Style.STROKE);
			marker.setStrokeWidth(Theme.dp(3));
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldW, int oldH)
		{
			paint.setShader(new LinearGradient(0, 0, w, 0, SPECTRUM, null, Shader.TileMode.CLAMP));
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			final float w = getWidth(), h = getHeight();
			final float radius = h / 2f;
			canvas.drawRoundRect(0, 0, w, h, radius, radius, paint);

			final float half = Theme.dp(6);
			final float x = half + (w - 2f * half) * hsv[0] / 360f;
			marker.setColor(0xCC000000);
			canvas.drawRoundRect(x - half, 1f, x + half, h - 1f, half, half, marker);
			marker.setColor(0xFFFFFFFF);
			marker.setStrokeWidth(Theme.dp(2));
			canvas.drawRoundRect(x - half, 2f, x + half, h - 2f, half, half, marker);
			marker.setStrokeWidth(Theme.dp(3));
		}

		@Override
		public boolean onTouchEvent(MotionEvent event)
		{
			final android.view.ViewParent parent = getParent();
			if (parent != null)
				parent.requestDisallowInterceptTouchEvent(true);
			hsv[0] = clamp(event.getX() / getWidth()) * 360f;
			if (onChange != null)
				onChange.run();
			return true;
		}

		@Override
		public boolean performClick()
		{
			return super.performClick();
		}
	}

	private static float clamp(float value)
	{
		if (Float.isNaN(value))
			return 0f;
		return Math.max(0f, Math.min(1f, value));
	}

	static String hex(int color)
	{
		return String.format(java.util.Locale.US, "#%06x", color & 0xFFFFFF);
	}

	static int parse(String hex)
	{
		try
		{
			return Color.parseColor(hex);
		}
		catch (Exception e)
		{
			return Color.WHITE;
		}
	}
}
