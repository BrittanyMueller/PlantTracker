#
# (C) Copyright 2023 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#
Project("Plant Listener" VERSION 1.0.2)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Needed for rpi
link_libraries(atomic)

# Grab nlohmann JSON
include(FetchContent)
FetchContent_Declare(
    nlohmann_json
    GIT_REPOSITORY  https://github.com/nlohmann/json.git
    GIT_TAG         v3.11.3
)
FetchContent_Populate(nlohmann_json)
add_library(json INTERFACE)
target_include_directories(json INTERFACE ${nlohmann_json_SOURCE_DIR}/single_include/)

# Add spdlog for logging
find_package(spdlog REQUIRED)
find_package(fmt REQUIRED)

if("${CMAKE_BUILD_TYPE}" STREQUAL "test" OR "${CMAKE_BUILD_TYPE}" STREQUAL "coverage")
    add_compile_options(-g -Wall -fprofile-arcs -ftest-coverage)
    link_libraries(gcov)
elseif("${CMAKE_BUILD_TYPE}" STREQUAL "debug")
    add_compile_options(-g -Wall)
elseif("${CMAKE_BUILD_TYPE}" STREQUAL "release")
    add_compile_options(-Wall -Werror -O3)
    add_link_options(-static-libstdc++)
endif()

add_subdirectory(devices)
add_subdirectory(client)
add_subdirectory(core)
add_subdirectory(util)

if("${CMAKE_BUILD_TYPE}" STREQUAL "test")
    include(GoogleTest)
    add_subdirectory(test)
else()
    install(TARGETS 
        plant_listener
        plantlistener-device-mcp3008-adc-dev
        plantlistener-device-dht22-temp-dev
        plantlistener-device-dh1750-light-dev)

    install(
        FILES
        ${CMAKE_SOURCE_DIR}/deployment/plantlistener.service
        DESTINATION /etc/systemd/system/
    )

    
    install(
        FILES
        ${CMAKE_CURRENT_SOURCE_DIR}/data/config_v2.json
        RENAME plant_listener.json
        DESTINATION /etc/
        )
        
    ## Install info
    set(CPACK_PACKAGE_FILE_NAME "plant-listener")
    set(CPACK_PACKAGE_NAME "plant-listener")
    set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "Client component for Plant Tracker")
    set(CPACK_PACKAGE_VENDOR "larrycloud")
    set(CPACK_VERBATIM_VARIABLES YES)
    set(CPACK_STRIP_FILES YES)
    SET(CPACK_OUTPUT_FILE_PREFIX "${CMAKE_BINARY_DIR}/_packages")
    set(CPACK_PACKAGING_INSTALL_PREFIX "/usr")
    set(CPACK_PACKAGE_VERSION_MAJOR ${PROJECT_VERSION_MAJOR})
    set(CPACK_PACKAGE_VERSION_MINOR ${PROJECT_VERSION_MINOR})
    set(CPACK_PACKAGE_VERSION_PATCH ${PROJECT_VERSION_PATCH})
    set(CPACK_PACKAGE_CONTACT "temp@larrycloud.ca")
    set(CPACK_DEBIAN_PACKAGE_MAINTAINER "Larry Milne")
    set(CPACK_DEBIAN_PACKAGE_CONTROL_EXTRA ${CMAKE_CURRENT_SOURCE_DIR}/cpack/conffiles)
    set(CPACK_DEBIAN_FILE_NAME DEB-DEFAULT)
    set(CPACK_DEB_COMPONENT_INSTALL YES)
    set(CPACK_DEBIAN_PACKAGE_DEPENDS "")
    set(CPACK_RESOURCE_FILE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/../LICENSE")
    set(CPACK_RESOURCE_FILE_README "${CMAKE_CURRENT_SOURCE_DIR}/README.md")
    set(CPACK_COMPONENTS_GROUPING ALL_COMPONENTS_IN_ONE)

    include(CPack)
endif()
