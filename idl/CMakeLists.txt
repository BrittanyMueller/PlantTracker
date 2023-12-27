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

include(FindProtobuf)
find_package(Protobuf REQUIRED)
find_package(gRPC CONFIG REQUIRED)

add_library(plant_tracker_idl planttracker.proto)
target_link_libraries(plant_tracker_idl PUBLIC gRPC::grpc++)
protobuf_generate(TARGET plant_tracker_idl)
protobuf_generate(
    TARGET plant_tracker_idl
    LANGUAGE grpc
    PLUGIN protoc-gen-grpc=$<TARGET_FILE:gRPC::grpc_cpp_plugin>
    PLUGIN_OPTIONS generate_mock_code=true
    GENERATE_EXTENSIONS .grpc.pb.h .grpc.pb.cc
)