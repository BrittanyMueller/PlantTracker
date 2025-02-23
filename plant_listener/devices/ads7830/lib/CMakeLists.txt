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

find_package(Threads REQUIRED)
add_library(ads7830 STATIC ads7830.cpp)
target_include_directories(ads7830 PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(ads7830 PUBLIC pigpio rt Threads::Threads)
