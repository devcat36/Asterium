
########### install files ###############
INSTALL(FILES ssystem_major.ini ssystem_minor.ini ssystem_1000comets.ini 
	base_locations.bin.gz default_cfg.ini languages.tab iso3166.tab 
	regions-geoscheme.tab constellations_spans.dat
        nomenclature.dat asteroid_elements.json DESTINATION ${SDATALOC}/data)
# install fonts
INSTALL(FILES NotoSans-Regular.ttf NotoSansMono-Regular.ttf NotoSansSC-Regular.otf
	DejaVuSans.ttf DejaVuSansMono.ttf OFL.txt DESTINATION ${SDATALOC}/data)
# install shaders
INSTALL(DIRECTORY shaders/ DESTINATION ${SDATALOC}/data/shaders FILES_MATCHING
	PATTERN "planet*")

# install splash
IF(STELLARIUM_RELEASE_BUILD)
	INSTALL(FILES splash.png DESTINATION ${SDATALOC}/data)
ELSE()
	INSTALL(FILES splash-gray.png DESTINATION ${SDATALOC}/data RENAME splash.png)
ENDIF()

# install icon
IF(MACOS)
	IF(STELLARIUM_RELEASE_BUILD)
		INSTALL(FILES stellarium.icns DESTINATION ${SDATALOC})
	ELSE()
		INSTALL(FILES stellarium-gray.icns DESTINATION ${SDATALOC} RENAME stellarium.icns)
	ENDIF()
	INSTALL(FILES Info.plist PkgInfo DESTINATION .)
ELSE()
	IF(STELLARIUM_RELEASE_BUILD)
		INSTALL(FILES stellarium.ico DESTINATION ${SDATALOC}/data)
	ELSE()
		INSTALL(FILES stellarium-gray.ico DESTINATION ${SDATALOC}/data RENAME stellarium.ico)
	ENDIF()
ENDIF()

# install Scenery3d shaders
IF(USE_PLUGIN_SCENERY3D)
INSTALL(DIRECTORY shaders/ DESTINATION ${SDATALOC}/data/shaders FILES_MATCHING
	PATTERN "s3d_*.vert" 
	PATTERN "s3d_*.geom" 
	PATTERN "s3d_*.frag")
ENDIF()

IF(UNIX AND NOT MACOS)
	INSTALL(FILES stellarium.xml DESTINATION share/mime/packages)
ENDIF()

