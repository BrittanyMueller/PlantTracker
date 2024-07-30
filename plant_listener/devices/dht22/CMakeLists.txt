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

add_executable(dht22_tester dht22-main.c)
target_link_libraries(dht22_tester dht22)


add_library(plantlistener-device-dht22-temp-dev SHARED device_dht22.cpp)

target_link_libraries(plantlistener-device-dht22-temp-dev
  PRIVATE 
  plantlistener-device_objlib
  json
  dht22
)