#
# (C) Copyright 2023-2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
#
# This code is distributed on "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# @author: qawse3dr a.k.a Larry Milne
# @author: BrittanyMueller
#

include(FindProtobuf)
find_package(Protobuf REQUIRED)
find_package(gRPC CONFIG REQUIRED)

# It seems sometimes this is needed in the case of our build server but locally it isn't so if it isn't found ignore.
find_package(utf8_range QUIET)
if (${utf8_range_FOUND})
    set(utf8_range_libs utf8_range::utf8_validity)
else()
    message(WARNING "utf8_range wasn't found if the build fails you may need to install it for protobufs.")
endif()

find_package(absl QUIET)
if (${absl_FOUND})
	set(absl_libs absl::check absl::flags)
else()
    message(WARNING "absl not found if build fails you may needs this.")
endif()

add_library(plant_tracker_idl planttracker.proto)
target_link_libraries(plant_tracker_idl PUBLIC gRPC::grpc++ protobuf::libprotobuf ${absl_libs} ${utf8_range_libs})
target_include_directories(plant_tracker_idl PUBLIC ${CMAKE_BINARY_DIR}/idl)

if (EXISTS $<TARGET_FILE:gRPC::grpc_cpp_plugin>)
    set(grpc_plugin $<TARGET_FILE:gRPC::grpc_cpp_plugin>)
else()
    # The cross compiler is ahving some trouble picking up the grpc_cpp_plugin so instead just do it by path.
    set(grpc_plugin /usr/local/bin/grpc_cpp_plugin)
endif()

protobuf_generate(TARGET plant_tracker_idl)
protobuf_generate(
    TARGET plant_tracker_idl
    LANGUAGE grpc
    PLUGIN protoc-gen-grpc=${grpc_plugin}
    GENERATE_EXTENSIONS .grpc.pb.h .grpc.pb.cc
)
