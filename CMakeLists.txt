#
# (C) Copyright 2023 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#
cmake_minimum_required(VERSION 3.20)
Project("Plant Tracker")

set(CMAKE_CONFIGURATION_TYPES
debug
release
test # Don't build raspberrypi specific libaries
)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

if("${CMAKE_BUILD_TYPE}" STREQUAL "test")
    enable_testing()
endif()


add_subdirectory(idl)
add_subdirectory(plant_listener)

add_custom_target(PlantTracker
    COMMAND mkdir -p ${CMAKE_SOURCE_DIR}/app
    COMMAND cd ${CMAKE_SOURCE_DIR}/PlantTracker/ && ./gradlew assembleDebug
    COMMAND cp ${CMAKE_SOURCE_DIR}/PlantTracker/app/build/outputs/apk/debug/app-debug.apk ${CMAKE_BINARY_DIR}/app
)

add_custom_target(format
    COMMAND find ${CMAKE_SOURCE_DIR} -name '*.h' -o -name '*.hpp' -o -name '*.c' -o -name '*.cpp' -o -name '*.java' -exec clang-format -i {} "+"
    COMMAND echo "Format Done"
    )

add_custom_target(test-format
    COMMAND find ${CMAKE_SOURCE_DIR} -name '*.h' -o -name '*.hpp' -o -name '*.c' -o -name '*.cpp' -o -name '*.java' -exec clang-format --dry-run -Werror {} "+"
    COMMAND echo "Format Check Passed"
)

