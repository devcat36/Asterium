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

final class AstroCalcPages
{
	private AstroCalcPages() {}

	static AstroCalcPage create(int index)
	{
		switch (index)
		{
			case 2:  return new RtsPage();
			case 3:  return new PhenomenaPage();
			case 4:  return new GraphsPage();
			case 5:  return new WutPage();
			case 6:  return new PcPage();
			case 7:  return new EclipsesPage();
			case 8:  return new AlmanacPage();
			default: throw new IllegalArgumentException("no page " + index);
		}
	}
}
