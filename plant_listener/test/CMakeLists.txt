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


# In order to add a test suite use the following format.
# add_executable(plantlistener_test
#     example_test.cpp
# )

# target_link_libraries(plantlistener_test 
#     gtest 
#     gtest_main
# )

# gtest_add_tests(TARGET plantlistener_test)

add_subdirectory(client)
add_subdirectory(core)
add_subdirectory(util)

add_custom_target(plantlistener_coverage
    WORKING_DIRECTORY ${CMAKE_BINARY_DIR}
    COMMAND pwd
    COMMAND echo "=================== LCOV ===================="
    COMMAND echo "-- Passing lcov tool under code coverage"
    COMMAND lcov --capture --directory plant_listener --include plant_listener --exclude test --output-file lcoverage/main_coverage.info
    COMMAND echo "-- Generating HTML output files"
    COMMAND genhtml lcoverage/main_coverage.info --output-directory coverage/plantlistener
    )


