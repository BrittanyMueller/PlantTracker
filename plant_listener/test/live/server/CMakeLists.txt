#
# (C) Copyright 2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#

# Shared object for unit tests.
add_library(plantlistener-test-server SHARED src/test_plant_server.cpp)
target_link_libraries(plantlistener-test-server PRIVATE plantlistener-util_objlib)
target_link_libraries(plantlistener-test-server PUBLIC plant_tracker_idl plantlistener-core_objlib)
target_include_directories(plantlistener-test-server PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

# Server for manual testing.
add_executable(plantlistener_test_server src/main.cpp)
target_link_libraries(plantlistener_test_server plantlistener-test-server plantlistener-util_objlib)
