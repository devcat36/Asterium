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
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

final class Theme
{
	private Theme() {}

	static final int SKY          = 0xFF04060A;
	static final int SHEET        = 0xFF141415;
	static final int PANEL        = 0xE01F1F1F;
	static final int PANEL_SOLID  = 0xF01F1F1F;
	static final int PANEL_SHEER  = 0xCC1F1F1F;
	static final int PANEL_EDGE   = 0xB3000000;

	static final int TEXT         = 0xFFEEEEEE;
	static final int TEXT_DIM     = 0xFFAAADA4;
	static final int TEXT_FAINT   = 0xFF787A76;
	static final int TEXT_CHIP    = 0xFFDCDFD6;
	static final int TEXT_MUTED   = 0xFF969892;
	static final int TEXT_TITLE   = 0xFF969696;

	static final int ACCENT       = 0xFFFDD886;
	static final int ACCENT_DARK  = 0xFFB1822B;
	static final int ACCENT_LIGHT = 0xFFD2CF85;
	static final int ACCENT_LINK  = 0xFFA28C42;

	static final int HEADER_TOP   = 0xFF292F32;
	static final int HEADER_BOT   = 0xFF1F2124;

	static final int TRACK_OFF    = 0xFF323233;
	static final int KNOB_OFF     = 0xFF8F8F8F;
	static final int KNOB_ON      = 0xFFF6EFDD;
	static final int SLIDER_TRACK = 0xFF363636;
	static final int THUMB_TOP    = 0xFFB7B8B9;
	static final int THUMB_BOT    = 0xFF6F7172;

	static final int FILL_SOFT    = 0x12FFFFFF;
	static final int FILL_FAINT   = 0x0BFFFFFF;
	static final int HAIRLINE     = 0x0DFFFFFF;

	static final int TOUCH        = 44;
	static final int ROW          = 56;
	static final int RAIL_BUTTON  = 48;
	static final int TOOL_BUTTON  = 58;
	static final int CHIP         = 40;
	static final int TABLET_WIDTH = 720;

	private static float density = 3f;
	private static Typeface sans;
	private static Typeface mono;
	private static final Map<String, Drawable> icons = new HashMap<>();

	static void init(Context context)
	{
		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		density = metrics.density;
		sans = fontFromAssets(context, "data/DejaVuSans.ttf", Typeface.DEFAULT);
		mono = fontFromAssets(context, "data/DejaVuSansMono.ttf", Typeface.MONOSPACE);
	}

	private static Typeface fontFromAssets(Context context, String path, Typeface fallback)
	{
		try
		{
			final File cached = new File(context.getCacheDir(), path.replace('/', '_'));
			if (!cached.exists() || cached.length() == 0)
			{
				try (InputStream in = context.getAssets().open(path);
				     FileOutputStream out = new FileOutputStream(cached))
				{
					final byte[] buffer = new byte[16384];
					int read;
					while ((read = in.read(buffer)) > 0)
						out.write(buffer, 0, read);
				}
			}
			final Typeface face = Typeface.createFromFile(cached);
			return face != null ? face : fallback;
		}
		catch (Exception e)
		{
			return fallback;
		}
	}

	static Typeface sans() { return sans != null ? sans : Typeface.DEFAULT; }
	static Typeface mono() { return mono != null ? mono : Typeface.MONOSPACE; }

	static int dp(float value)
	{
		return Math.round(value * density);
	}

	static Drawable icon(Context context, String stem, boolean on)
	{
		final String name = stem + (on ? "-on" : "-off");
		Drawable cached = icons.get(name);
		if (cached != null)
			return cached;
		if (stem.equals("gyro"))
		{
			final Drawable drawn = gyro(context, on);
			icons.put(name, drawn);
			return drawn;
		}
		Bitmap bitmap = read(context, "gui/" + name + ".png");
		if (bitmap == null)
			bitmap = read(context, "gui/" + stem + ".png");
		if (bitmap == null)
			return null;
		final Drawable drawable = new android.graphics.drawable.BitmapDrawable(
				context.getResources(), bitmap);
		icons.put(name, drawable);
		return drawable;
	}

