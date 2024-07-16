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
 private:
  size_t max_data_records = 100;  // max data records to save memory.
  std::vector<planttracker::grpc::MoistureDevice>& devices_;
  std::vector<planttracker::grpc::PlantData>& data_;

 public:
  PlantListenerServiceImpl(std::vector<planttracker::grpc::MoistureDevice>& devices,
                           std::vector<planttracker::grpc::PlantData>& data)
      : devices_(devices), data_(data) {}

 private:
  grpc::Status Initialize(grpc::ServerContext* context, const planttracker::grpc::PlantListenerConfig* cfg,
                          planttracker::grpc::InitializeResponse* response) {
    std::string dev_name = "foobar";
    spdlog::info("Initialzed called for \"{}\". START", cfg->name());
    for (const auto& dev : cfg->devices()) {
      spdlog::info("Devices (name: {}, num_sensors: {})", dev.name(), dev.num_sensors());
      dev_name = dev.name();  // Grab the last dev name so we can use it for a
                              // fake plant.
      devices_.push_back(dev);
    }
    spdlog::info("Initialzed called for \"{}\". END", cfg->name());

    // Returns fake plants
    auto* plant = response->add_plants();
    plant->set_device_name(dev_name);
    plant->set_device_port(1);
    plant->set_id(1);

    return grpc::Status::OK;
  }
  grpc::Status ReportSensor(grpc::ServerContext* context, const planttracker::grpc::PlantDataList* request,
                            planttracker::grpc::Result* response) {
    std::stringstream report;
    report << "\n--------ReportSensor Start----------\n";
    for (const auto& plant_data : request->data()) {
      report << "plant_id: " << plant_data.plant_id() << " Moisture: " << plant_data.moisture().sensor_value()
             << " Light: " << plant_data.light().sensor_value() << " Humidity: " << plant_data.humidity()
             << " Temp: " << plant_data.temp() << std::endl;
      if (data_.size() < max_data_records) data_.push_back(plant_data);
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

    // TODO(lmilne) we might need a queue for the test server and maybe a cli to
    // control it for testing.

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
  std::string address = "127.0.0.1:5051";

  service = std::make_unique<PlantListenerServiceImpl>(devices, data);
  grpc::ServerBuilder builder;
  builder.AddListeningPort(address, grpc::InsecureServerCredentials());
  builder.RegisterService(service.get());
  server = builder.BuildAndStart();

  spdlog::info("Starting server on {}", address);
  state_ = State::STARTED;
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

Error TestPlantServer::wait() {
  server->Wait();
  return {};
}
