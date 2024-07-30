#
# (C) Copyright 2023-2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#

# Headers for both devices and using devices.
add_library(plantlistener-device_headers INTERFACE)

# Build device object library
add_library(plantlistener-device_objlib src/device.cpp src/device_config.cpp)
target_link_libraries(plantlistener-device_objlib PUBLIC json spdlog::spdlog fmt::fmt plantlistener-util_objlib)
target_include_directories(plantlistener-device_objlib PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include/)


# Only build the mock sensor if we aren't on actual hardware
add_subdirectory(mock)
add_subdirectory(ADS7830)
add_subdirectory(dht22)
