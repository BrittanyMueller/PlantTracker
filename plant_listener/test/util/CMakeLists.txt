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

add_executable(plantlistener_util_test
    test_error.cpp
)

target_link_libraries(plantlistener_util_test 
    gtest 
    gtest_main
    plantlistener-util_objlib
)

gtest_add_tests(TARGET plantlistener_util_test)