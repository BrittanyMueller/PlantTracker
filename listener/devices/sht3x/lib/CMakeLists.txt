#
# (C) Copyright 2025 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARANTINES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#

find_package(Threads REQUIRED)
add_library(sht3x STATIC sht3x.cpp)
target_include_directories(sht3x PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(sht3x PRIVATE pigpio rt Threads::Threads)
