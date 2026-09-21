package org.asterium.asterium;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class SettingDefaults
{
	private static final Map<String, String[]> entries = new HashMap<>();
	private static Map<String, String> shipped;

	static
	{
		add("AsterismMgr.asterismLineThickness", "viewing/asterism_line_thickness", "1.0");
		add("AsterismMgr.fontSize", "viewing/asterism_font_size", "14.0");
		add("AsterismMgr.linesColor", "color/asterism_lines_color", "0.5,0.5,0.7");
		add("AsterismMgr.linesFadeDuration", "viewing/asterism_lines_fade_duration", "1.0");
		add("AsterismMgr.namesColor", "color/asterism_names_color", "0.5,0.5,0.7");
		add("AsterismMgr.namesFadeDuration", "viewing/asterism_labels_fade_duration", "1.0");
		add("AsterismMgr.rayHelperThickness", "viewing/rayhelper_line_thickness", "1.0");
		add("AsterismMgr.rayHelpersColor", "color/rayhelper_lines_color", "0.5,0.5,0.7");
		add("AsterismMgr.rayHelpersFadeDuration", "viewing/rayhelper_lines_fade_duration", "1.0");
		add("ConstellationMgr.artFadeDuration", "viewing/constellation_art_fade_duration", "1.5");
		add("ConstellationMgr.artIntensity", "viewing/constellation_art_intensity", "0.45");
		add("ConstellationMgr.boundariesColor", "color/const_boundary_color", "0.3,0.1,0.1");
		add("ConstellationMgr.boundariesFadeDuration", "viewing/constellation_boundaries_fade_duration", "1.0");
		add("ConstellationMgr.boundariesThickness", "viewing/constellation_boundaries_thickness", "1.0");
		add("ConstellationMgr.constellationLineThickness", "viewing/constellation_line_thickness", "2.0");
		add("ConstellationMgr.fontSize", "viewing/constellation_font_size", "15.0");
		add("ConstellationMgr.hullsColor", "color/const_hull_color", "0.6,0.2,0.2");
		add("ConstellationMgr.linesColor", "color/const_lines_color", "0.4,0.6,0.9");
		add("ConstellationMgr.linesFadeDuration", "viewing/constellation_lines_fade_duration", "1.0");
		add("ConstellationMgr.lunarSystemColor", "color/skyculture_lunarsystem_color", "0.0,1.0,0.5");
		add("ConstellationMgr.lunarSystemFadeDuration", "viewing/skyculture_lunarsystem_fade_duration", "1.0");
		add("ConstellationMgr.lunarSystemThickness", "viewing/skyculture_lunarsystem_thickness", "1");
		add("ConstellationMgr.namesColor", "color/const_names_color", "0.4,0.6,0.9");
		add("ConstellationMgr.namesFadeDuration", "viewing/constellation_labels_fade_duration", "1.0");
		add("ConstellationMgr.zodiacColor", "color/skyculture_zodiac_color", "1.0,1.0,0.0");
		add("ConstellationMgr.zodiacFadeDuration", "viewing/skyculture_zodiac_fade_duration", "1.0");
		add("ConstellationMgr.zodiacThickness", "viewing/skyculture_zodiac_thickness", "1.0");
		add("GridLinesMgr.antisolarPointColor", "color/antisolar_point_color", "0.9,0.3,0.5");
		add("GridLinesMgr.apexPointsColor", "color/apex_points_color", "0.8,0.2,0.3");
		add("GridLinesMgr.azimuthalGridColor", "color/azimuthal_color", "0.0,0.3,0.2");
		add("GridLinesMgr.celestialJ2000PolesColor", "color/celestial_J2000_poles_color", "0.2,0.2,0.6");
		add("GridLinesMgr.celestialPolesColor", "color/celestial_poles_color", "0.3,0.5,1.0");
		add("GridLinesMgr.circumpolarCirclesColor", "color/circumpolar_circles_color", "0.3,0.5,1.0");
		add("GridLinesMgr.colureLinesColor", "color/colures_color", "0.5,0.0,0.5");
		add("GridLinesMgr.currentVerticalLineColor", "color/current_vertical_color", "0.5,0.5,0.7");
		add("GridLinesMgr.eclipticGridColor", "color/ecliptical_color", "0.6,0.3,0.1");
		add("GridLinesMgr.eclipticJ2000GridColor", "color/ecliptical_J2000_color", "0.4,0.1,0.1");
		add("GridLinesMgr.eclipticJ2000LineColor", "color/ecliptic_J2000_color", "0.7,0.2,0.2");
		add("GridLinesMgr.eclipticJ2000PolesColor", "color/ecliptic_J2000_poles_color", "0.7,0.2,0.2");
		add("GridLinesMgr.eclipticLineColor", "color/ecliptic_color", "0.9,0.6,0.2");
		add("GridLinesMgr.eclipticPolesColor", "color/ecliptic_poles_color", "0.9,0.6,0.2");
		add("GridLinesMgr.equatorGridColor", "color/equatorial_color", "0.2,0.3,0.8");
		add("GridLinesMgr.equatorJ2000GridColor", "color/equatorial_J2000_color", "0.1,0.1,0.5");
		add("GridLinesMgr.equatorJ2000LineColor", "color/equator_J2000_color", "0.2,0.2,0.6");
		add("GridLinesMgr.equatorLineColor", "color/equator_color", "0.3,0.5,1.0");
		add("GridLinesMgr.equinoxJ2000PointsColor", "color/equinox_J2000_points_color", "0.7,0.2,0.2");
		add("GridLinesMgr.equinoxPointsColor", "color/equinox_points_color", "0.9,0.6,0.2");
		add("GridLinesMgr.fixedEquatorGridColor", "color/fixed_equatorial_color", "0.5,0.5,0.7");
		add("GridLinesMgr.fixedEquatorLineColor", "color/fixed_equator_color", "0.5,0.5,0.7");
		add("GridLinesMgr.galacticCenterColor", "color/galactic_center_color", "0.5,0.5,0.7");
		add("GridLinesMgr.galacticEquatorLineColor", "color/galactic_equator_color", "0.5,0.3,0.1");
		add("GridLinesMgr.galacticGridColor", "color/galactic_color", "0.3,0.2,0.1");
		add("GridLinesMgr.galacticPolesColor", "color/galactic_poles_color", "0.5,0.3,0.1");
		add("GridLinesMgr.horizonLineColor", "color/horizon_color", "0.2,0.6,0.2");
		add("GridLinesMgr.invariablePlaneLineColor", "color/invariable_plane_color", "0.5,0.5,0.7");
		add("GridLinesMgr.lineThickness", "viewing/line_thickness", "1.0");
		add("GridLinesMgr.longitudeLineColor", "color/oc_longitude_color", "0.6,0.2,0.4");
		add("GridLinesMgr.meridianLineColor", "color/meridian_color", "0.2,0.6,0.2");
		add("GridLinesMgr.partThickness", "viewing/part_thickness", "1.0");
		add("GridLinesMgr.penumbraCircleColor", "color/penumbra_circle_color", "0.5,0.5,0.7");
		add("GridLinesMgr.pointSize", "viewing/point_size", "5.0");
		add("GridLinesMgr.precessionCirclesColor", "color/precession_circles_color", "0.9,0.6,0.2");
		add("GridLinesMgr.primeVerticalLineColor", "color/prime_vertical_color", "0.2,0.5,0.2");
		add("GridLinesMgr.quadratureLineColor", "color/quadrature_color", "0.5,0.5,0.7");
		add("GridLinesMgr.solarEquatorLineColor", "color/solar_equator_color", "0.5,0.5,0.7");
		add("GridLinesMgr.solsticeJ2000PointsColor", "color/solstice_J2000_points_color", "0.7,0.2,0.2");
		add("GridLinesMgr.solsticePointsColor", "color/solstice_points_color", "0.9,0.6,0.2");
		add("GridLinesMgr.supergalacticEquatorLineColor", "color/supergalactic_equator_color", "0.4,0.4,0.4");
		add("GridLinesMgr.supergalacticGridColor", "color/supergalactic_color", "0.2,0.2,0.2");
		add("GridLinesMgr.supergalacticPolesColor", "color/supergalactic_poles_color", "0.4,0.4,0.4");
		add("GridLinesMgr.umbraCircleColor", "color/umbra_circle_color", "0.5,0.5,0.7");
		add("GridLinesMgr.zenithNadirColor", "color/zenith_nadir_color", "0.2,0.6,0.2");
		add("LandscapeMgr.cardinalPointsColor", "color/cardinal_color", "0.8,0.2,0.1");
		add("LandscapeMgr.defaultMinimalBrightness", "landscape/minimal_brightness", "0.10");
		add("LandscapeMgr.labelAngle", "landscape/label_angle", "45.0");
		add("LandscapeMgr.labelColor", "landscape/label_color", "0.2,0.8,0.2");
		add("LandscapeMgr.labelFontSize", "landscape/label_font_size", "18");
		add("LandscapeMgr.landscapeTransparency", "landscape/transparency", "0.5");
		add("LandscapeMgr.polyLineColor", "landscape/polyline_color", "1.0,0.0,0.0");
		add("LandscapeMgr.polyLineThickness", "landscape/polyline_thickness", "1.0");
		add("MilkyWay.intensity", "astro/milky_way_intensity", "2.00");
		add("MilkyWay.saturation", "astro/milky_way_saturation", "1");
		add("NebulaMgr.activeGalaxiesColor", "color/dso_active_galaxy_color", "1.0,0.5,0.2");
		add("NebulaMgr.bipolarNebulaeColor", "color/dso_bipolar_nebula_color", "0.1,1.0,0.1");
		add("NebulaMgr.blLacObjectsColor", "color/dso_bl_lac_color", "1.0,0.2,0.2");
		add("NebulaMgr.blazarsColor", "color/dso_blazar_color", "1.0,0.2,0.2");
		add("NebulaMgr.circlesColor", "color/dso_circle_color", "1.0,0.7,0.2");
		add("NebulaMgr.clustersColor", "color/dso_cluster_color", "1.0,1.0,0.1");
		add("NebulaMgr.darkNebulaeColor", "color/dso_dark_nebula_color", "0.3,0.3,0.3");
		add("NebulaMgr.emissionLineStarsColor", "color/dso_emission_star_color", "1.0,0.7,0.2");
		add("NebulaMgr.emissionNebulaeColor", "color/dso_emission_nebula_color", "0.1,1.0,0.1");
		add("NebulaMgr.emissionObjectsColor", "color/dso_emission_object_color", "1.0,0.7,0.2");
		add("NebulaMgr.galaxiesColor", "color/dso_galaxy_color", "1.0,0.2,0.2");
		add("NebulaMgr.galaxyClustersColor", "color/dso_galaxy_cluster_color", "0.2,0.8,1.0");
		add("NebulaMgr.globularClustersColor", "color/dso_globular_cluster_color", "1.0,1.0,0.1");
		add("NebulaMgr.hintsAmount", "astro/nebula_hints_amount", "3.0");
		add("NebulaMgr.hintsBrightness", "astro/nebula_hints_brightness", "1.0");
		add("NebulaMgr.hydrogenRegionsColor", "color/dso_hydrogen_region_color", "0.1,1.0,0.1");
		add("NebulaMgr.interactingGalaxiesColor", "color/dso_interacting_galaxy_color", "0.2,0.5,1.0");
		add("NebulaMgr.interstellarMatterColor", "color/dso_interstellar_matter_color", "0.1,1.0,0.1");
		add("NebulaMgr.labelsAmount", "astro/nebula_labels_amount", "3.0");
		add("NebulaMgr.labelsBrightness", "astro/nebula_labels_brightness", "1.0");
		add("NebulaMgr.labelsColor", "color/dso_label_color", "0.2,0.6,0.7");
		add("NebulaMgr.maxSizeLimit", "astro/size_limit_max", "600.0");
		add("NebulaMgr.minSizeLimit", "astro/size_limit_min", "1.0");
		add("NebulaMgr.molecularCloudsColor", "color/dso_molecular_cloud_color", "0.1,1.0,0.1");
		add("NebulaMgr.nebulaeColor", "color/dso_nebula_color", "0.1,1.0,0.1");
		add("NebulaMgr.openClustersColor", "color/dso_open_cluster_color", "1.0,1.0,0.1");
		add("NebulaMgr.planetaryNebulaeColor", "color/dso_planetary_nebula_color", "0.1,1.0,0.1");
		add("NebulaMgr.possiblePlanetaryNebulaeColor", "color/dso_possible_planetary_nebula_color", "0.1,1.0,0.1");
		add("NebulaMgr.possibleQuasarsColor", "color/dso_possible_quasar_color", "1.0,0.2,0.2");
		add("NebulaMgr.protoplanetaryNebulaeColor", "color/dso_protoplanetary_nebula_color", "0.1,1.0,0.1");
		add("NebulaMgr.quasarsColor", "color/dso_quasar_color", "1.0,0.2,0.2");
		add("NebulaMgr.radioGalaxiesColor", "color/dso_radio_galaxy_color", "0.3,0.3,0.3");
		add("NebulaMgr.reflectionNebulaeColor", "color/dso_reflection_nebula_color", "0.1,1.0,0.1");
		add("NebulaMgr.regionsColor", "color/dso_region_color", "0.7,0.7,0.2");
		add("NebulaMgr.starCloudsColor", "color/dso_star_cloud_color", "1.0,1.0,0.1");
		add("NebulaMgr.starsColor", "color/dso_star_color", "1.0,0.7,0.2");
		add("NebulaMgr.stellarAssociationsColor", "color/dso_stellar_association_color", "1.0,1.0,0.1");
		add("NebulaMgr.supernovaCandidatesColor", "color/dso_supernova_candidate_color", "0.1,1.0,0.1");
		add("NebulaMgr.supernovaRemnantCandidatesColor", "color/dso_supernova_remnant_cand_color", "0.1,1.0,0.1");
		add("NebulaMgr.supernovaRemnantsColor", "color/dso_supernova_remnant_color", "0.1,1.0,0.1");
		add("NebulaMgr.symbioticStarsColor", "color/dso_symbiotic_star_color", "1.0,0.7,0.2");
		add("NebulaMgr.youngStellarObjectsColor", "color/dso_young_stellar_object_color", "1.0,0.7,0.2");
		add("NomenclatureMgr.nomenclatureColor", "color/planet_nomenclature_color", "0.1,1.0,0.1");
		add("NomenclatureMgr.terminatorMaxAltitude", "astro/planet_nomenclature_solar_altitude_max", "40.0");
		add("NomenclatureMgr.terminatorMinAltitude", "astro/planet_nomenclature_solar_altitude_min", "-5.0");
		add("Oculars.ccdCropOverlayHSize", "Oculars/ccd_crop_overlay_hsize", "250");
		add("Oculars.ccdCropOverlayVSize", "Oculars/ccd_crop_overlay_vsize", "250");
		add("Oculars.focuserColor", "Oculars/focuser_color", "0.0,0.67,1.0");
		add("Oculars.lineColor", "Oculars/line_color", "0.77,0.14,0.16");
		add("Oculars.reticleColor", "Oculars/reticle_color", "1.0,0.0,0.0");
		add("Oculars.textColor", "Oculars/text_color", "0.8,0.48,0.0");
		add("Oculars.transparencyMask", "Oculars/transparency_mask", "85.0");
		add("SolarSystem.cometsOrbitsColor", "color/comet_orbits_color", "0.7,0.8,0.8");
		add("SolarSystem.cubewanosOrbitsColor", "color/cubewano_orbits_color", "0.7,0.5,0.5");
		add("SolarSystem.dwarfPlanetsOrbitsColor", "color/dwarf_planet_orbits_color", "0.7,0.5,0.5");
		add("SolarSystem.earthOrbitColor", "color/earth_orbit_color", "0.0,0.0,1.0");
		add("SolarSystem.ephemerisGenericMarkerColor", "color/ephemeris_generic_marker_color", "1.0,1.0,0.0");
		add("SolarSystem.ephemerisJupiterMarkerColor", "color/ephemeris_jupiter_marker_color", "0.3,1.0,1.0");
		add("SolarSystem.ephemerisMarsMarkerColor", "color/ephemeris_mars_marker_color", "1.0,0.0,0.0");
		add("SolarSystem.ephemerisMercuryMarkerColor", "color/ephemeris_mercury_marker_color", "1.0,1.0,0.0");
		add("SolarSystem.ephemerisNeptuneMarkerColor", "color/ephemeris_neptune_marker_color", "0.2,0.3,0.5");
		add("SolarSystem.ephemerisSaturnMarkerColor", "color/ephemeris_saturn_marker_color", "0.0,1.0,0.0");
		add("SolarSystem.ephemerisSecondaryMarkerColor", "color/ephemeris_secondary_marker_color", "0.7,0.7,1.0");
		add("SolarSystem.ephemerisSelectedMarkerColor", "color/ephemeris_selected_marker_color", "1.0,0.7,0.0");
		add("SolarSystem.ephemerisUranusMarkerColor", "color/ephemeris_uranus_marker_color", "0.2,0.5,0.3");
		add("SolarSystem.ephemerisVenusMarkerColor", "color/ephemeris_venus_marker_color", "1.0,1.0,1.0");
		add("SolarSystem.interstellarOrbitsColor", "color/interstellar_orbits_color", "1.0,0.6,1.0");
		add("SolarSystem.jupiterOrbitColor", "color/jupiter_orbit_color", "1.0,0.6,0.0");
		add("SolarSystem.labelsAmount", "astro/labels_amount", "3.0");
		add("SolarSystem.labelsColor", "color/planet_names_color", "0.5,0.5,0.7");
		add("SolarSystem.majorPlanetsOrbitsColor", "color/major_planet_orbits_color", "0.7,0.2,0.2");
		add("SolarSystem.markerMagThreshold", "astro/planet_markers_mag_threshold", "15.0");
		add("SolarSystem.marsOrbitColor", "color/mars_orbit_color", "0.8,0.4,0.1");
		add("SolarSystem.maxTrailTimeExtent", "viewing/max_trail_time_extent", "1.0");
		add("SolarSystem.mercuryOrbitColor", "color/mercury_orbit_color", "0.5,0.5,0.5");
		add("SolarSystem.minorBodyScale", "viewing/minorbodies_scale", "10");
		add("SolarSystem.minorPlanetsOrbitsColor", "color/minor_planet_orbits_color", "0.7,0.5,0.5");
		add("SolarSystem.moonScale", "viewing/moon_scale", "4");
		add("SolarSystem.moonScaleMaxFov", "viewing/moon_scale_max_fov", "90.0");
		add("SolarSystem.moonScaleMinFov", "viewing/moon_scale_min_fov", "10.0");
		add("SolarSystem.moonsOrbitsColor", "color/moon_orbits_color", "0.7,0.2,0.2");
		add("SolarSystem.neptuneOrbitColor", "color/neptune_orbit_color", "0.0,0.3,1.0");
		add("SolarSystem.numberIsolatedTrails", "viewing/number_isolated_trails", "1.0");
		add("SolarSystem.oortCloudObjectsOrbitsColor", "color/oco_orbits_color", "0.7,0.5,0.5");
		add("SolarSystem.orbitsColor", "color/sso_orbits_color", "0.7,0.2,0.2");
		add("SolarSystem.orbitsThickness", "astro/object_orbits_thickness", "1.0");
		add("SolarSystem.planetScale", "viewing/planets_scale", "150.0");
		add("SolarSystem.plutinosOrbitsColor", "color/plutino_orbits_color", "0.7,0.5,0.5");
		add("SolarSystem.pointerColor", "color/planet_pointers_color", "1.0,0.3,0.3");
		add("SolarSystem.saturnOrbitColor", "color/saturn_orbit_color", "1.0,0.8,0.0");
		add("SolarSystem.scatteredDiskObjectsOrbitsColor", "color/sdo_orbits_color", "0.7,0.5,0.5");
		add("SolarSystem.sednoidsOrbitsColor", "color/sednoid_orbits_color", "0.7,0.5,0.5");
		add("SolarSystem.sunScale", "viewing/sun_scale", "4.0");
		add("SolarSystem.trailsColor", "color/object_trails_color", "1.0,0.7,0.0");
		add("SolarSystem.trailsThickness", "astro/object_trails_thickness", "1.0");
		add("SolarSystem.uranusOrbitColor", "color/uranus_orbit_color", "0.0,0.7,1.0");
		add("SolarSystem.venusOrbitColor", "color/venus_orbit_color", "0.9,0.9,0.7");
		add("SpecialMarkersMgr.compassMarksColor", "color/compass_marks_color", "0.5,0.5,0.7");
		add("SpecialMarkersMgr.fovCenterMarkerColor", "color/fov_center_marker_color", "0.5,0.5,0.7");
		add("SpecialMarkersMgr.fovCircularMarkerColor", "color/fov_circular_marker_color", "0.5,0.5,0.7");
		add("SpecialMarkersMgr.fovCircularMarkerSize", "viewing/size_fov_circular_marker", "1.0");
		add("SpecialMarkersMgr.fovRectangularMarkerColor", "color/fov_rectangular_marker_color", "0.5,0.5,0.7");
		add("SpecialMarkersMgr.fovRectangularMarkerHeight", "viewing/height_fov_rectangular_marker", "3.0");
		add("SpecialMarkersMgr.fovRectangularMarkerRotationAngle", "viewing/rot_fov_rectangular_marker", "0.0");
		add("SpecialMarkersMgr.fovRectangularMarkerWidth", "viewing/width_fov_rectangular_marker", "4.0");
		add("SpecificTimeMgr.twilightAltitude", "astro/twilight_altitude", "-6.0");
		add("SporadicMeteorMgr.zhr", "astro/meteor_zhr", "10");
		add("StarMgr.labelsAmount", "stars/labels_amount", "3.0");
		add("StelMovementMgr.currentFov", "navigation/init_fov", "40");
		add("StelMovementMgr.userMaxFov", "navigation/max_fov", "180");
		add("StelMovementMgr.viewportVerticalOffsetTarget", "projection/viewport_center_offset_y", "0");
		add("StelSkyDrawer.absoluteStarScale", "stars/absolute_scale", "1.0");
		add("StelSkyDrawer.customNebulaMagLimit", "astro/nebula_magnitude_limit", "8.5");
		add("StelSkyDrawer.customPlanetMagLimit", "astro/planet_magnitude_limit", "6.5");
		add("StelSkyDrawer.customStarMagLimit", "astro/star_magnitude_limit", "6.5");
		add("StelSkyDrawer.lightPollutionLuminance", "stars/init_light_pollution_luminance", "0.000051708816");
		add("StelSkyDrawer.relativeStarScale", "stars/relative_scale", "1.0");
		add("StelSkyDrawer.twinkleAmount", "stars/star_twinkle_amount", "0.2");
		add("ZodiacalLight.intensity", "astro/zodiacal_light_intensity", "1.0");
		add("astro/custom_moon_altitude", "astro/custom_moon_altitude", "18");
		add("astro/custom_sun_altitude", "astro/custom_sun_altitude", "-7");
		add("astrocalc/altvstime_positive_limit", "astrocalc/altvstime_positive_limit", "0");
		add("astrocalc/celestial_magnitude_limit", "astrocalc/celestial_magnitude_limit", "6");
		add("astrocalc/ephemeris_sun_altitude", "astrocalc/ephemeris_sun_altitude", "-10");
		add("astrocalc/hec_magnitude_limit", "astrocalc/hec_magnitude_limit", "9");
		add("astrocalc/me_positive_limit", "astrocalc/me_positive_limit", "0");
		add("astrocalc/me_time", "astrocalc/me_time", "0");
		add("astrocalc/phenomena_angular_separation", "astrocalc/phenomena_angular_separation", "1");
		add("astrocalc/wut_altitude_min", "astrocalc/wut_altitude_min", "0");
		add("astrocalc/wut_angular_limit_max", "astrocalc/wut_angular_limit_max", "600");
		add("astrocalc/wut_angular_limit_min", "astrocalc/wut_angular_limit_min", "10");
		add("astrocalc/wut_magnitude_limit", "astrocalc/wut_magnitude_limit", "10");
	}

	private SettingDefaults() {}

	private static void add(String id, String key, String fallback)
	{
		entries.put(id, new String[] { key, fallback });
	}

	static Double number(Context context, String id)
	{
		final String value = value(context, id);
		return value == null ? null : Double.valueOf(value);
	}

	static double opacity(Context context, String key)
	{
		return Double.parseDouble(configValue(context, "overlay_opacity/" + key,
				"ConstellationMgr.linesColor".equals(key) ? "0.15" : "1.0"));
	}

	static Integer color(Context context, String id)
	{
		final String value = value(context, id);
		if (value == null)
			return null;
		final String[] rgb = value.split(",");
		int color = 0xFF000000;
		for (int i = 0; i < 3; ++i)
			color |= Math.round(Math.max(0f, Math.min(1f, Float.parseFloat(rgb[i].trim()))) * 255f)
					<< (16 - i * 8);
		return color;
	}

	private static String value(Context context, String id)
	{
		final String[] entry = entries.get(id);
		if (entry == null)
			return null;
		return configValue(context, entry[0], entry[1]);
	}

	private static String configValue(Context context, String key, String fallback)
	{
		if (shipped == null)
		{
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.getAssets().open("data/default_cfg.ini"), StandardCharsets.UTF_8)))
			{
				shipped = read(reader);
			}
			catch (IOException e)
			{
				shipped = new HashMap<>();
			}
		}
		return shipped.containsKey(key) ? shipped.get(key) : fallback;
	}

	static Map<String, String> read(BufferedReader reader) throws IOException
	{
		final Map<String, String> values = new HashMap<>();
		String section = "";
		String line;
		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#") || line.startsWith(";"))
				continue;
			if (line.startsWith("[") && line.endsWith("]"))
			{
				section = line.substring(1, line.length() - 1).trim();
				continue;
			}
			final int equals = line.indexOf('=');
			if (equals > 0)
				values.put(section + "/" + line.substring(0, equals).trim(),
						line.substring(equals + 1).trim());
		}
		return values;
	}
}
