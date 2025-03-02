/*
 * (C) Copyright 2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
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
#include <thread>

using plantlistener::Error;
using plantlistener::test::RequestQueue;
using plantlistener::test::TestPlantServer;

class PlantListenerServiceImpl final : public planttracker::grpc::PlantListener::Service {
 private:
  size_t max_data_records = 100;  // max data records to save memory.
  std::vector<planttracker::grpc::MoistureDevice>& devices_;
  std::vector<planttracker::grpc::PlantSensorData>& data_;
  std::string dev_name_;
  RequestQueue& request_queue_;

 public:
  PlantListenerServiceImpl(std::vector<planttracker::grpc::MoistureDevice>& devices,
                           std::vector<planttracker::grpc::PlantSensorData>& data, RequestQueue& request_queue)
      : devices_(devices), data_(data), request_queue_(request_queue) {}

 private:
  grpc::Status initialize(grpc::ServerContext* context, const planttracker::grpc::PlantListenerConfig* cfg,
                          planttracker::grpc::InitializeResponse* response) override {
    spdlog::info("initialzed called for \"{}\". START", cfg->name());
    for (const auto& dev : cfg->devices()) {
      spdlog::info("Devices (name: {}, num_sensors: {})", dev.name(), dev.num_sensors());
      dev_name_ = dev.name();  // Grab the last dev name so we can use it for a fake plant.
      devices_.push_back(dev);
    }
    spdlog::info("initialzed called for \"{}\". END", cfg->name());

    // Returns fake plants
    auto* plant = response->add_plants();
    plant->set_device_name(dev_name_);
    plant->set_sensor_port(1);
    plant->set_plant_id(1);

    return grpc::Status::OK;
  }
  grpc::Status reportSensor(grpc::ServerContext* context, const planttracker::grpc::PlantSensorDataList* request,
                            planttracker::grpc::Result* response) override {
    std::stringstream report;
    report << "\n--------reportSensor Start----------\n";
    for (const auto& plant_data : request->data()) {
      report << "plant_id: " << plant_data.plant_id() << " Moisture: " << plant_data.moisture().sensor_value()
             << " Light: " << plant_data.light().sensor_value() << " Humidity: " << plant_data.humidity()
             << " Temp: " << plant_data.temp() << std::endl;
      if (data_.size() < max_data_records) data_.push_back(plant_data);
    }
    report << "--------reportSensor End----------\n";
    spdlog::info(report.str());

    response->set_return_code(static_cast<int>(Error::Code::OK));
    return grpc::Status::OK;
  }
  grpc::Status poll(grpc::ServerContext* context, const planttracker::grpc::PollRequest* req,
                    grpc::ServerWriter<planttracker::grpc::ListenerRequest>* stream) override {
    spdlog::info("poll() called");

    while (true) {
      auto request = request_queue_.getRequest();
      planttracker::grpc::Result res;
      stream->Write(request);

      if (request.type() == planttracker::grpc::ListenerRequestType::SHUTDOWN) {
        return grpc::Status::OK;
      }
    }
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

  service_ = std::make_unique<PlantListenerServiceImpl>(devices, data, request_queue);
  grpc::ServerBuilder builder;
  builder.AddListeningPort(address, grpc::InsecureServerCredentials());
  builder.RegisterService(service_.get());
  server_ = builder.BuildAndStart();

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
  server_->Shutdown();
  return {};
};

Error TestPlantServer::wait() {
  server_->Wait();
  return {};
}
