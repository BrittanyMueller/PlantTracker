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
add_library(dht22 STATIC dht22.c)
target_include_directories(dht22 PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(dht22 PRIVATE pigpio rt Threads::Threads)
