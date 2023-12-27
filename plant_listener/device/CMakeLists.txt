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

# Headers for both devices and using devices.
add_library(plant-listener-device_headers INTERFACE)
target_include_directories(plant-listener-device_headers INTERFACE ${CMAKE_SOURCE_DIR}/include/)

# Build device object library
add_library(plant-listener-device_objlib
    src/device_loader.cpp
)
target_link_libraries(plant-listener-device_objlib 
    PUBLIC plant-listener-device_headers
)


# Only build the mock sensor if we aren't on actual hardware
add_subdirectory(mock)
if(NOT ${CMAKE_BUILD_TYPE} STREQUAL "test")
    add_subdirectory(ADS7830)
endif()
