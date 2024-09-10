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
cmake_minimum_required(VERSION 3.13)

# set(CMAKE_CXX_COMPILER "g++")
Project("Plant Tracker")

set(CMAKE_CONFIGURATION_TYPES
debug
release
test # Don't build raspberrypi specific libaries
)
set (CMAKE_CXX_STANDARD 20)

if (NOT DEFINED CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE debug)
endif()

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if("${CMAKE_BUILD_TYPE}" STREQUAL "test")
    enable_testing()
endif()

add_subdirectory(idl)

if(NOT "$ENV{BUILD_PLANT_LISTENER}" STREQUAL "0")
    add_subdirectory(plant_listener)
endif()

add_custom_target(PlantTracker
    COMMAND mkdir -p ${CMAKE_SOURCE_DIR}/app
    COMMAND chmod +x ${CMAKE_SOURCE_DIR}/android/plant_tracker//gradlew
    COMMAND cd ${CMAKE_SOURCE_DIR}/android/plant_tracker/ && ./gradlew assembleDebug
    COMMAND cp ${CMAKE_SOURCE_DIR}/android/plant_tracker/app/build/outputs/apk/debug/app-debug.apk ${CMAKE_BINARY_DIR}/app
)

set(FORMAT_SUFFIXES .h .hpp .c .cpp .java .proto)
string(JOIN "' -o -name '*" FORMAT_SUFFIXES_OUT ${FORMAT_SUFFIXES})
string(PREPEND FORMAT_SUFFIXES_OUT " -name '*")
string(APPEND FORMAT_SUFFIXES_OUT "'")

add_custom_target(format
    COMMAND bash -c "find ${CMAKE_SOURCE_DIR} -not -path \"*build/*\" -a \"\\(\" ${FORMAT_SUFFIXES_OUT} \"\\)\" -exec clang-format -i {} \"+\""
    COMMAND echo "Format Done"
    )

add_custom_target(test-format
    COMMAND bash -c "find ${CMAKE_SOURCE_DIR} -not -path \"*build/*\" -a \"\\(\" ${FORMAT_SUFFIXES_OUT} \"\\)\" -exec clang-format --dry-run -Werror {} \"+\""
    COMMAND echo "Format Check Passed"
)

