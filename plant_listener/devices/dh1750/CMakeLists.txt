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
add_subdirectory(lib)

add_executable(dh1750_tester main.cpp)
target_link_libraries(dh1750_tester dh1750)

add_library(plantlistener-device-dh1750-light-dev SHARED device_dh1750.cpp)

target_link_libraries(plantlistener-device-dh1750-light-dev
  PRIVATE 
  plantlistener-device_objlib
  json
  dh1750
)