#
# (C) Copyright 2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#

add_subdirectory(server)

add_executable(plantlistener_live_test
    live_test.cpp
)

target_link_libraries(plantlistener_live_test 
    gtest 
    gtest_main
    plantlistener-core_objlib
    plantlistener-test-server
)



gtest_add_tests(TARGET plantlistener_live_test WORKING_DIRECTORY ${CMAKE_BINARY_DIR})
