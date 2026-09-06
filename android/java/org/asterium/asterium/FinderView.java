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
import android.os.Build;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.View;

final class FinderView extends View
{
	private static final long FADE_MS = 260;
	private static final long PULSE_MS = 450;

	private final double[] aim = new double[6];
	private final double[] target = new double[3];
	private boolean aimed = false;
	private boolean targeted = false;
	private double fovDeg = 60.;
	private boolean active = false;
	private boolean locked = false;
	private long lockedAt = 0;
	private double lastAngle = 0.;

	private final Paint arrow = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint pulse = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path chevron = new Path();

	FinderView(Context context)
	{
		super(context);
		setVisibility(GONE);

		arrow.setStyle(Paint.Style.STROKE);
		arrow.setStrokeWidth(Theme.dp(3.5f));
		arrow.setStrokeCap(Paint.Cap.ROUND);
		arrow.setStrokeJoin(Paint.Join.ROUND);
		arrow.setColor(Theme.ACCENT);

		ring.setStyle(Paint.Style.STROKE);
		ring.setStrokeCap(Paint.Cap.ROUND);

		halo.setStyle(Paint.Style.STROKE);
		halo.setStrokeCap(Paint.Cap.ROUND);
		halo.setStrokeJoin(Paint.Join.ROUND);
		halo.setColor(0x73000000);

		pulse.setStyle(Paint.Style.STROKE);
		pulse.setStrokeWidth(Theme.dp(2));
		pulse.setColor(Theme.ACCENT);

		final float arm = Theme.dp(9);
		chevron.moveTo(-arm * 0.5f, -arm);
		chevron.lineTo(arm * 0.6f, 0);
		chevron.lineTo(-arm * 0.5f, arm);
	}

	void setAim(double[] world)
	{
		System.arraycopy(world, 0, aim, 0, 6);
		aimed = true;
		if (active)
			invalidate();
	}

	void setTarget(double east, double north, double up)
	{
		target[0] = east;
		target[1] = north;
		target[2] = up;
		targeted = true;
	}

	void clearTarget()
	{
		targeted = false;
	}

	void setFov(double value)
	{
		if (value > 0.)
			fovDeg = value;
	}

	void setActive(boolean value)
	{
		if (value == active)
			return;
		active = value;
		locked = false;
		animate().cancel();
		if (value)
		{
			setAlpha(0f);
			setVisibility(VISIBLE);
			animate().alpha(1f).setDuration(FADE_MS);
		}
		else
		{
			animate().alpha(0f).setDuration(FADE_MS).withEndAction(() -> setVisibility(GONE));
		}
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		if (!aimed || !targeted)
			return;

		final double rx = aim[1] * aim[5] - aim[2] * aim[4];
		final double ry = aim[2] * aim[3] - aim[0] * aim[5];
		final double rz = aim[0] * aim[4] - aim[1] * aim[3];
		final double ox = target[0] * rx + target[1] * ry + target[2] * rz;
		final double oy = target[0] * aim[3] + target[1] * aim[4] + target[2] * aim[5];
		final double of = target[0] * aim[0] + target[1] * aim[1] + target[2] * aim[2];
		final double sep = Math.toDegrees(Math.acos(Math.max(-1., Math.min(1., of))));

		final float cx = getWidth() / 2f;
		final float cy = getHeight() / 2f;
		final float span = Math.min(getWidth(), getHeight());
		final double want = Math.max(6.75, Math.min(22.5, fovDeg * 0.27));
		final float hold = Math.max(Theme.dp(30),
				Math.min((float) (want / fovDeg) * span, span / 2f - Theme.dp(90)));
		final double lockIn = hold / span * fovDeg;
		if (!locked && sep < lockIn)
		{
			locked = true;
			lockedAt = SystemClock.uptimeMillis();
			performHapticFeedback(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
					? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.LONG_PRESS);
		}
		else if (locked && sep > lockIn * 1.25)
		{
			locked = false;
		}

		ring.setColor(locked ? Theme.ACCENT : 0x66FFFFFF);
		ring.setStrokeWidth(Theme.dp(locked ? 2f : 1.5f));
		halo.setStrokeWidth(ring.getStrokeWidth() + Theme.dp(2));
		canvas.drawCircle(cx, cy + Theme.dp(1), hold, halo);
		canvas.drawCircle(cx, cy, hold, ring);

		if (locked)
		{
			final float t = (SystemClock.uptimeMillis() - lockedAt) / (float) PULSE_MS;
			if (t < 1f)
			{
				pulse.setAlpha((int) (200 * (1f - t)));
				canvas.drawCircle(cx, cy, hold + Theme.dp(34) * t, pulse);
				postInvalidateOnAnimation();
			}
		}
		else
		{
			final double flat = Math.hypot(ox, oy);
			final double angle = flat < 1e-9 ? lastAngle : Math.atan2(-oy, ox);
			lastAngle = angle;
			final float radius = hold + Theme.dp(28);
			final float wave = (float) Math.sin(SystemClock.uptimeMillis() / 260.) * Theme.dp(3);
			canvas.save();
			canvas.translate(cx + (float) Math.cos(angle) * (radius + wave),
			                 cy + (float) Math.sin(angle) * (radius + wave));
			canvas.rotate((float) Math.toDegrees(angle));
			halo.setStrokeWidth(arrow.getStrokeWidth() + Theme.dp(2.5f));
			canvas.save();
			canvas.translate(0, Theme.dp(1));
			canvas.drawPath(chevron, halo);
			canvas.restore();
			canvas.drawPath(chevron, arrow);
			if (sep > 45.)
			{
				canvas.translate(-Theme.dp(8), 0);
				arrow.setAlpha(130);
				canvas.drawPath(chevron, arrow);
				arrow.setAlpha(255);
			}
			canvas.restore();
		}
	}
}
