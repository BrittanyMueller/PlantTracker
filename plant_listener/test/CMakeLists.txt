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
add_subdirectory(common)
add_subdirectory(util)
add_subdirectory(live)


add_custom_target(plantlistener_coverage
    WORKING_DIRECTORY ${CMAKE_BINARY_DIR}
    COMMAND mkdir -p coverage
    COMMAND gcovr -r "${CMAKE_SOURCE_DIR}"  --txt --print-summary --gcov-ignore-parse-errors --filter ".*plant_listener.*" --exclude ".*test.*" --cobertura coverage.xml --html-details coverage/index.html
    )


