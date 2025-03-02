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

add_executable(plantlistener_util_test
    test_error.cpp
    test_expected.cpp
    test_ld_loader.cpp
)

target_link_libraries(plantlistener_util_test 
    gtest 
    gmock
    gtest_main
    plantlistener-util_objlib
)

# Test lib for test_ld_loader
add_library(ld_loader_lib SHARED ld_loader_lib.c)
add_library(ld_loader_lib2 SHARED ld_loader_lib.c)

gtest_add_tests(TARGET plantlistener_util_test WORKING_DIRECTORY ${CMAKE_BINARY_DIR})
