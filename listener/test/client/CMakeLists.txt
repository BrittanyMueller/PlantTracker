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

add_executable(plantlistener_client_test
    test_command_line.cpp
)

target_link_libraries(plantlistener_client_test 
    PUBLIC
    gtest 
    gmock
    gtest_main
    plantlistener-client_objlib
    test_common
)

gtest_add_tests(TARGET plantlistener_client_test)
