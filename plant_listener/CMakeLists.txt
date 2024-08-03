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
Project("Plant Listener")

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

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
endif()

add_subdirectory(devices)
add_subdirectory(client)
add_subdirectory(core)
add_subdirectory(util)

if("${CMAKE_BUILD_TYPE}" STREQUAL "test")
    include(GoogleTest)
    add_subdirectory(test)
endif()
