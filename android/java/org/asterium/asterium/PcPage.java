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
import android.graphics.Paint;
import android.graphics.Path;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

final class PcPage extends AstroCalcPage
{
	private JSONObject state = new JSONObject();
	private FrameLayout body;
	private LinearLayout root;
	private boolean graphs = false;
	private TextView firstValue, secondValue;

	@Override
	View build()
	{
		root = new LinearLayout(context());
		root.setOrientation(LinearLayout.VERTICAL);
		body = new FrameLayout(context());
		buildTabs();
		root.addView(body, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
		return root;
	}

	@Override
	void onShow()
	{
		refresh();
	}

	private void buildTabs()
	{
		if (root.getChildCount() > 0 && root.getChildAt(0) != body)
			root.removeViewAt(0);
		root.addView(subTabs(new String[] { "Data", "Graphs" }, graphs ? 1 : 0, index ->
		{
			graphs = (index == 1);
			buildTabs();
			refresh();
		}), 0);
	}

	private void refresh()
	{
		NativeBridge.request(graphs ? "pc.graph" : "pc", "", payload -> { state = payload; show(); });
	}

	private void set(String key, String id)
	{
		NativeBridge.send("pc.set", key + "=" + id);
		refresh();
	}

	private void show()
	{
		if (body == null)
			return;
		body.removeAllViews();
		final ScrollView scroll = new ScrollView(context());
		final LinearLayout column = column(scroll);
		column.addView(pickers());
		if (graphs)
			fillGraphs(column);
		else
			fillData(column);
		body.addView(scroll);
		host.setSummary(state.optString("title"));
	}

	private View pickers()
	{
		final LinearLayout group = new LinearLayout(context());
		group.setOrientation(LinearLayout.VERTICAL);
		final JSONArray bodies = state.optJSONArray("bodies");

		firstValue = pickerValue();
		setPickerValue(firstValue, state.optString("first"), bodies);
		group.addView(pickerRow("First body", "First celestial body", firstValue,
				() -> state.optJSONArray("bodies"), id -> set("first_celestial_body", id)));

		secondValue = pickerValue();
		setPickerValue(secondValue, state.optString("second"), bodies);
		group.addView(pickerRow("Second body", "Second celestial body", secondValue,
				() -> state.optJSONArray("bodies"), id -> set("second_celestial_body", id)));
		group.addView(Widgets.hairline(context()));
		return group;
	}

	private void fillData(LinearLayout column)
	{
		final Context context = context();
		final JSONArray rows = state.optJSONArray("data");
		for (int i = 0; rows != null && i < rows.length(); ++i)
		{
			final JSONObject entry = rows.optJSONObject(i);
			if (entry != null)
				column.addView(Widgets.valueRow(context, entry.optString("label"),
						entry.optString("value")));
		}
		column.addView(Widgets.gap(context, 12));
	}

	private void fillGraphs(LinearLayout column)
	{
		final Context context = context();
		final JSONObject graph = state.optJSONObject("graph");
		final JSONArray au = graph == null ? null : graph.optJSONArray("au");
		if (au == null || au.length() == 0)
		{
			column.addView(note("Pick two different bodies to see how their distance changes."));
			return;
		}

		final DistancePlot plot = new DistancePlot(context);
		plot.setData(graph);
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(240));
		params.setMargins(Theme.dp(8), Theme.dp(8), Theme.dp(8), Theme.dp(4));
		column.addView(plot, params);

		final LinearLayout legend = new LinearLayout(context);
		legend.setOrientation(LinearLayout.HORIZONTAL);
		legend.setGravity(Gravity.CENTER);
		Theme.padding(legend, 16, 0, 16, 8);
		legend.addView(Theme.text(context, "— " + state.optString("auLabel"), 11,
				DistancePlot.LINEAR, false));
		final JSONArray deg = graph.optJSONArray("deg");
		if (deg != null && deg.length() > 0)
		{
			final TextView angular = Theme.text(context, "    — " + state.optString("degLabel"),
					11, DistancePlot.ANGULAR, false);
			legend.addView(angular);
		}
		column.addView(legend);
		column.addView(Widgets.gap(context, 12));
	}

	private static final class DistancePlot extends View
	{
		static final int LINEAR = 0xFF7CFC7C;

		static final int ANGULAR = Theme.ACCENT;

		private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
		private JSONObject data;

