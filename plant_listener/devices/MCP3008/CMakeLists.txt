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

add_subdirectory(lib)

add_executable(mcp3008_tester main.cpp)
target_link_libraries(mcp3008_tester mcp3008)

add_library(plantlistener-device-mcp3008-adc-dev SHARED device_mcp3008.cpp)

target_link_libraries(plantlistener-device-mcp3008-adc-dev
  PRIVATE 
  plantlistener-device_objlib
  json
  mcp3008
)
