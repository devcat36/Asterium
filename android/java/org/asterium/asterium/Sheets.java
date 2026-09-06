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

final class Sheets
{
	private Sheets() {}

	static Sheet create(Context context, Overlay overlay, String id)
	{
		switch (id)
		{
			case "location":  return new LocationSheet(context, overlay);
			case "time":      return new TimeSheet(context, overlay);
			case "view":      return new ViewSheet(context, overlay);
			case "search":    return new SearchSheet(context, overlay);
			case "obslist":   return new ObsListSheet(context, overlay);
			case "config":    return new ConfigSheet(context, overlay);
			case "astrocalc": return new AstroCalcSheet(context, overlay);
			case "help":      return new HelpSheet(context, overlay);
			case "oculars":   return new OcularsSheet(context, overlay);
			case "ocularpanel": return new OcularPanelSheet(context, overlay);
			case "info":      return new InfoSheet(context, overlay);
			default:          return null;
		}
	}
}