		DistancePlot(Context context)
		{
			super(context);
			grid.setStyle(Paint.Style.STROKE);
			grid.setStrokeWidth(Math.max(1, Theme.dp(1)));
			grid.setColor(Theme.FILL_SOFT);
			label.setColor(Theme.TEXT_DIM);
			label.setTextSize(Theme.dp(9));
			label.setTypeface(Theme.sans());
			line.setStyle(Paint.Style.STROKE);
			line.setStrokeWidth(Math.max(2, Theme.dp(2)));
			line.setStrokeJoin(Paint.Join.ROUND);
		}

		void setData(JSONObject data)
		{
			this.data = data;
			invalidate();
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			if (data == null)
				return;
			final JSONArray au = data.optJSONArray("au");
			final JSONArray deg = data.optJSONArray("deg");
			if (au == null || au.length() < 2)
				return;

			final float left = Theme.dp(38);
			final float right = getWidth() - Theme.dp(38);
			final float top = Theme.dp(10);
			final float bottom = getHeight() - Theme.dp(18);
			if (right <= left || bottom <= top)
				return;

			final double[] auRange = range(au);
			final double[] degRange = range(deg);

			for (int i = 0; i <= 4; ++i)
			{
				final float y = bottom - (bottom - top) * i / 4f;
				canvas.drawLine(left, y, right, y, grid);
				label.setColor(LINEAR);
				label.setTextAlign(Paint.Align.RIGHT);
				canvas.drawText(trim(auRange[0] + (auRange[1] - auRange[0]) * i / 4., auRange[1] - auRange[0]),
						left - Theme.dp(4), y + Theme.dp(3), label);
				if (deg != null && deg.length() > 1)
				{
					label.setColor(ANGULAR);
					label.setTextAlign(Paint.Align.LEFT);
					canvas.drawText(trim(degRange[0] + (degRange[1] - degRange[0]) * i / 4., degRange[1] - degRange[0]),
							right + Theme.dp(4), y + Theme.dp(3), label);
				}
			}
			canvas.drawLine(left, top, left, bottom, grid);
			canvas.drawLine(right, top, right, bottom, grid);

			label.setColor(Theme.TEXT_DIM);
			label.setTextAlign(Paint.Align.CENTER);
			final JSONArray ticks = data.optJSONArray("ticks");
			for (int i = 0; ticks != null && i < ticks.length(); ++i)
			{
				final JSONObject tick = ticks.optJSONObject(i);
				if (tick == null)
					continue;
				final float x = left + (right - left) * (float) tick.optDouble("pos", 0.);
				canvas.drawLine(x, top, x, bottom, grid);
				canvas.drawText(tick.optString("label"), x, bottom + Theme.dp(13), label);
			}

			final float now = left + (right - left) * (float) data.optDouble("now", 0.5);
			grid.setColor(Theme.TEXT_FAINT);
			canvas.drawLine(now, top, now, bottom, grid);
			grid.setColor(Theme.FILL_SOFT);

			line.setColor(LINEAR);
			canvas.drawPath(curve(au, auRange, left, right, top, bottom), line);
			if (deg != null && deg.length() > 1)
			{
				line.setColor(ANGULAR);
				canvas.drawPath(curve(deg, degRange, left, right, top, bottom), line);
			}
		}

		private static Path curve(JSONArray values, double[] range, float left, float right,
		                          float top, float bottom)
		{
			final Path path = new Path();
			final double span = range[1] - range[0];
			for (int i = 0; i < values.length(); ++i)
			{
				final float x = left + (right - left) * i / (float) (values.length() - 1);
				final double f = span <= 0. ? 0.5 : (values.optDouble(i, 0.) - range[0]) / span;
				final float y = bottom - (float) f * (bottom - top);
				if (i == 0)
					path.moveTo(x, y);
				else
					path.lineTo(x, y);
			}
			return path;
		}

		private static double[] range(JSONArray values)
		{
			if (values == null || values.length() == 0)
				return new double[] { 0., 1. };
			double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			for (int i = 0; i < values.length(); ++i)
			{
				final double v = values.optDouble(i, 0.);
				min = Math.min(min, v);
				max = Math.max(max, v);
			}
			if (max - min < 1e-9)
			{
				min -= 0.5;
				max += 0.5;
			}
			final double pad = (max - min) * 0.05;
			return new double[] { min - pad, max + pad };
		}

		private static String trim(double value, double span)
		{
			final double step = Math.abs(span) / 4.;
			int decimals = 0;
			while (decimals < 6 && step * Math.pow(10., decimals) < 5.)
				++decimals;
			return String.format(Locale.getDefault(), "%." + decimals + "f", value);
		}
	}
}
