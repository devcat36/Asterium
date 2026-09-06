file(ARCHIVE_EXTRACT INPUT ${CMAKE_CURRENT_SOURCE_DIR}/moon-vertices-indices.bin.7z DESTINATION ${CMAKE_CURRENT_BINARY_DIR})

########### install files ###############

# install models
INSTALL(DIRECTORY ./ DESTINATION ${SDATALOC}/models
	FILES_MATCHING 
	PATTERN "*.mtl"
	PATTERN "*.png"
	PATTERN "*.obj"
	PATTERN "*.gz"
	PATTERN "CMakeFiles" EXCLUDE )
INSTALL(FILES ${CMAKE_CURRENT_BINARY_DIR}/moon-vertices-indices.bin DESTINATION ${SDATALOC}/models)
