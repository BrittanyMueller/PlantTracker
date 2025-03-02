#
# (C) Copyright 2023 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#

add_library(plantlistener-client_objlib OBJECT src/command_line.cpp)
target_include_directories(plantlistener-client_objlib PUBLIC src)
target_link_libraries(plantlistener-client_objlib PUBLIC plantlistener-core_objlib)

add_executable(plant_listener
  src/main.cpp
)
target_link_libraries(plant_listener plantlistener-client_objlib)