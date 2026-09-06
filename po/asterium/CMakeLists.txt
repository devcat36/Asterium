SET(asterium_POT asterium.pot)

FILE(GLOB pofiles RELATIVE "${CMAKE_SOURCE_DIR}/po/asterium/" "*.po")
LIST(SORT pofiles)
STRING(REGEX REPLACE ".po" "" outpofiles "${pofiles}")

ADD_CUSTOM_TARGET(
     generate-pot-asterium
     python3 ${CMAKE_SOURCE_DIR}/util/extract-ui-strings.py --pot
             ${CMAKE_SOURCE_DIR}/android/java
             ${CMAKE_CURRENT_SOURCE_DIR}/asterium.pot
     WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
     COMMENT "Generating asterium.pot"
     VERBATIM
)
SET_TARGET_PROPERTIES(generate-pot-asterium PROPERTIES FOLDER "po")

GETTEXT_CREATE_TRANSLATIONS(asterium DEFAULT_TARGET ${outpofiles})

IF(TARGET generate-pot)
     ADD_DEPENDENCIES(generate-pot generate-pot-asterium)
ENDIF()
