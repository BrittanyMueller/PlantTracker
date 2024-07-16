#
# (C) Copyright 2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#

add_executable(plantlistener_core_test
    test_plantlistener_config.cpp
    test_plantlistener.cpp
)

target_link_libraries(plantlistener_core_test 
    gtest 
    gtest_main
    plantlistener-core_objlib
    test_common
)

gtest_add_tests(TARGET plantlistener_core_test)