#
# (C) Copyright 2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARANTINES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#

add_subdirectory(lib)

add_executable(sht3x_tester sht3x-main.cpp)
target_link_libraries(sht3x_tester sht3x)


add_library(plantlistener-device-sht3x-temp-dev SHARED device_sht3x.cpp)

target_link_libraries(plantlistener-device-sht3x-temp-dev
  PRIVATE 
  plantlistener-device_objlib
  json
  sht3x
  spdlog
)