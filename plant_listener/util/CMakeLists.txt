#
# (C) Copyright 2023-2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#

add_library(plantlistener-util_objlib OBJECT
    src/error.cpp
)
target_include_directories(plantlistener-util_objlib PUBLIC include)
target_link_libraries(plantlistener-util_objlib PUBLIC spdlog::spdlog fmt::fmt json)
