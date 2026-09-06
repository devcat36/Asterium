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
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ViewSheet extends PropertySheet
{
	private static final String[][] PAGES = {
		{ "Sky",         ""                   },
		{ "SSO",         ""                   },
		{ "DSO",         ""                   },
		{ "Markings",    ""                   },
		{ "Landscape",   ""                   },
		{ "Sky Culture", ""                   },
	};

	private static final String[][] QUICK_SELECTION = {
		{ "select all",        "all"      },
		{ "select standard",   "standard" },
		{ "select none",       "none"     },
		{ "select preference", "load"     },
		{ "store preference",  "store"    },
	};

	private int catalogMask;
	private int typeMask;
	private GridLayout catalogGrid;
	private GridLayout typeGrid;
	private boolean typeFiltersOn = true;

	private String landscapeId = "";
	private TextView landscapeValue, landscapeText;
	private View defaultRow;
	private android.widget.Switch defaultSwitch;

	private LinearLayout culture, labelStyles, asterisms, zodiac, zodiacStyle,
	                     lunarSystem, lunarSystemStyle;
	private View partitions;

	ViewSheet(Context context, Overlay overlay)
	{
		super(context, overlay, "view", "Sky and viewing options", PAGES);
	}

	@Override
	void buildCuratedPage(LinearLayout into, String pageName)
	{
		if ("SSO".equals(pageName))
		{
			buildSolarSystem(into);
			return;
		}
		if ("DSO".equals(pageName))
		{
			buildDeepSky(into);
			return;
		}
		if ("Markings".equals(pageName))
		{
			buildMarkings(into);
			return;
		}
		if ("Landscape".equals(pageName))
		{
			buildLandscape(into);
			return;
		}
		if ("Sky Culture".equals(pageName))
		{
			buildSkyCulture(into);
			return;
		}
		buildSky(into);
		buildStars(into);
		buildProjection(into);
	}

	private void buildSky(LinearLayout into)
	{
		final Context context = getContext();
		into.addView(Widgets.section(context, "Sky"));

		addSwitch(into, "Milky Way", "The galactic band",
		          "MilkyWay.flagMilkyWayDisplayed");
		addSlider(into, "Milky Way brightness", "MilkyWay.intensity", 0., 10., 2);
		addSlider(into, "Milky Way saturation", "MilkyWay.saturation", 0., 1., 2);

		addSwitch(into, "Zodiacal Light", "Sunlight scattered by interplanetary dust",
		          "ZodiacalLight.flagZodiacalLightDisplayed");
		addSlider(into, "Zodiacal Light brightness", "ZodiacalLight.intensity", 0., 10., 2);

		addSwitch(into, "Dynamic eye adaptation", "Adjust brightness to bright objects",
		          "StelSkyDrawer.flagLuminanceAdaptation");
		addSwitch(into, "Atmosphere visualization", "Refraction, extinction, sky brightness",
		          "LandscapeMgr.atmosphereDisplayed");

		addSwitch(into, "Light pollution from database", "Use the value stored for this location",
		          "LandscapeMgr.flagUseLightPollutionFromDatabase");
		addSlider(into, "Light pollution", "StelSkyDrawer.lightPollutionLuminance",
		          LIGHT_POLLUTION);

		addSlider(into, "Solar altitude for Twilight Finder", "SpecificTimeMgr.twilightAltitude",
		          linear(-20., 0., 2, "°"));
		addSlider(into, "Shooting stars", "SporadicMeteorMgr.zhr", SHOOTING_STARS);
	}

	private void buildSolarSystem(LinearLayout into)
	{
		final Context context = getContext();

		final LinearLayout body = new LinearLayout(context);
		body.setOrientation(LinearLayout.VERTICAL);
		addSwitch(into, "Solar System objects", "Draw the planets, moons and minor bodies",
		          "SolarSystem.planetsDisplayed",
		          on -> body.setVisibility(on ? View.VISIBLE : View.GONE));
		into.addView(body);

		body.addView(Widgets.section(context, "Show"));
		final LinearLayout labels = addGroup(body, "Labels and Markers",
				"Names beside the planets and their markers", "SolarSystem.labelsDisplayed");
		addSlider(labels, "Labels and Markers amount", "SolarSystem.labelsAmount", 0., 10., 1);
		addColor(labels, "Color of planet labels", "SolarSystem.labelsColor");

		addSwitch(body, "Planet markers", "Hint circles marking each planet",
		          "SolarSystem.flagHints");
		final LinearLayout markers = addGroup(body, "Mark minor bodies",
				"Show tiny circles for minor bodies regardless of magnitude",
				"SolarSystem.flagMarkers");
		addSlider(markers, "Brighter than", "SolarSystem.markerMagThreshold",
		          linear(-5., 37., 2, " mag"));

		addSwitch(body, "Moon's halo", "Toggle drawing halo around the Moon",
		          "SolarSystem.flagDrawMoonHalo");
		addSwitch(body, "Sun's glare", "Toggle drawing Sun's glare",
		          "SolarSystem.flagDrawSunHalo");
		addSwitch(body, "Sun's corona",
		          "Toggle permanent drawing of Sun's corona when atmosphere is disabled",
		          "SolarSystem.flagPermanentSolarCorona");

		body.addView(Widgets.section(context, "Orbits"));
		final LinearLayout orbits = addGroup(body, "Show orbits", null, "SolarSystem.flagOrbits");
		addSwitch(orbits, "permanently", "Show orbit even if object is off screen",
		          "SolarSystem.flagPermanentOrbits");
		addSwitch(orbits, "Major planets", "Show orbits of major planets",
		          "SolarSystem.flagPlanetsOrbits");
		addSwitch(orbits, "Only orbits of major planets…", "Hide orbits of minor bodies",
		          "SolarSystem.flagPlanetsOrbitsOnly");
		addSwitch(orbits, "Only orbit for selected object…", "Show orbit only for selected SSO",
		          "SolarSystem.flagIsolatedOrbits");
		addSwitch(orbits, "… and moons", "Also show moons of selected object or planets",
		          "SolarSystem.flagOrbitsWithMoons");
		addSlider(orbits, "Thickness", "SolarSystem.orbitsThickness", linear(1., 5., 0, " px"));
		orbits.addView(Widgets.navigationRow(context, "Configure colors of orbit lines", null,
				v -> overlay().open(new OrbitColorsSheet(context, overlay()))));

		body.addView(Widgets.section(context, "Trails"));
		final LinearLayout trails = addGroup(body, "Show trails",
				"The path each body has taken across the sky", "SolarSystem.trailsDisplayed");
		addSlider(trails, "Thickness", "SolarSystem.trailsThickness", linear(1., 5., 0, " px"));
		addSlider(trails, "Duration (years)", "SolarSystem.maxTrailTimeExtent", 1., 250., 0);
		addColor(trails, "Color of trails", "SolarSystem.trailsColor");
		final LinearLayout someTrails = addGroup(trails, "Only for N latest selected objects",
				"Switch this off to see the trails for all Solar system bodies",
				"SolarSystem.flagIsolatedTrails");
		addSlider(someTrails, "How many", "SolarSystem.numberIsolatedTrails", 1., 9., 0);

		body.addView(Widgets.section(context, "Planetary nomenclature"));
		final LinearLayout names = addGroup(body, "Show planetary nomenclature",
				"Show hints and labels for planetary features",
				"NomenclatureMgr.flagShowNomenclature");
		addSwitch(names, "Hide nomenclature on the celestial body of observer", null,
		          "NomenclatureMgr.flagHideLocalNomenclature");
		final LinearLayout terminator = addGroup(names, "Only for Solar elevation",
				"Label only features along the terminator",
				"NomenclatureMgr.flagShowTerminatorZoneOnly");
		addSlider(terminator, "Minimum Solar altitude", "NomenclatureMgr.terminatorMinAltitude",
		          linear(-90., 20., 0, "°"));
		addSlider(terminator, "Maximum Solar altitude", "NomenclatureMgr.terminatorMaxAltitude",
		          linear(0., 90., 0, "°"));
		addSwitch(names, "Outline craters", "Mark impact features with ellipses",
		          "NomenclatureMgr.flagOutlineCraters");
		addSwitch(names, "Special points only", "Show special nomenclature points only",
		          "NomenclatureMgr.specialNomenclatureOnlyDisplayed");
		addColor(names, "Color of nomenclature labels", "NomenclatureMgr.nomenclatureColor");

		body.addView(Widgets.section(context, "Scale"));
		body.addView(Widgets.note(context,
				"Drawn larger than life, so a body that is a few arcseconds wide is "
				+ "visible at all. The position stays true."));
		final LinearLayout moon = addGroup(body, "Moon", null, "SolarSystem.flagMoonScale");
		addSlider(moon, "Scale factor", "SolarSystem.moonScale", linear(1., 100., 2, "×"));
		final LinearLayout dynamic = addGroup(moon, "Dynamic (FOV-based) Moon size",
				"Scales smoothly between 1× at the minimum field of view and the factor above "
				+ "at the maximum", "SolarSystem.flagDynamicMoonScale");
		addSlider(dynamic, "1× below", "SolarSystem.moonScaleMinFov", linear(0.5, 89., 1, "°"));
		addSlider(dynamic, "N× above", "SolarSystem.moonScaleMaxFov", linear(1., 180., 1, "°"));

		final LinearLayout sun = addGroup(body, "Sun", null, "SolarSystem.flagSunScale");
		addSlider(sun, "Scale factor", "SolarSystem.sunScale", linear(1., 100., 2, "×"));
		final LinearLayout planets = addGroup(body, "Planets", null, "SolarSystem.flagPlanetScale");
		addSlider(planets, "Scale factor", "SolarSystem.planetScale", linear(1., 500., 2, "×"));
		final LinearLayout minor = addGroup(body, "Minor bodies", null,
				"SolarSystem.flagMinorBodyScale");
		addSlider(minor, "Scale factor", "SolarSystem.minorBodyScale", linear(1., 1000., 2, "×"));

		body.addView(Widgets.section(context, "Magnitude"));
		final LinearLayout limit = addGroup(body, "Limit magnitude",
				"Limit the magnitude of solar system objects",
				"StelSkyDrawer.flagPlanetMagnitudeLimit");
		addSlider(limit, "Faintest shown", "StelSkyDrawer.customPlanetMagLimit",
		          linear(-5., 25., 2, " mag"));

		final TextView algorithmNote = Widgets.note(context, "");
		addChoice(body, "Planets magnitude algorithm",
		          "SolarSystem.apparentMagnitudeAlgorithmOnEarth",
		          MAGNITUDE_IDS, MAGNITUDE_NAMES,
		          id ->
		          {
			          for (int i = 0; i < MAGNITUDE_IDS.length; ++i)
			          {
				          if (MAGNITUDE_IDS[i].equals(id))
					          algorithmNote.setText(T.t(MAGNITUDE_NOTES[i]));
			          }
		          });
		body.addView(algorithmNote);

		body.addView(Widgets.section(context, "Simulation"));
		addSwitch(body, "Simulate light speed",
		          "Show each body where it was when the light left it (recommended)",
		          "SolarSystem.flagLightTravelTime");
		final LinearLayout models = addGroup(body, "Use more accurate 3D models (where available)",
				"A polygonal model for some small moons, asteroids and comets instead of a sphere",
				"SolarSystem.flagUseObjModels");
		addSwitch(models, "Simulate self-shadowing",
		          "Shadow map for non-convex bodies. May coarsen the penumbra on some objects",
		          "SolarSystem.flagShowObjSelfShadows");
		addSwitch(body, "Earth shadow enlargement after Danjon",
		          "Astronomical Almanac (default) enlarges by 2%; Danjon (1951) differs slightly",
		          "SolarSystem.earthShadowEnlargementDanjon");

		body.addView(Widgets.section(context, "Great Red Spot"));
		body.addView(Widgets.navigationRow(context, "GRS details…",
				"Where Jupiter's Great Red Spot was, and when",
				v -> overlay().open(new GrsSheet(context, overlay()))));
	}

	private static final String[] MAGNITUDE_IDS = {
		"Mueller1893", "AstrAlm1984", "ExpSup1992", "ExpSup2013", "Mallama2018", "Generic",
	};
	private static final String[] MAGNITUDE_NAMES = {
		"G. Müller (1893)",
		"Astronomical Almanac (1984)",
		"Explanatory Supplement (1992)",
		"Explanatory Supplement (2013)",
		"Mallama & Hilton (2018)",
		"Generic",
	};
	private static final String[] MAGNITUDE_NOTES = {
		"The algorithm is based on visual observations 1877-1891 by G. Müller and was still "
			+ "republished in the Explanatory Supplement to the Astronomical Ephemeris (1961).",
		"The algorithm was used in the Astronomical Almanac (1984 and later) and gives V "
			+ "(instrumental) magnitudes (allegedly from D.L. Harris).",
		"The algorithm was published in the 2nd edition of the Explanatory Supplement to the "
			+ "Astronomical Almanac (1992).",
		"The algorithm was published in the 3rd edition of the Explanatory Supplement to the "
			+ "Astronomical Almanac (2013).",
		"The algorithm was published by A. Mallama & J. L. Hilton: Computing apparent planetary "
			+ "magnitudes for the Astronomical Almanac. Astronomy&Computing 25 (2018) 10-24.",
		"Visual magnitude based on phase angle and albedo.",
	};

	private void buildDeepSky(LinearLayout into)
	{
		final Context context = getContext();

		into.addView(Widgets.section(context, "Display objects from catalogs"));
		catalogGrid = Widgets.chipGrid(context, 3);
		into.addView(catalogGrid);
		addQuickSelection(into);

		into.addView(Widgets.gap(context, 14));
		typeGrid = Widgets.chipGrid(context, 2);
		addSwitch(into, "Filter by type", "Show only the kinds ticked below",
		          "NebulaMgr.flagTypeFiltersUsage", on ->
		          {
			          typeFiltersOn = on;
			          setGridEnabled(typeGrid, on);
		          });
		into.addView(typeGrid);

		into.addView(Widgets.gap(context, 14));
		addSwitch(into, "Labels and Markers", "Draw deep-sky objects at all",
		          "NebulaMgr.flagHintDisplayed");
		addSlider(into, "Labels", "NebulaMgr.labelsAmount", 0., 10., 1);
		addSlider(into, "Labels brightness", "NebulaMgr.labelsBrightness", 0., 1., 1);
		addSlider(into, "Hints", "NebulaMgr.hintsAmount", 0., 10., 1);
		addSlider(into, "Hints brightness", "NebulaMgr.hintsBrightness", 0., 1., 1);
		addSwitch(into, "Use designations for screen labels",
		          "Catalogue numbers instead of common names",
		          "NebulaMgr.flagDesignationLabels");
		addSwitch(into, "Use outlines for big deep-sky objects", null,
		          "NebulaMgr.flagOutlinesDisplayed");
		addSwitch(into, "Use proportional hints", "Markers sized by the object's angular size",
		          "NebulaMgr.hintsProportional");
		addSwitch(into, "Use surface brightness",
		          "Scale marker and label visibility by surface brightness",
		          "NebulaMgr.flagSurfaceBrightnessUsage");
		addSwitch(into, "Use additional names of DSO", null,
		          "NebulaMgr.flagAdditionalNamesDisplayed");
		addSwitch(into, "Show only named DSO", null, "NebulaMgr.flagShowOnlyNamedDSO");

		addSwitch(into, "Limit magnitude", "Hide deep-sky objects fainter than the limit",
		          "StelSkyDrawer.flagNebulaMagnitudeLimit");
		addSlider(into, "Limit magnitude", "StelSkyDrawer.customNebulaMagLimit", 3., 21., 2);
		addSwitch(into, "Limit angular size", "Show only objects between the two sizes",
		          "NebulaMgr.flagUseSizeLimits");
		addSlider(into, "Minimal angular size", "NebulaMgr.minSizeLimit",
		          linear(0.01, 600., 2, "′"));
		addSlider(into, "Maximum angular size", "NebulaMgr.maxSizeLimit",
		          linear(0.01, 1200., 2, "′"));

		into.addView(Widgets.navigationRow(context, "Configure colors of markers",
				"The colour each kind of object is drawn in",
				v -> overlay().open(new DsoColorsSheet(context, overlay()))));

		loadDeepSkyFilters();
	}

	private void addQuickSelection(LinearLayout into)
	{
		final Context context = getContext();
		LinearLayout row = quickRow(context);
		for (int i = 0; i < QUICK_SELECTION.length; ++i)
		{
			if (i == 3)
			{
				into.addView(row);
				row = quickRow(context);
			}
			final String preset = QUICK_SELECTION[i][1];
			final TextView button = Widgets.secondaryButton(context, QUICK_SELECTION[i][0], v ->
			{
				NativeBridge.send("dso.preset", preset);
				loadDeepSkyFilters();
			});
			button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			final LinearLayout.LayoutParams params =
					new LinearLayout.LayoutParams(0, Theme.dp(42), 1f);
			params.leftMargin = row.getChildCount() == 0 ? 0 : Theme.dp(8);
			row.addView(button, params);
		}
		into.addView(row);
	}

	private static LinearLayout quickRow(Context context)
	{
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(row, 16, 0, 16, 8);
		return row;
	}

	private void loadDeepSkyFilters()
	{
		NativeBridge.request("dso", "", payload ->
		{
			catalogMask = payload.optInt("catalogFilters");
			typeMask = payload.optInt("typeFilters");
			fillGrid(catalogGrid, payload.optJSONArray("catalogs"), true);
			fillGrid(typeGrid, payload.optJSONArray("types"), false);
		});
	}

	private void fillGrid(final GridLayout grid, JSONArray entries, final boolean catalogs)
	{
		if (grid == null || entries == null)
			return;
		grid.removeAllViews();
		for (int i = 0; i < entries.length(); ++i)
		{
			final JSONObject entry = entries.optJSONObject(i);
			if (entry == null)
				continue;
			final int bit = entry.optInt("bit");
			final TextView chip = Widgets.toggleChip(getContext(), entry.optString("label"),
					(mask(catalogs) & bit) != 0, null);
			chip.setOnClickListener(v ->
			{
				final int updated = mask(catalogs) ^ bit;
				if (catalogs)
					catalogMask = updated;
				else
					typeMask = updated;
				NativeBridge.send("dso.filter", (catalogs ? "catalogs=" : "types=") + updated);
				Widgets.setChipOn(chip, (updated & bit) != 0);
			});
			final String name = entry.optString("name");
			chip.setContentDescription(T.t(name.isEmpty() ? entry.optString("label") : name));
			chip.setTooltipText(name.isEmpty() ? null : name);
			grid.addView(chip, Widgets.chipCell());
		}
		if (!catalogs)
			setGridEnabled(grid, typeFiltersOn);
	}

	private int mask(boolean catalogs)
	{
		return catalogs ? catalogMask : typeMask;
	}

	private static void setGridEnabled(GridLayout grid, boolean enabled)
	{
		if (grid == null)
			return;
		grid.setAlpha(enabled ? 1f : 0.4f);
		for (int i = 0; i < grid.getChildCount(); ++i)
			grid.getChildAt(i).setEnabled(enabled);
	}

	private void buildStars(LinearLayout into)
	{
		final Context context = getContext();
		into.addView(Widgets.section(context, "Stars"));

		addSwitch(into, "Stars", null, "StarMgr.flagStarsDisplayed");
		addSlider(into, "Absolute scale", "StelSkyDrawer.absoluteStarScale", 0.05, 10., 2);
		addSlider(into, "Relative scale", "StelSkyDrawer.relativeStarScale", 0.25, 5., 2);

		addSwitch(into, "Twinkle", null, "StelSkyDrawer.flagStarTwinkle");
		addSlider(into, "Twinkle amount", "StelSkyDrawer.twinkleAmount", 0., 1.5, 2);
		addSwitch(into, "Twinkle even without atmosphere",
		          "Twinkle is caused by atmospheric turbulence; this shows it regardless",
		          "StelSkyDrawer.flagForcedTwinkle");

		addSwitch(into, "Limit magnitude", null, "StelSkyDrawer.flagStarMagnitudeLimit");
		addSlider(into, "Limit magnitude", "StelSkyDrawer.customStarMagLimit", 0., 21., 2);
		addSwitch(into, "Spiky stars", null, "StelSkyDrawer.flagStarSpiky");

		addSwitch(into, "Labels and Markers", null, "StarMgr.flagLabelsDisplayed");
		addSlider(into, "Labels and Markers amount", "StarMgr.labelsAmount", 0., 10., 1);
		addSwitch(into, "Show additional star names", null,
		          "StarMgr.flagAdditionalNamesDisplayed");
		addSwitch(into, "Use designations for screen labels", null,
		          "StarMgr.flagDesignationLabels");
		addSwitch(into, "Dbl. stars", "Designations for double stars",
		          "StarMgr.flagDblStarsDesignation");
		addSwitch(into, "Var. stars", "Designations for variable stars",
		          "StarMgr.flagVarStarsDesignation");
		addSwitch(into, "HIP", "Hipparcos designations",
		          "StarMgr.flagHIPDesignation");
	}

	private void buildProjection(LinearLayout into)
	{
		final Context context = getContext();
		into.addView(Widgets.section(context, "Projection"));

		final LinearLayout current = new LinearLayout(context);
		current.setOrientation(LinearLayout.VERTICAL);
		into.addView(current);
		fillProjection(current);

		addSlider(into, "Current FoV", "StelMovementMgr.currentFov", FIELD_OF_VIEW);
		addSlider(into, "Vertical viewport offset",
		          "StelMovementMgr.viewportVerticalOffsetTarget", linear(-50., 50., 0, "%"));
		addSlider(into, "Custom FoV limit", "StelMovementMgr.userMaxFov",
		          linear(1., 360., 1, "°"));
	}

	private void fillProjection(final LinearLayout into)
	{
		final Context context = getContext();
		NativeBridge.request("projections", "", payload ->
		{
			into.removeAllViews();
			final String key = payload.optString("current");
			final String name = payload.optString("name");
			into.addView(Widgets.navigationRow(context, "Projection", name,
					v -> pickProjection(into, key)));
			String description = payload.optString("description").trim();
			if (description.startsWith(name))
				description = description.substring(name.length()).trim();
			if (!description.isEmpty())
				into.addView(Widgets.note(context, description));
		});
	}

	private void pickProjection(final LinearLayout into, String current)
	{
		final PickerSheet picker = new PickerSheet(getContext(), overlay(), "Projection",
				current, id ->
				{
					NativeBridge.send("prop.set", "StelCore.currentProjectionTypeKey=" + id);
					fillProjection(into);
				});
		overlay().open(picker);
		NativeBridge.request("projections", "", payload ->
		{
			final JSONArray list = payload.optJSONArray("projections");
			final List<String> ids = new ArrayList<>(), labels = new ArrayList<>();
			for (int i = 0; list != null && i < list.length(); ++i)
			{
				final JSONObject item = list.optJSONObject(i);
				if (item == null)
					continue;
				ids.add(item.optString("id"));
				labels.add(item.optString("name"));
			}
			picker.setItems(ids, labels);
		});
	}

	private void buildLandscape(LinearLayout into)
	{
		final Context context = getContext();

		landscapeValue = Theme.text(context, "", 14, Theme.ACCENT, false);
		into.addView(Widgets.pickerRow(context, "Landscape", landscapeValue, v -> pickLandscape()));
		into.addView(Widgets.hairline(context));

		landscapeText = Theme.text(context, "", 12, Theme.TEXT_DIM, false);
		landscapeText.setLineSpacing(Theme.dp(3), 1f);
		Theme.padding(landscapeText, 16, 14, 16, 14);
		into.addView(landscapeText);

		into.addView(Widgets.section(context, "Options"));

		final JSONObject currentId = value("LandscapeMgr.currentLandscapeID");
		final JSONObject defaultId = value("LandscapeMgr.defaultLandscapeID");
		final boolean isDefault = currentId != null && defaultId != null
				&& currentId.optString("value").equals(defaultId.optString("value"));
		defaultRow = Widgets.switchRow(context, "Use this landscape as default", null, isDefault, null);
		defaultSwitch = Widgets.switchIn(defaultRow);
		into.addView(defaultRow);
		into.addView(Widgets.hairline(context));
		showDefault(isDefault);

		addSwitch(into, "Location from landscape",
		          "Stand where the landscape was shot, on the planet it names",
		          "LandscapeMgr.flagLandscapeSetsLocation");
		addSwitch(into, "Show ground", null, "LandscapeMgr.landscapeDisplayed");
		addSwitch(into, "Show illumination", "Lit windows, street lighting and the like",
		          "LandscapeMgr.illuminationDisplayed");
		addSwitch(into, "Show fog", null, "LandscapeMgr.fogDisplayed");

		final LinearLayout brightness = nested(context);
		addSwitch(into, "Minimal brightness", "Leaves the landscape visible in darkness",
		          "LandscapeMgr.flagLandscapeUseMinimalBrightness", on -> show(brightness, on));
		into.addView(brightness);
		addSlider(brightness, "Brightness", "LandscapeMgr.defaultMinimalBrightness", 0., 1., 2);
		addSwitch(brightness, "from landscape, if given",
		          "Use the value in landscape.ini when it has one",
		          "LandscapeMgr.flagLandscapeSetsMinimalBrightness");

		final LinearLayout manualTransparency = new LinearLayout(context);
		manualTransparency.setOrientation(LinearLayout.VERTICAL);
		final LinearLayout transparency = nested(context);
		addSwitch(manualTransparency, "Transparency", "See the sky through the ground",
		          "LandscapeMgr.flagLandscapeUseTransparency", on -> show(transparency, on));
		manualTransparency.addView(transparency);
		addSlider(transparency, "Amount", "LandscapeMgr.landscapeTransparency", 0., 1., 2);

		addSwitch(into, "Automatic transparency",
		          "Fades the ground as you look below the horizon or zoom in",
		          "LandscapeMgr.flagLandscapeAutoTransparency",
		          on -> show(manualTransparency, !on));
		into.addView(manualTransparency);

		final LinearLayout polygon = nested(context);
		addSwitch(into, "Draw only polygon", "The horizon outline instead of the panorama",
		          "LandscapeMgr.flagPolyLineDisplayedOnly", on -> show(polygon, on));
		into.addView(polygon);
		addSlider(polygon, "Thickness", "LandscapeMgr.polyLineThickness", 0., 5., 0);
		addColor(polygon, "Polygon colour", "LandscapeMgr.polyLineColor");

		final LinearLayout labels = nested(context);
		addSwitch(into, "Landscape labels", "Named features the landscape marks",
		          "LandscapeMgr.labelsDisplayed", on -> show(labels, on));
		into.addView(labels);
		addSlider(labels, "Font size", "LandscapeMgr.labelFontSize", 5., 48., 0);
		addSlider(labels, "Text angle", "LandscapeMgr.labelAngle", 0., 90., 0);
		addColor(labels, "Label colour", "LandscapeMgr.labelColor");

		refreshLandscape();
	}

	private static LinearLayout nested(Context context)
	{
		final LinearLayout group = new LinearLayout(context);
		group.setOrientation(LinearLayout.VERTICAL);
		group.setBackgroundColor(Theme.FILL_FAINT);
		group.setPadding(Theme.dp(12), Theme.dp(2), 0, Theme.dp(6));
		return group;
	}

	private static void show(View group, boolean on)
	{
		group.setVisibility(on ? View.VISIBLE : View.GONE);
	}

	private void refreshLandscape()
	{
		NativeBridge.request("landscapes", "", payload ->
		{
			landscapeId = payload.optString("current");
			landscapeValue.setText(T.t(payload.optString("name")));
			landscapeText.setText(Html.fromHtml(payload.optString("html"),
					Html.FROM_HTML_MODE_COMPACT, source -> null, null));
			showDefault(payload.optBoolean("isDefault"));
		});
	}

	private void showDefault(boolean isDefault)
	{
		defaultSwitch.setOnCheckedChangeListener(null);
		defaultSwitch.setChecked(isDefault);
		defaultSwitch.setEnabled(!isDefault);
		defaultRow.setClickable(!isDefault);
		defaultRow.setAlpha(isDefault ? 0.55f : 1f);
		if (isDefault)
			return;
		defaultRow.setOnClickListener(v -> defaultSwitch.toggle());
		defaultSwitch.setOnCheckedChangeListener((button, checked) ->
		{
			if (!checked)
				return;
			NativeBridge.send("prop.set", "LandscapeMgr.defaultLandscapeID=" + landscapeId);
			showDefault(true);
		});
	}

	private void pickLandscape()
	{
		final PickerSheet picker = new PickerSheet(getContext(), overlay(), "Landscape",
				landscapeId, id ->
				{
					NativeBridge.send("prop.set", "LandscapeMgr.currentLandscapeID=" + id);
					refreshLandscape();
				});
		overlay().open(picker);
		NativeBridge.request("landscapes", "", payload ->
		{
			final JSONArray list = payload.optJSONArray("landscapes");
			final List<String> ids = new ArrayList<>(), labels = new ArrayList<>();
			for (int i = 0; list != null && i < list.length(); ++i)
			{
				final JSONObject item = list.optJSONObject(i);
				if (item == null)
					continue;
				ids.add(item.optString("id"));
				labels.add(item.optString("name"));
			}
			picker.setItems(ids, labels);
		});
	}


	private void buildMarkings(LinearLayout into)
	{
		final Context context = getContext();

		addSwitch(into, "Celestial Sphere", "Master switch for the grids, lines and points below",
		          "GridLinesMgr.gridlinesDisplayed");

		into.addView(Widgets.section(context, "Grids"));
		marking(into, "Equatorial grid (J2000)",   "equatorJ2000Grid");
		marking(into, "Equatorial grid (of date)", "equatorGrid");
		marking(into, "Fixed Equatorial grid",     "fixedEquatorGrid");
		marking(into, "Ecliptic grid (J2000)",     "eclipticJ2000Grid");
		marking(into, "Ecliptic grid (of date)",   "eclipticGrid");
		marking(into, "Azimuthal grid",            "azimuthalGrid");
		marking(into, "Galactic grid",             "galacticGrid");
		marking(into, "Supergalactic grid",        "supergalacticGrid");

		into.addView(Widgets.section(context, "Lines and circles"));
		line(into, "Equator (J2000)",         "equatorJ2000",         "Line");
		line(into, "Equator (of date)",       "equator",              "Line");
		line(into, "Fixed Equator",           "fixedEquator",         "Line");
		line(into, "Ecliptic (J2000)",        "eclipticJ2000",        "Line");
		line(into, "Ecliptic (of date)",      "ecliptic",             "Line",
		     new String[] { "with Solar Dates", "GridLinesMgr.eclipticDatesLabeled" });
		line(into, "Horizon",                 "horizon",              "Line");
		line(into, "Meridian",                "meridian",             "Line");
		line(into, "Prime Vertical",          "primeVertical",        "Line");
		line(into, "Altitude",                "currentVertical",      "Line");
		line(into, "Galactic equator",        "galacticEquator",      "Line");
		line(into, "Supergalactic equator",   "supergalacticEquator", "Line");
		line(into, "O./C. longitude",         "longitude",            "Line");
		line(into, "Projected Solar Equator", "solarEquator",         "Line");
		line(into, "Colures",                 "colure",               "Lines");
		line(into, "Precession circles",      "precession",           "Circles");
		marking(into, "Circumpolar circles", "circumpolarCircles");
		marking(into, "Quadrature circle",   "quadratureLine");
		marking(into, "Invariable plane of the Solar system", "invariablePlaneLine");
		chips(marking(into, "Earth umbra", "umbraCircle"),
		      new String[] { "Centre point", "GridLinesMgr.umbraCenterPointDisplayed" });
		marking(into, "Earth penumbra", "penumbraCircle");

		into.addView(Widgets.section(context, "Poles and points"));
		marking(into, "Celestial poles (J2000)",   "celestialJ2000Poles");
		marking(into, "Celestial poles (of date)", "celestialPoles");
		marking(into, "Ecliptic poles (J2000)",    "eclipticJ2000Poles");
		marking(into, "Ecliptic poles (of date)",  "eclipticPoles");
		marking(into, "Galactic poles",            "galacticPoles");
		marking(into, "Supergalactic poles",       "supergalacticPoles");
		marking(into, "Zenith and Nadir",          "zenithNadir");
		marking(into, "Galactic center and anticenter", "galacticCenter");
		marking(into, "Equinoxes (J2000)",   "equinoxJ2000Points");
		marking(into, "Equinoxes (of date)", "equinoxPoints");
		marking(into, "Solstices (J2000)",   "solsticeJ2000Points");
		marking(into, "Solstices (of date)", "solsticePoints");
		marking(into, "Apex points",         "apexPoints");
		marking(into, "Antisolar point",     "antisolarPoint");

		into.addView(Widgets.section(context, "Cardinal points"));
		chips(marking(into, "Cardinal points", "LandscapeMgr.cardinalPointsDisplayed",
		              "LandscapeMgr.cardinalPointsColor"),
		      new String[] { "8",  "LandscapeMgr.ordinalPointsDisplayed",     "Intercardinal points" },
		      new String[] { "16", "LandscapeMgr.ordinal16WRPointsDisplayed", "Secondary intercardinal points" },
		      new String[] { "32", "LandscapeMgr.ordinal32WRPointsDisplayed", "Tertiary intercardinal points" });
		marking(into, "Compass marks", "SpecialMarkersMgr.compassMarksDisplayed",
		        "SpecialMarkersMgr.compassMarksColor");

		into.addView(Widgets.section(context, "Field of view"));
		marking(into, "Center of FOV", "SpecialMarkersMgr.fovCenterMarkerDisplayed",
		        "SpecialMarkersMgr.fovCenterMarkerColor");
		final LinearLayout circular = marking(into, "Circular FOV",
				"SpecialMarkersMgr.fovCircularMarkerDisplayed",
				"SpecialMarkersMgr.fovCircularMarkerColor");
		addSlider(circular, "Field of view", "SpecialMarkersMgr.fovCircularMarkerSize",
		          linear(0.1, 28., 2, "°"));
		final LinearLayout rectangular = marking(into, "Rectangular FOV",
				"SpecialMarkersMgr.fovRectangularMarkerDisplayed",
				"SpecialMarkersMgr.fovRectangularMarkerColor");
		addSlider(rectangular, "Width", "SpecialMarkersMgr.fovRectangularMarkerWidth",
		          linear(0.1, 180., 2, "°"));
		addSlider(rectangular, "Height", "SpecialMarkersMgr.fovRectangularMarkerHeight",
		          linear(0.1, 180., 2, "°"));
		addSlider(rectangular, "Rotation", "SpecialMarkersMgr.fovRectangularMarkerRotationAngle",
		          linear(-90., 90., 1, "°"));

		into.addView(Widgets.section(context, "Appearance"));
		addDensity(into);
		addSlider(into, "Line thickness", "GridLinesMgr.lineThickness", 1., 5., 0);
		addSlider(into, "Partitions thickness", "GridLinesMgr.partThickness", 1., 5., 0);
		addSlider(into, "Point size", "GridLinesMgr.pointSize", 5., 25., 0);
	}

	private LinearLayout marking(LinearLayout into, String label, String stem)
	{
		return marking(into, label, "GridLinesMgr." + stem + "Displayed",
		               "GridLinesMgr." + stem + "Color");
	}

	private LinearLayout marking(LinearLayout into, final String label, final String showProperty,
	                             String colourProperty)
	{
		final Context context = getContext();
		final JSONObject property = value(showProperty);
		final boolean on = property != null && property.optBoolean("value");

		final LinearLayout group = new LinearLayout(context);
		group.setOrientation(LinearLayout.VERTICAL);

		final LinearLayout extras = new LinearLayout(context);
		extras.setOrientation(LinearLayout.VERTICAL);
		extras.setVisibility(on ? View.VISIBLE : View.GONE);
		Theme.padding(extras, 30, 0, 0, 8);

		group.addView(Widgets.switchRow(context, colorDot(label, colourProperty), label, null, on,
				(button, checked) ->
				{
					NativeBridge.send("prop.set", showProperty + "=" + checked);
					extras.setVisibility(checked ? View.VISIBLE : View.GONE);
				}));
		group.addView(extras);

		into.addView(group);
		into.addView(Widgets.hairline(context));
		return extras;
	}

	private void line(LinearLayout into, String label, String stem, String kind, String[]... more)
	{
		final LinearLayout extras = marking(into, label,
				"GridLinesMgr." + stem + kind + "Displayed",
				"GridLinesMgr." + stem + kind + "Color");
		final String[][] items = new String[2 + more.length][];
		items[0] = new String[] { "Partitions", "GridLinesMgr." + stem + "PartsDisplayed" };
		items[1] = new String[] { "Labels",     "GridLinesMgr." + stem + "PartsLabeled" };
		System.arraycopy(more, 0, items, 2, more.length);
		chips(extras, items);
	}

	private void chips(LinearLayout extras, String[]... items)
	{
		final Context context = getContext();
		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(row, 16, 2, 16, 2);
		for (final String[] item : items)
		{
			final String propertyId = item[1];
			final JSONObject property = value(propertyId);
			final boolean[] on = { property != null && property.optBoolean("value") };
			final TextView chip = Widgets.filterChip(context, item[0], on[0], null);
			chip.setContentDescription(T.t(item.length > 2 ? item[2] : item[0]));
			chip.setOnClickListener(v ->
			{
				on[0] = !on[0];
				Widgets.chipStyle(chip, on[0]);
				NativeBridge.send("prop.set", propertyId + "=" + on[0]);
			});
			row.addView(chip);
		}
		final HorizontalScrollView scroller = new HorizontalScrollView(context);
		scroller.setHorizontalScrollBarEnabled(false);
		scroller.addView(row);
		extras.addView(scroller);
	}

	private View colorDot(final String label, final String propertyId)
	{
		final Context context = getContext();
		final JSONObject property = value(propertyId);
		final int[] current = { ColorPicker.parse(
				property == null ? "" : property.optString("value")) };
		final GradientDrawable fill = Theme.box(current[0], 4, Theme.PANEL_EDGE);

		final View view = Widgets.colorDot(context, fill, v ->
				ColorPicker.show(context, label, current[0], chosen ->
				{
					current[0] = chosen;
					fill.setColor(chosen);
					NativeBridge.send("prop.set", propertyId + "=" + ColorPicker.hex(chosen));
				}));
		view.setContentDescription(String.format(T.t("%s colour"), label));
		return view;
	}

	private void addDensity(LinearLayout into)
	{
		final Context context = getContext();
		final String[] names = { "Fine", "Normal", "Coarse" };
		final double[] values = { 2., 1., 0.5 };
		final TextView[] buttons = new TextView[names.length];

		final JSONObject property = value("GridLinesMgr.gridSpacingMultiplier");
		final double multiplier = property == null ? 1. : property.optDouble("value", 1.);
		final int chosen = multiplier >= 1.8 ? 0 : (multiplier <= 0.7 ? 2 : 1);

		final LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(row, 16, 8, 16, 10);
		row.addView(Theme.text(context, "Grid density", 14, Theme.TEXT_CHIP, false));

		final LinearLayout line = new LinearLayout(context);
		line.setOrientation(LinearLayout.HORIZONTAL);
		Theme.padding(line, 0, 8, 0, 0);
		for (int i = 0; i < names.length; ++i)
		{
			final int index = i;
			buttons[i] = Widgets.filterChip(context, names[i], i == chosen, v ->
			{
				NativeBridge.send("prop.set", "GridLinesMgr.gridSpacingMultiplier=" + values[index]);
				for (int j = 0; j < buttons.length; ++j)
					Widgets.chipStyle(buttons[j], j == index);
			});
			line.addView(buttons[i]);
		}
		row.addView(line);
		into.addView(row);
	}

	private static Scale linear(double min, double max, final int decimals, final String suffix)
	{
		return new Scale(min, max, decimals)
		{
			@Override String readout(double value) { return format(value, decimals) + suffix; }
		};
	}

	private static final String[] BORTLE = {
		"excellent dark sky", "truly dark", "rural", "rural/suburban", "suburban",
		"bright suburban", "suburban/urban", "city", "inner city",
	};

	private static final double DARKEST_MPSAS = 26.5, BRIGHTEST_MPSAS = 15.;

	private static final Scale LIGHT_POLLUTION = new Scale(0., 1., 9)
	{
		@Override
		double value(double position)
		{
			final double mpsas = DARKEST_MPSAS - (DARKEST_MPSAS - BRIGHTEST_MPSAS) * position;
			return 10.8e4 * Math.pow(10., -0.4 * mpsas);
		}

		@Override
		double position(double luminance)
		{
			if (luminance <= 0.)
				return 0.;
			final double mpsas = Math.log10(luminance / 10.8e4) / -0.4;
			return Math.max(0., Math.min(1.,
					(DARKEST_MPSAS - mpsas) / (DARKEST_MPSAS - BRIGHTEST_MPSAS)));
		}

		@Override
		String readout(double luminance)
		{
			final int bortle = bortleClass(luminance);
			return "Bortle " + bortle + " · " + BORTLE[bortle - 1];
		}
	};

	private static int bortleClass(double luminance)
	{
		final double nelm = 8.32 - 2.17147240951626
				* Math.log(1. + 88.5588612190873 * Math.sqrt(Math.max(0., luminance)));
		if (nelm < 4.0) return 9;
		if (nelm < 4.5) return 8;
		if (nelm < 5.0) return 7;
		if (nelm < 5.5) return 6;
		if (nelm < 6.0) return 5;
		if (nelm < 6.5) return 4;
		if (nelm < 7.0) return 3;
		if (nelm < 7.5) return 2;
		return 1;
	}

	private static final Scale SHOOTING_STARS = new Scale(0., 240000., 0)
	{
		@Override
		double value(double position) { return max * position * position * position; }

		@Override
		double position(double zhr) { return Math.cbrt(Math.max(0., zhr) / max); }

		@Override
		String readout(double value)
		{
			final int zhr = (int) Math.round(value);
			return zhr + describeRate(zhr);
		}
	};

	private static String describeRate(int zhr)
	{
		if (zhr == 0)                       return " · No shooting stars";
		if (zhr <= 10)                      return " · Normal rate";
		if (zhr >= 20 && zhr <= 30)         return " · Standard Orionids rate";
		if (zhr >= 90 && zhr <= 110)        return " · Standard Perseids rate";
		if (zhr >= 111 && zhr <= 132)       return " · Standard Geminids rate";
		if (zhr >= 180 && zhr <= 220)       return " · Exceptional Perseid rate";
		if (zhr >= 900 && zhr <= 1100)      return " · Meteor storm rate";
		if (zhr >= 5400 && zhr <= 6600)     return " · Exceptional Draconid rate";
		if (zhr >= 9000 && zhr <= 11000)    return " · Exceptional Leonid rate";
		if (zhr >= 130000 && zhr <= 160000) return " · Very high rate (1966 Leonids)";
		if (zhr >= 230000)                  return " · Highest rate ever (1833 Leonids)";
		return "";
	}

	private static final Scale FIELD_OF_VIEW = new Scale(0.001, 360., 3)
	{
		@Override
		double value(double position) { return min * Math.pow(max / min, position); }

		@Override
		double position(double fov)
		{
			if (fov <= min)
				return 0.;
			return Math.max(0., Math.min(1., Math.log(fov / min) / Math.log(max / min)));
		}

		@Override
		String readout(double fov) { return format(fov, fov < 1. ? 3 : 2) + "°"; }
	};

	private static final String[][] NAME_FORMS = {
		{ "native",          "Native"     },
		{ "transliteration", "Pronounce"  },
		{ "sci. translit.",  "Translit"   },
		{ "IPA",             "IPA"        },
		{ "translation",     "Translated" },
		{ "byname",          "Byname"     },
		{ "modern",          "Modern"     },
	};

	private static final String[][] SINGLE_NAME_FORMS = {
		{ "native",          "Native"     },
		{ "transliteration", "Pronounce"  },
		{ "sci. translit.",  "Translit"   },
		{ "translated",      "Translated" },
	};

	private void buildSkyCulture(LinearLayout into)
	{
		final Context context = getContext();

		culture = column(context);
		into.addView(culture);

		into.addView(Widgets.section(context, "Names"));
		labelStyles = column(context);
		into.addView(labelStyles);
		addSwitch(into, "Abbreviated names", "The shortest label the culture defines",
		          "StelSkyCultureMgr.flagUseAbbreviatedNames");

		into.addView(Widgets.section(context, "Constellations"));

		LinearLayout group = addGroup(into, "Constellation lines", null,
		                              "ConstellationMgr.linesDisplayed");
		addSlider(group, "Thickness", "ConstellationMgr.constellationLineThickness", 1., 5., 0);
		addSlider(group, "Fading duration", "ConstellationMgr.linesFadeDuration", 0.1, 10., 1);
		addColor(group, "Color of constellation lines", "ConstellationMgr.linesColor");

		group = addGroup(into, "Constellation labels", null, "ConstellationMgr.namesDisplayed");
		addSlider(group, "Constellations font size", "ConstellationMgr.fontSize", 8., 40., 0);
		addSlider(group, "Fading duration", "ConstellationMgr.namesFadeDuration", 0.1, 10., 1);
		addColor(group, "Color of constellation names", "ConstellationMgr.namesColor");

		group = addGroup(into, "Constellation art", null, "ConstellationMgr.artDisplayed");
		addSlider(group, "Brightness", "ConstellationMgr.artIntensity", 0., 1., 2);
		addSlider(group, "Fading duration", "ConstellationMgr.artFadeDuration", 0.1, 10., 1);

		group = addGroup(into, "Constellation boundaries", "Show boundaries of constellations",
		                 "ConstellationMgr.boundariesDisplayed");
		addSlider(group, "Thickness", "ConstellationMgr.boundariesThickness", 1., 5., 0);
		addSlider(group, "Fading duration", "ConstellationMgr.boundariesFadeDuration", 0.1, 10., 1);
		addColor(group, "Color of constellation boundaries", "ConstellationMgr.boundariesColor");

		addSwitch(into, "Select single constellation", "Click on star to show its constellation",
		          "ConstellationMgr.isolateSelected");
		addSwitch(into, "Isolated", "Only one at a time",
		          "ConstellationMgr.flagConstellationPick");

		asterisms = column(context);
		into.addView(asterisms);
		asterisms.addView(Widgets.section(context, "Asterisms"));

		group = addGroup(asterisms, "Asterism lines", null, "AsterismMgr.linesDisplayed");
		addSlider(group, "Thickness", "AsterismMgr.asterismLineThickness", 1., 5., 0);
		addSlider(group, "Fading duration", "AsterismMgr.linesFadeDuration", 0.1, 10., 1);
		addColor(group, "Color of asterism lines", "AsterismMgr.linesColor");

		group = addGroup(asterisms, "Asterism labels", null, "AsterismMgr.namesDisplayed");
		addSlider(group, "Asterisms font size", "AsterismMgr.fontSize", 8., 40., 0);
		addSlider(group, "Fading duration", "AsterismMgr.namesFadeDuration", 0.1, 10., 1);
		addColor(group, "Color of asterism names", "AsterismMgr.namesColor");

		group = addGroup(asterisms, "Ray helpers", null, "AsterismMgr.rayHelpersDisplayed");
		addSlider(group, "Thickness", "AsterismMgr.rayHelperThickness", 1., 5., 0);
		addSlider(group, "Fading duration", "AsterismMgr.rayHelpersFadeDuration", 0.1, 10., 1);
		addColor(group, "Color of ray helpers", "AsterismMgr.rayHelpersColor");

		partitions = Widgets.section(context, "Zodiac and lunar stations");
		into.addView(partitions);

		zodiac = column(context);
		into.addView(zodiac);
		group = addGroup(zodiac, "Zodiac", "Show zodiac (if defined)",
		                 "ConstellationMgr.zodiacDisplayed");
		zodiacStyle = column(context);
		group.addView(zodiacStyle);
		addSlider(group, "Thickness", "ConstellationMgr.zodiacThickness", 1., 5., 0);
		addSlider(group, "Fading duration", "ConstellationMgr.zodiacFadeDuration", 0.1, 10., 1);
		addColor(group, "Color of zodiac lines", "ConstellationMgr.zodiacColor");

		lunarSystem = column(context);
		into.addView(lunarSystem);
		group = addGroup(lunarSystem, "Lunar Stations", "Show lunar stations (if defined)",
		                 "ConstellationMgr.lunarSystemDisplayed");
		lunarSystemStyle = column(context);
		group.addView(lunarSystemStyle);
		addSlider(group, "Thickness", "ConstellationMgr.lunarSystemThickness", 1., 5., 0);
		addSlider(group, "Fading duration", "ConstellationMgr.lunarSystemFadeDuration", 0.1, 10., 1);
		addColor(group, "Color of Lunar station lines", "ConstellationMgr.lunarSystemColor");

		asterisms.setVisibility(View.GONE);
		partitions.setVisibility(View.GONE);
		zodiac.setVisibility(View.GONE);
		lunarSystem.setVisibility(View.GONE);

		NativeBridge.request("skyculture", "", this::fillSkyCulture);
	}

	private void fillSkyCulture(JSONObject payload)
	{
		final Context context = getContext();
		final String name = payload.optString("name");
		if (name.isEmpty())
			return;

		String origin = payload.optString("region");
		final String period = SkyCultureSheet.period(payload.optInt("begin"), payload.optInt("end"));
		if (period != null)
			origin = origin.isEmpty() ? period : origin + " · " + period;

		culture.removeAllViews();
		culture.addView(Widgets.section(context, "Sky culture"));
		culture.addView(Widgets.navigationRow(context, name, origin,
				v -> overlay().open(new SkyCultureSheet(context, overlay(), this::reloadPage))));
		culture.addView(Widgets.hairline(context));

		final boolean isDefault = payload.optBoolean("isDefault");
		final View asDefault = Widgets.switchRow(context, "Use this sky culture as default", null,
				isDefault, (button, checked) ->
				{
					if (!checked)
						return;
					NativeBridge.send("skyculture.default");
					reloadPage();
				});
		if (isDefault)
		{
			asDefault.setOnClickListener(null);
			asDefault.setClickable(false);
			asDefault.setAlpha(0.55f);
			final android.widget.Switch spent = Widgets.switchIn(asDefault);
			if (spent != null)
				spent.setEnabled(false);
		}
		culture.addView(asDefault);
		culture.addView(Widgets.hairline(context));
		addCultureDescription(payload, name);

		labelStyles.removeAllViews();
		labelStyles.addView(styleRow("Info label", "info", payload.optString("infoLabel"),
		                             NAME_FORMS, true));
		labelStyles.addView(styleRow("Screen label", "screen", payload.optString("screenLabel"),
		                             NAME_FORMS, true));

		final boolean hasZodiac = payload.optBoolean("hasZodiac");
		final boolean hasLunarSystem = payload.optBoolean("hasLunarSystem");
		asterisms.setVisibility(payload.optBoolean("hasAsterisms") ? View.VISIBLE : View.GONE);
		zodiac.setVisibility(hasZodiac ? View.VISIBLE : View.GONE);
		lunarSystem.setVisibility(hasLunarSystem ? View.VISIBLE : View.GONE);
		partitions.setVisibility(hasZodiac || hasLunarSystem ? View.VISIBLE : View.GONE);

		zodiacStyle.removeAllViews();
		zodiacStyle.addView(styleRow("Label", "zodiac", payload.optString("zodiacLabel"),
		                             SINGLE_NAME_FORMS, false));
		lunarSystemStyle.removeAllViews();
		lunarSystemStyle.addView(styleRow("Label", "lunar", payload.optString("lunarLabel"),
		                                  SINGLE_NAME_FORMS, false));
	}

	private void addCultureDescription(JSONObject payload, String name)
	{
		final Context context = getContext();
		String description = payload.optString("description").trim();
		if (description.startsWith(name))
			description = description.substring(name.length()).trim();
		if (description.isEmpty())
			return;

		culture.addView(Widgets.section(context, "About this sky culture"));
		final TextView text = Theme.text(context, description, 13, Theme.TEXT_DIM, false);
		text.setLineSpacing(Theme.dp(4), 1f);
		text.setMaxLines(8);
		text.setEllipsize(TextUtils.TruncateAt.END);
		Theme.padding(text, 16, 0, 16, 8);
		culture.addView(text);

		final TextView more = Theme.text(context, "Show more", 13, Theme.ACCENT_LINK, false);
		Theme.padding(more, 16, 4, 16, 12);
		more.setBackground(Theme.pressable(Theme.box(android.graphics.Color.TRANSPARENT, 0), 0));
		more.setOnClickListener(v ->
		{
			final boolean clipped = text.getMaxLines() != Integer.MAX_VALUE;
			text.setMaxLines(clipped ? Integer.MAX_VALUE : 8);
			more.setText(T.t(clipped ? "Show less" : "Show more"));
		});
		culture.addView(more);
		culture.addView(Widgets.hairline(context));
	}

	private View styleRow(String label, final String target, String current,
	                      final String[][] forms, final boolean multiple)
	{
		final Context context = getContext();
		final LinearLayout block = new LinearLayout(context);
		block.setOrientation(LinearLayout.VERTICAL);
		Theme.padding(block, 16, 8, 16, 6);
		block.addView(Theme.text(context, label, 13, Theme.TEXT_CHIP, false));

		final Set<String> chosen = new LinkedHashSet<>();
		for (String token : current.toLowerCase(Locale.US).split(","))
		{
			final String form = token.trim();
			if (!form.isEmpty() && !form.equals("none"))
				chosen.add(form);
		}

		final HorizontalScrollView rail = new HorizontalScrollView(context);
		rail.setHorizontalScrollBarEnabled(false);
		final LinearLayout chips = new LinearLayout(context);
		chips.setOrientation(LinearLayout.HORIZONTAL);
		chips.setPadding(0, Theme.dp(8), 0, 0);
		rail.addView(chips);
		block.addView(rail);
		fillStyleChips(chips, target, forms, chosen, multiple);
		return block;
	}

	private void fillStyleChips(final LinearLayout chips, final String target, final String[][] forms,
	                            final Set<String> chosen, final boolean multiple)
	{
		chips.removeAllViews();
		for (final String[] form : forms)
		{
			final String token = form[1].toLowerCase(Locale.US);
			chips.addView(Widgets.filterChip(chips.getContext(), form[0], chosen.contains(token), v ->
			{
				if (multiple)
				{
					if (!chosen.remove(token))
						chosen.add(token);
				}
				else
				{
					chosen.clear();
					chosen.add(token);
				}
				NativeBridge.send("skyculture.style", target + "="
						+ (chosen.isEmpty() ? "none" : TextUtils.join(",", chosen)));
				fillStyleChips(chips, target, forms, chosen, multiple);
			}));
		}
	}

	private static LinearLayout column(Context context)
	{
		final LinearLayout group = new LinearLayout(context);
		group.setOrientation(LinearLayout.VERTICAL);
		return group;
	}

}
