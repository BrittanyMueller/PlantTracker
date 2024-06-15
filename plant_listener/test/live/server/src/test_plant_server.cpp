/*
 * (C) Copyright 2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */

#include <grpcpp/grpcpp.h>
#include <planttracker.grpc.pb.h>
#include <spdlog/spdlog.h>

#include <plantlistener/test/test_plant_server.hpp>
#include <sstream>

using plantlistener::Error;
using plantlistener::test::TestPlantServer;

class PlantListenerServiceImpl final : public planttracker::grpc::PlantListener::Service {
  grpc::Status Initialize(grpc::ServerContext* context, const planttracker::grpc::PlantListenerConfig* request,
                          planttracker::grpc::InitializeResponse* response) {
    spdlog::info("Initialzed called.");
    spdlog::info("PlantInfo.");

    return grpc::Status::OK;
  }
  grpc::Status ReportSensor(grpc::ServerContext* context, const planttracker::grpc::PlantDataList* request,
                            planttracker::grpc::Result* response) {
    std::stringstream report;
    report << "\n--------ReportSensor Start----------\n";
    for (const auto& plant_data : request->data()) {
      report << "plant_id: " << plant_data.plant_id() << " Moisture: " << plant_data.moisture().sensor_value()
             << " Light: " << plant_data.light().sensor_value() << " Humidity: " << plant_data.humidity() << " Temp: " << plant_data.temp() << std::endl;
    }
    report << "--------ReportSensor End----------\n";
    spdlog::info(report.str());

    response->set_return_code(static_cast<int>(Error::Code::OK));
    return grpc::Status::OK;
  }
  grpc::Status PollRequest(
      grpc::ServerContext* context,
      grpc::ServerReaderWriter<planttracker::grpc::ListenerResponse, planttracker::grpc::ListenerResponse>* stream) {
    spdlog::info("PollRequest Started.");

    // TODO(lmilne) we might need a queue for the test server and maybe a cli to control it for testing.

    return grpc::Status::OK;
  }
};

TestPlantServer::TestPlantServer() = default;
TestPlantServer::~TestPlantServer() = default;

Error TestPlantServer::start() {
  if (state_ != State::NOT_INITALIZED) {
    spdlog::warn("PlantSever already started");
    return {Error::Code::ERROR_AGAIN, "PlantSever already running."};
  }

  state_ = State::INITIALIZING;
  std::string address = "127.0.0.1:1234";

  PlantListenerServiceImpl service;
  grpc::ServerBuilder builder;
  builder.AddListeningPort(address, grpc::InsecureServerCredentials());
  builder.RegisterService(&service);
  server = builder.BuildAndStart();

  spdlog::info("Starting server on {}", address);
  state_ = State::STARTED;
  server->Wait();
  return {};
};

Error TestPlantServer::stop() {
  if (state_ != State::STARTED) {
    spdlog::warn("PlantSever not started");
    return {Error::Code::ERROR_NOT_INIT, "PlantSever not started."};
  }
  state_ = State::STOPPING;
  server->Shutdown();
  return {};
};
