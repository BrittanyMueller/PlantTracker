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

# Build device object library
add_library(plantlistener-core_objlib
    src/device_loader.cpp
    src/sensor.cpp
    src/sensor_config.cpp
    src/plant_listener.cpp
    src/plant_listener_config.cpp
)
target_link_libraries(plantlistener-core_objlib PUBLIC plantlistener-device_objlib plantlistener-util_objlib json plant_tracker_idl uuid)
target_include_directories(plantlistener-device_objlib PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include/)
