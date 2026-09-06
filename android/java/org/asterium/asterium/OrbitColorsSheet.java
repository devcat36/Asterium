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
import android.widget.LinearLayout;
import android.widget.TextView;

final class OrbitColorsSheet extends PropertySheet
{
	private static final String[][] PAGES = { { "Orbit colors", "" } };

	private static final String[] STYLE_IDS = {
		"one_color", "groups", "major_planets", "major_planets_minor_types",
	};
	private static final String[] STYLE_NAMES = {
		"One color for all orbits",
		"Separate colors for orbits by object type",
		"Separate colors for orbits of major planets",
		"Major planets by planet, minor bodies by type",
	};

	private static final String[][] GROUPS = {
		{ "Orbits of major planets",      "majorPlanetsOrbitsColor"         },
		{ "Orbits of minor planets",      "minorPlanetsOrbitsColor"         },
		{ "Orbits of dwarf planets",      "dwarfPlanetsOrbitsColor"         },
		{ "Orbits of moons",              "moonsOrbitsColor"                },
		{ "Orbits of cubewanos",          "cubewanosOrbitsColor"            },
		{ "Orbits of plutinos",           "plutinosOrbitsColor"             },
		{ "Orbits of scattered disk objects", "scatteredDiskObjectsOrbitsColor" },
		{ "Orbits of Oort cloud objects", "oortCloudObjectsOrbitsColor"     },
		{ "Orbits of comets",             "cometsOrbitsColor"               },
		{ "Orbits of sednoids",           "sednoidsOrbitsColor"             },
		{ "Orbits of interstellar objects", "interstellarOrbitsColor"       },
	};

	private static final String[][] MAJOR_PLANETS = {
		{ "Mercury", "mercuryOrbitColor" },
		{ "Venus",   "venusOrbitColor"   },
		{ "Earth",   "earthOrbitColor"   },
		{ "Mars",    "marsOrbitColor"    },
		{ "Jupiter", "jupiterOrbitColor" },
		{ "Saturn",  "saturnOrbitColor"  },
		{ "Uranus",  "uranusOrbitColor"  },
		{ "Neptune", "neptuneOrbitColor" },
	};

	OrbitColorsSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "orbitcolors", "Colors of orbit lines", PAGES);
	}

	@Override
	void buildCuratedPage(LinearLayout into, String pageName)
	{
		final Context context = getContext();

		final LinearLayout single = section(context, "One color");
		addColor(single, "Color of orbits", "SolarSystem.orbitsColor");

		final LinearLayout groups = section(context, "By object type");
		for (String[] entry : GROUPS)
			addColor(groups, entry[0], "SolarSystem." + entry[1]);

		final LinearLayout majors = section(context, "By planet");
		for (String[] entry : MAJOR_PLANETS)
			addColor(majors, entry[0], "SolarSystem." + entry[1]);

		addChoice(into, "Style", "SolarSystem.orbitColorStyle", STYLE_IDS, STYLE_NAMES,
		          id ->
		          {
			          final boolean byPlanet = id.startsWith("major_planets");
			          final boolean byType = id.equals("groups") || id.equals("major_planets_minor_types");
			          show(single, !byPlanet && !byType);
			          show(groups, byType);
			          show(majors, byPlanet);
		          });
		into.addView(single);
		into.addView(groups);
		into.addView(majors);
	}

	private LinearLayout section(Context context, String title)
	{
		final LinearLayout block = new LinearLayout(context);
		block.setOrientation(LinearLayout.VERTICAL);
		block.setVisibility(android.view.View.GONE);
		final TextView heading = Widgets.section(context, title);
		block.addView(heading);
		return block;
	}

	private static void show(LinearLayout block, boolean visible)
	{
		block.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
	}
}