	private static Bitmap read(Context context, String assetPath)
	{
		try (InputStream in = context.getAssets().open(assetPath))
		{
			return BitmapFactory.decodeStream(in);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	static Drawable gear(int color)
	{
		final float centre = 50f, tooth = 47f, root = 33f, hole = 14f;
		final Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
		for (int i = 0; i < 8; ++i)
		{
			final float base = i * 45f;
			vertex(path, centre, base - 17f, root, i == 0);
			vertex(path, centre, base - 11f, tooth, false);
			vertex(path, centre, base + 11f, tooth, false);
			vertex(path, centre, base + 17f, root, false);
		}
		path.close();
		path.addCircle(centre, centre, hole, Path.Direction.CCW);

		final ShapeDrawable drawable = new ShapeDrawable(new PathShape(path, 100f, 100f));
		drawable.getPaint().setColor(color);
		drawable.setIntrinsicWidth(dp(24));
		drawable.setIntrinsicHeight(dp(24));
		return drawable;
	}

	static Drawable gyro(Context context, boolean glow)
	{
		final Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
		path.addRoundRect(34f, 32f, 66f, 83.4f, 4.5f, 4.5f, Path.Direction.CW);
		path.addRoundRect(38.2f, 37.1f, 61.8f, 75.2f, 1.5f, 1.5f, Path.Direction.CW);
		path.addCircle(50f, 79.3f, 2.7f, Path.Direction.CW);

		final float mid = 36f, band = 3f, head = 9.5f, sweep = 84f;
		final float from = 270f - sweep / 2f, to = 270f + sweep / 2f;
		path.arcTo(ring(mid + band), from, sweep, true);
		path.arcTo(ring(mid - band), to, -sweep, false);
		path.close();
		vertex(path, 50f, to, mid + head, true);
		vertex(path, 50f, to, mid - head, false);
		vertex(path, 50f, to + 19f, mid, false);
		path.close();
		vertex(path, 50f, from, mid + head, true);
		vertex(path, 50f, from, mid - head, false);
		vertex(path, 50f, from - 19f, mid, false);
		path.close();

		final int size = dp(64);
		final float margin = size * 0.05f;
		final RectF bounds = new RectF();
		path.computeBounds(bounds, true);
		final Matrix fit = new Matrix();
		fit.setRectToRect(bounds, new RectF(margin, margin, size - margin, size - margin),
		                  Matrix.ScaleToFit.CENTER);
		path.transform(fit);

		final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(bitmap);
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(TEXT);
		if (glow)
		{
			final Paint halo = new Paint(paint);
			halo.setMaskFilter(new BlurMaskFilter(size * 0.05f, BlurMaskFilter.Blur.NORMAL));
			canvas.drawPath(path, halo);
		}
		canvas.drawPath(path, paint);
		return new android.graphics.drawable.BitmapDrawable(context.getResources(), bitmap);
	}

	private static RectF ring(float radius)
	{
		return new RectF(50f - radius, 50f - radius, 50f + radius, 50f + radius);
	}

	private static void vertex(Path path, float centre, float degrees, float radius, boolean first)
	{
		final double angle = Math.toRadians(degrees);
		final float x = centre + radius * (float) Math.cos(angle);
		final float y = centre + radius * (float) Math.sin(angle);
		if (first)
			path.moveTo(x, y);
		else
			path.lineTo(x, y);
	}

	static Drawable star(int color, boolean filled)
	{
		final Path path = new Path();
		for (int i = 0; i < 5; ++i)
		{
			vertex(path, 50f, i * 72f - 90f, 46f, i == 0);
			vertex(path, 50f, i * 72f - 54f, 19f, false);
		}
		path.close();
		final ShapeDrawable drawable = new ShapeDrawable(new PathShape(path, 100f, 100f));
		drawable.getPaint().setColor(color);
		if (!filled)
		{
			drawable.getPaint().setStyle(Paint.Style.STROKE);
			drawable.getPaint().setStrokeWidth(7f);
			drawable.getPaint().setStrokeJoin(Paint.Join.ROUND);
		}
		drawable.setIntrinsicWidth(dp(16));
		drawable.setIntrinsicHeight(dp(16));
		return drawable;
	}

	static GradientDrawable box(int fill, int radiusDp, int strokeColor)
	{
		final GradientDrawable shape = new GradientDrawable();
		shape.setShape(GradientDrawable.RECTANGLE);
		shape.setColor(fill);
		shape.setCornerRadius(dp(radiusDp));
		if (strokeColor != Color.TRANSPARENT)
			shape.setStroke(Math.max(1, dp(1)), strokeColor);
		return shape;
	}

	static GradientDrawable box(int fill, int radiusDp)
	{
		return box(fill, radiusDp, Color.TRANSPARENT);
	}

	static GradientDrawable circle(int fill, int strokeColor)
	{
		final GradientDrawable shape = new GradientDrawable();
		shape.setShape(GradientDrawable.OVAL);
		shape.setColor(fill);
		if (strokeColor != Color.TRANSPARENT)
			shape.setStroke(Math.max(1, dp(1)), strokeColor);
		return shape;
	}

	static GradientDrawable circleAccent()
	{
		final GradientDrawable shape = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { 0x52B1822B, 0x2ED2CF85 });
		shape.setShape(GradientDrawable.OVAL);
		shape.setStroke(Math.max(1, dp(1)), 0x80FDD886);
		return shape;
	}

	static GradientDrawable accentSolid(int radiusDp)
	{
		final GradientDrawable shape = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { ACCENT_DARK, ACCENT_LIGHT });
		shape.setCornerRadius(dp(radiusDp));
		return shape;
	}

	static GradientDrawable accentSoft(int radiusDp)
	{
		final GradientDrawable shape = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { 0x52B1822B, 0x2ED2CF85 });
		shape.setCornerRadius(dp(radiusDp));
		shape.setStroke(Math.max(1, dp(1)), 0x8CFDD886);
		return shape;
	}

	static Drawable pressable(Drawable background, int radiusDp)
	{
		final GradientDrawable mask = new GradientDrawable();
		mask.setShape(GradientDrawable.RECTANGLE);
		mask.setColor(Color.WHITE);
		mask.setCornerRadius(dp(radiusDp));
		return new RippleDrawable(
				android.content.res.ColorStateList.valueOf(0x40FDD886), background, mask);
	}

	static Drawable pressableCircle(Drawable background)
	{
		final GradientDrawable mask = new GradientDrawable();
		mask.setShape(GradientDrawable.OVAL);
		mask.setColor(Color.WHITE);
		return new RippleDrawable(
				android.content.res.ColorStateList.valueOf(0x40FDD886), background, mask);
	}

	static TextView text(Context context, String value, float sizeSp, int color, boolean monospace)
	{
		final TextView view = new TextView(context);
		view.setText(T.t(value));
		view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
		view.setTextColor(color);
		view.setTypeface(monospace ? mono() : sans());
		view.setIncludeFontPadding(false);
		return view;
	}

	static void padding(View view, int left, int top, int right, int bottom)
	{
		view.setPadding(dp(left), dp(top), dp(right), dp(bottom));
	}
}
