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

add_library(plantlistener-device-mock-dev SHARED
  mock_device.cpp
)

target_link_libraries(plantlistener-device-mock-dev
  PRIVATE 
  plantlistener-device_objlib
  json
)