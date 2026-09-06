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
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class GraphsPage extends AstroCalcPage
{
	private static final String[] TABS = { "alt", "azi", "me", "xy", "lunar" };
	private static final String[] LABELS = { "Altitude vs. Time", "Azimuth vs. Time",
	                                         "Monthly Elevation", "Graphs", "Lunar Elongation" };

	private JSONObject state = new JSONObject();
	private int tab = 0;
	private FrameLayout tabHost;
	private TextView title, readout;
	private GraphView chart;
	private LinearLayout legend, body, controls;

	@Override
	View build()
	{
		final Context context = context();
		host.addButton("Update", false, this::refresh);

		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);

		tabHost = new FrameLayout(context);
		column.addView(tabHost, fill());
		buildTabs();

		final ScrollView scroll = new ScrollView(context);
		body = column(scroll);

		title = Theme.text(context, "", 13, Theme.TEXT_CHIP, false);
		Theme.padding(title, 16, 6, 16, 2);
		body.addView(title);

		chart = new GraphView(context);
		final LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(250));
		chartParams.setMargins(Theme.dp(8), Theme.dp(4), Theme.dp(8), 0);
		body.addView(chart, chartParams);

		readout = Theme.text(context, "", 11, Theme.ACCENT, true);
		readout.setGravity(Gravity.CENTER_HORIZONTAL);
		Theme.padding(readout, 16, 4, 16, 0);
		body.addView(readout, fill());
		chart.setReadout(text -> readout.setText(T.t(text)));

		legend = new LinearLayout(context);
		legend.setOrientation(LinearLayout.HORIZONTAL);
		legend.setGravity(Gravity.CENTER_HORIZONTAL);
		Theme.padding(legend, 12, 6, 12, 2);
		final android.widget.HorizontalScrollView legendScroll =
				new android.widget.HorizontalScrollView(context);
		legendScroll.setHorizontalScrollBarEnabled(false);
		legendScroll.addView(legend);
		body.addView(legendScroll, fill());

		controls = new LinearLayout(context);
		controls.setOrientation(LinearLayout.VERTICAL);
		body.addView(controls, fill());

		column.addView(scroll, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
		return column;
	}

	@Override
	void onShow()
	{
		refresh();
	}

	private void buildTabs()
	{
		final View rail = subTabs(LABELS, tab, index ->
		{
			tab = index;
			setActiveTab(tabHost.getChildAt(0), tab);
			refresh();
		});
		tabHost.removeAllViews();
		tabHost.addView(rail);
	}

	private void refresh()
	{
		NativeBridge.request("graphs", TABS[tab], payload -> { state = payload; show(); });
	}

	private void set(String key, String value)
	{
		NativeBridge.send("graphs.set", key + "=" + value);
		refresh();
	}

	private static String whole(String value)
	{
		try
		{
			return String.valueOf(Math.round(Double.parseDouble(value)));
		}
		catch (NumberFormatException e)
		{
			return "0";
		}
	}

	private void show()
	{
		final Context context = context();
		final JSONArray series = state.optJSONArray("series");
		final boolean drawable = series != null && series.length() > 0;

		title.setText(T.t(state.optString("title")));
		title.setVisibility(state.optString("title").isEmpty() ? View.GONE : View.VISIBLE);
		chart.setData(state);
		chart.setVisibility(drawable ? View.VISIBLE : View.GONE);
		readout.setText(T.t(""));

		legend.removeAllViews();
		for (int i = 0; drawable && i < series.length(); ++i)
		{
			final JSONObject one = series.optJSONObject(i);
			if (one == null)
				continue;
			legend.addView(swatch(context, colorOf(one.optString("id")), one.optString("name")));
		}

		controls.removeAllViews();
		final String note = state.optString("note");
		if (!note.isEmpty())
			controls.addView(note(note));
		if (!drawable)
			host.setSummary(note.isEmpty() ? "Nothing to plot." : note);
		else
			host.setSummary("Tap the chart to read a value.");

		switch (TABS[tab])
		{
			case "alt":   buildAltControls(); break;
			case "me":    buildMonthlyControls(); break;
			case "xy":    buildCurveControls(); break;
			default:      break;
		}
	}

	private void buildAltControls()
	{
		controls.addView(Widgets.section(context(), "Also plot"));
		controls.addView(switchRow("Sun", "Its elevation, and the civil, nautical and astronomical twilights",
				state.optBoolean("sun", false),
				(button, checked) -> set("altvstime_sun", checked ? "true" : "false")));
		controls.addView(switchRow("Moon", null, state.optBoolean("moon", false),
				(button, checked) -> set("altvstime_moon", checked ? "true" : "false")));

		controls.addView(Widgets.section(context(), "Scale"));
		final boolean positive = state.optBoolean("positiveOnly", false);
		controls.addView(switchRow("Positive values only", "Cut the scale off below the limit",
				positive, (button, checked) -> set("altvstime_positive_only", checked ? "true" : "false")));
		if (positive)
			controls.addView(sliderRow("Altitude limit, °", -85., 85.,
					state.optDouble("positiveLimit", 0.),
					v -> set("altvstime_positive_limit", whole(v))));
	}

	private void buildMonthlyControls()
	{
		controls.addView(sliderRow("Local time, h", 0., 23., state.optDouble("hour", 0.),
				v -> set("me_time", whole(v))));
		final boolean positive = state.optBoolean("positiveOnly", false);
		controls.addView(switchRow("Positive values only", "Cut the scale off below the limit",
				positive, (button, checked) -> set("me_positive_only", checked ? "true" : "false")));
		if (positive)
			controls.addView(sliderRow("Altitude limit, °", 0., 85.,
					state.optDouble("positiveLimit", 0.),
					v -> set("me_positive_limit", whole(v))));
	}

	private void buildCurveControls()
	{
		final TextView bodyValue = pickerValue();
		setPickerValue(bodyValue, state.optString("body"), state.optJSONArray("bodies"));
		controls.addView(pickerRow("Object", "Celestial body", bodyValue,
				() -> state.optJSONArray("bodies"), id -> set("graphs_celestial_body", id)));

		final TextView firstValue = pickerValue();
		setPickerValue(firstValue, state.optString("first"), state.optJSONArray("firstFunctions"));
		controls.addView(pickerRow("First graph", "First graph", firstValue,
				() -> state.optJSONArray("firstFunctions"), id -> set("graphs_first_id", id)));

		final TextView secondValue = pickerValue();
		setPickerValue(secondValue, state.optString("second"), state.optJSONArray("secondFunctions"));
		controls.addView(pickerRow("Second graph", "Second graph", secondValue,
				() -> state.optJSONArray("secondFunctions"), id -> set("graphs_second_id", id)));

		controls.addView(numberRow("Duration, months", state.optInt("duration", 1),
				value -> set("graphs_duration", String.valueOf(Math.max(1, Math.min(600, value))))));
		controls.addView(numberRow("Step, hours", state.optInt("step", 1),
				value -> set("graphs_step", String.valueOf(Math.max(1, Math.min(240, value))))));

		final int step = state.optInt("step", 1), used = state.optInt("effectiveStep", step);
		if (used > step)
			controls.addView(note("Sampled every " + used + " h instead of " + step
					+ " h: a curve that long has more points than the screen has pixels."));

		controls.addView(note(state.optString("note").isEmpty()
				? "This tool works on Earth only. Short steps over a long duration are slow."
				: "Short steps over a long duration are slow."));
	}

	private static View swatch(Context context, int color, String name)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		Theme.padding(row, 6, 2, 6, 2);
		final View dash = new View(context);
		dash.setBackground(Theme.box(color, 1));
		row.addView(dash, new LinearLayout.LayoutParams(Theme.dp(14), Theme.dp(3)));
		final TextView label = Theme.text(context, name, 10, Theme.TEXT_DIM, false);
		Theme.padding(label, 5, 0, 0, 0);
		row.addView(label);
		return row;
	}

	private static int colorOf(String id)
	{
		switch (id)
		{
			case "sun":      return 0xFFFFB020;
			case "civil":    return 0xFFAFEEEE;
			case "nautical": return 0xFF63A8FF;
			case "astro":    return 0xFF8E93E0;
			case "moon":     return 0xFF6BE8A6;
			case "first":    return 0xFF8CE87C;
			case "second":   return Theme.ACCENT;
			default:         return 0xFFFF6E5A;
		}
	}

	private interface Readout { void say(String text); }

	private static final class GraphView extends View
	{
		private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Path path = new Path();

		private JSONObject data = new JSONObject();
		private Readout readout;
		private float touchX = Float.NaN;

		private float plotLeft, plotRight, plotTop, plotBottom;
		private double xMin, xMax;

		GraphView(Context context)
		{
			super(context);
			frame.setStyle(Paint.Style.STROKE);
			frame.setStrokeWidth(Math.max(1, Theme.dp(1)));
			frame.setColor(Theme.TEXT_FAINT);
			grid.setStyle(Paint.Style.STROKE);
			grid.setStrokeWidth(Math.max(1, Theme.dp(1)));

			grid.setColor((Theme.TEXT_FAINT & 0x00FFFFFF) | 0x38000000);
			text.setColor(Theme.TEXT_DIM);
			text.setTextSize(Theme.dp(9));
			text.setTypeface(Theme.sans());
			stroke.setStyle(Paint.Style.STROKE);
			stroke.setStrokeWidth(Math.max(2, Theme.dp(1.6f)));
			stroke.setStrokeJoin(Paint.Join.ROUND);
		}

		void setReadout(Readout readout)
		{
			this.readout = readout;
		}

		void setData(JSONObject data)
		{
			this.data = data == null ? new JSONObject() : data;
			touchX = Float.NaN;
			invalidate();
		}

		private JSONArray series()
		{
			final JSONArray list = data.optJSONArray("series");
			return list == null ? new JSONArray() : list;
		}

		private void readXRange()
		{
			xMin = data.optDouble("xMin", Double.NaN);
			xMax = data.optDouble("xMax", Double.NaN);
			if (!Double.isNaN(xMin) && !Double.isNaN(xMax) && xMax > xMin)
				return;
			xMin = Double.MAX_VALUE;
			xMax = -Double.MAX_VALUE;
			final JSONArray list = series();
			for (int i = 0; i < list.length(); ++i)
			{
				final JSONArray points = list.optJSONObject(i) == null
						? null : list.optJSONObject(i).optJSONArray("points");
				for (int j = 0; points != null && j < points.length(); ++j)
				{
					final JSONObject sample = points.optJSONObject(j);
					if (sample == null)
						continue;
					xMin = Math.min(xMin, sample.optDouble("x"));
					xMax = Math.max(xMax, sample.optDouble("x"));
				}
			}
			if (xMax <= xMin)
			{
				xMin = 0.;
				xMax = 1.;
			}
		}

		private double[] yRange(boolean right)
		{
			final String prefix = right ? "y2" : "y";
			double low = data.optDouble(prefix + "Min", 0.);
			double high = data.optDouble(prefix + "Max", 1.);
			if (high - low < 1e-9)
			{
				low -= 1.;
				high += 1.;
			}
			final double pad = (high - low) * 0.08;
			if (!data.optBoolean("positiveOnly", false))
				low -= pad;
			return new double[] { low, high + pad };
		}

		private float px(double x)
		{
			return (float) (plotLeft + (x - xMin) / (xMax - xMin) * (plotRight - plotLeft));
		}

		private float py(double y, double[] range, boolean reverse)
		{
			double fraction = (y - range[0]) / (range[1] - range[0]);
			if (reverse)
				fraction = 1. - fraction;
			return (float) (plotBottom - fraction * (plotBottom - plotTop));
		}

		private SimpleDateFormat formatter(String pattern)
		{
			final SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());

			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			return format;
		}

		private static String number(double value, double span)
		{
			final String pattern = span >= 100. ? "%.0f" : (span >= 10. ? "%.1f" : "%.2f");
			return String.format(Locale.getDefault(), pattern, value);
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			final boolean twoAxes = data.has("y2Min");
			plotLeft = Theme.dp(38);
			plotRight = getWidth() - (twoAxes ? Theme.dp(38) : Theme.dp(12));
			plotTop = Theme.dp(10);
			plotBottom = getHeight() - Theme.dp(26);
			if (plotRight <= plotLeft || plotBottom <= plotTop)
				return;
			readXRange();

			final double[] left = yRange(false);
			final double[] right = twoAxes ? yRange(true) : left;
			final boolean leftReverse = data.optBoolean("yReverse", false);
			final boolean rightReverse = data.optBoolean("y2Reverse", false);

			text.setTextAlign(Paint.Align.RIGHT);
			for (int i = 0; i <= 4; ++i)
			{
				final float y = plotBottom - i * (plotBottom - plotTop) / 4f;
				canvas.drawLine(plotLeft, y, plotRight, y, grid);
				final double value = leftReverse
						? left[1] - i * (left[1] - left[0]) / 4.
						: left[0] + i * (left[1] - left[0]) / 4.;
				canvas.drawText(number(value, left[1] - left[0]),
						plotLeft - Theme.dp(3), y + Theme.dp(3), text);
			}
			if (twoAxes)
			{
				text.setTextAlign(Paint.Align.LEFT);
				for (int i = 0; i <= 4; ++i)
				{
					final float y = plotBottom - i * (plotBottom - plotTop) / 4f;
					final double value = rightReverse
							? right[1] - i * (right[1] - right[0]) / 4.
							: right[0] + i * (right[1] - right[0]) / 4.;
					canvas.drawText(number(value, right[1] - right[0]),
							plotRight + Theme.dp(3), y + Theme.dp(3), text);
				}
			}

			final SimpleDateFormat format = formatter(data.optString("xFormat", "d MMM"));
			final int ticks = Math.max(2, Math.min(6, (int) ((plotRight - plotLeft) / Theme.dp(56))));
			for (int i = 0; i <= ticks; ++i)
			{
				final float x = plotLeft + i * (plotRight - plotLeft) / ticks;
				canvas.drawLine(x, plotTop, x, plotBottom, grid);
				text.setTextAlign(i == 0 ? Paint.Align.LEFT
						: (i == ticks ? Paint.Align.RIGHT : Paint.Align.CENTER));
				canvas.drawText(format.format(new Date((long) (xMin + i * (xMax - xMin) / ticks))),
						x, plotBottom + Theme.dp(13), text);
			}
			canvas.drawRect(plotLeft, plotTop, plotRight, plotBottom, frame);

			final double now = data.optDouble("now", Double.NaN);
			if (!Double.isNaN(now) && now >= xMin && now <= xMax)
			{
				stroke.setColor(0xFFE87CC8);
				stroke.setPathEffect(new DashPathEffect(new float[] { Theme.dp(4), Theme.dp(4) }, 0));
				canvas.drawLine(px(now), plotTop, px(now), plotBottom, stroke);
				stroke.setPathEffect(null);
				text.setTextAlign(Paint.Align.LEFT);
				text.setColor(0xFFE87CC8);
				canvas.drawText("now", px(now) + Theme.dp(3), plotTop + Theme.dp(9), text);
				text.setColor(Theme.TEXT_DIM);
			}

			final double transit = data.optDouble("transit", Double.NaN);
			if (!Double.isNaN(transit) && transit >= xMin && transit <= xMax)
			{
				stroke.setColor(0xFF5FD0DC);
				stroke.setPathEffect(new DashPathEffect(new float[] { Theme.dp(2), Theme.dp(5) }, 0));
				canvas.drawLine(px(transit), plotTop, px(transit), plotBottom, stroke);
				stroke.setPathEffect(null);
			}

			final JSONArray list = series();
			for (int i = 0; i < list.length(); ++i)
			{
				final JSONObject one = list.optJSONObject(i);
				final JSONArray points = one == null ? null : one.optJSONArray("points");
				if (points == null || points.length() == 0)
					continue;
				final boolean onRight = one.optInt("axis", 0) == 1;
				final double[] range = onRight ? right : left;
				final boolean reverse = onRight ? rightReverse : leftReverse;
				path.reset();
				for (int j = 0; j < points.length(); ++j)
				{
					final JSONObject sample = points.optJSONObject(j);
					if (sample == null)
						continue;
					final float x = px(sample.optDouble("x"));
					final float y = py(sample.optDouble("y"), range, reverse);
					if (j == 0)
						path.moveTo(x, y);
					else
						path.lineTo(x, y);
				}
				stroke.setColor(colorOf(one.optString("id")));
				canvas.drawPath(path, stroke);
			}

			if (!Float.isNaN(touchX))
			{
				stroke.setColor(Theme.TEXT_DIM);
				canvas.drawLine(touchX, plotTop, touchX, plotBottom, stroke);
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent event)
		{
			if (event.getAction() == MotionEvent.ACTION_DOWN)
			{
				if (getParent() != null)
					getParent().requestDisallowInterceptTouchEvent(true);
			}
			else if (event.getAction() == MotionEvent.ACTION_UP
			         || event.getAction() == MotionEvent.ACTION_CANCEL)
			{
				if (getParent() != null)
					getParent().requestDisallowInterceptTouchEvent(false);
			}
			touchX = Math.max(plotLeft, Math.min(plotRight, event.getX()));
			report();
			invalidate();
			return true;
		}

		private void report()
		{
			if (readout == null || plotRight <= plotLeft)
				return;
			final double x = xMin + (touchX - plotLeft) / (plotRight - plotLeft) * (xMax - xMin);
			final StringBuilder line = new StringBuilder(
					formatter(data.optString("xFormat", "d MMM").contains("HH")
							? "HH:mm" : "d MMM yyyy").format(new Date((long) x)));

			final JSONArray list = series();
			for (int i = 0; i < list.length(); ++i)
			{
				final JSONObject one = list.optJSONObject(i);
				final JSONArray points = one == null ? null : one.optJSONArray("points");
				if (points == null || points.length() == 0)
					continue;

				double best = Double.MAX_VALUE, value = 0.;
				for (int j = 0; j < points.length(); ++j)
				{
					final JSONObject sample = points.optJSONObject(j);
					if (sample == null)
						continue;
					final double distance = Math.abs(sample.optDouble("x") - x);
					if (distance < best)
					{
						best = distance;
						value = sample.optDouble("y");
					}
				}
				line.append("   ").append(one.optString("name")).append(' ')
				    .append(String.format(Locale.getDefault(), "%.2f", value));
			}
			readout.say(line.toString());
		}
	}
}
