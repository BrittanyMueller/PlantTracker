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

#include <plantlistener/plant_listener.hpp>

using plantlistener::core::PlantListener;
using plantlistener::core::PlantListenerConfig;
using planttracker::grpc::PlantData;
using planttracker::grpc::LightSensorData;
using planttracker::grpc::MoistureSensorData;
using planttracker::grpc::PlantDataList;

PlantListener::PlantListener(PlantListenerConfig&& cfg) : cfg_(std::move(cfg)) {}

PlantListener::~PlantListener() {
  // Ensure objects get destroyed in the right order.

  // Sensors have plants and devices so destroy it first.
  sensors_.clear();

  // Plants and devices_ can now get freed.
  plants_.clear();
  devices_.clear();

  // Now that all devices are destroyed we can get rid of the loader.
  device_loader_.reset();
}

plantlistener::Error PlantListener::init() {
  std::lock_guard<std::mutex> lck(mutex_);

  if (state_ != State::NOT_INITALIZED) {
    return {plantlistener::Error::Code::ERROR_AGAIN, "PlantListener is already initalized."};
  }

  // TODO do init.

  state_ = State::INITALIZED;
  return {};
}

plantlistener::Error PlantListener::start() {
  std::unique_lock<std::mutex> lck(mutex_);

  if (state_ == State::STARTED) {
    return {plantlistener::Error::Code::ERROR_AGAIN, "PlantListener is already running."};
  } else if (state_ != State::INITALIZED) {
    return {plantlistener::Error::Code::ERROR_NOT_INIT, "PlantListener not initalized."};
  }
  spdlog::info("Starting PlantListener please wait... ");

  // Start GRPC client.
  spdlog::info("Connecting too {}:{}", cfg_.address, cfg_.port);

  std::shared_ptr<grpc::Channel> channel =
      grpc::CreateChannel(fmt::format("{}:{}", cfg_.address, cfg_.port), grpc::InsecureChannelCredentials());
  std::unique_ptr<planttracker::grpc::PlantListener::Stub> client(planttracker::grpc::PlantListener::NewStub(channel));
  grpc::ClientContext client_context;

  spdlog::info("PlantListener started!");
  state_ = State::STARTED;

  // Poll until we are told to stop
  Error res;
  while (state_ == State::STARTED) {
    auto next_poll = std::chrono::steady_clock::now() + cfg_.poll_rate;
    spdlog::info("polling sensors");
    SPDLOG_DEBUG("POLLING SENSORS");

    for (const auto& sensor : sensors_) {
      res = sensor->updatePlants();
      if (res.isError()) {
        spdlog::error("encountered error when updating plants: {}", res.toStr());
        break;
      }
    }

    // send update to grpc
    PlantDataList plant_data_list;
    plant_data_list.Clear();
    planttracker::grpc::Result report_res;
    for (const auto& plant : plants_) {
      const auto& data = plant->getPlantData();
      auto* plant_data = plant_data_list.add_data();

      plant_data->set_plant_id(plant->getId());
      
      LightSensorData* lightData = new LightSensorData;
      lightData->set_sensor_value(data.light_data);
      lightData->set_lumens(data.light_data * 1.023); // TODO replace a with a real coefficient.
      
      MoistureSensorData* moistureData = new MoistureSensorData;
      moistureData->set_sensor_value(data.moisture_data);
      moistureData->set_moisture_level(data.moisture_data / 255.0);

      plant_data->set_allocated_light(lightData);
      plant_data->set_allocated_moisture(moistureData);

    }
    client->ReportSensor(&client_context, plant_data_list, &report_res);

    cv_.wait_until(lck, next_poll);
  }

  // Shut down GRPC client.

  // We are no longer running so notify whoever is stopping us we are finished.
  state_ = State::INITALIZED;
  cv_.notify_all();
  return res;
}

plantlistener::Error PlantListener::stop() {
  std::unique_lock<std::mutex> lck(mutex_);
  if (state_ == State::STOPPING) {
    return {plantlistener::Error::Code::ERROR_AGAIN, "PlantListener stop already requested."};
  } else if (state_ != State::STARTED) {
    return {plantlistener::Error::Code::ERROR_NOT_INIT, "PlantListener not running"};
  }
  state_ = State::STOPPING;
  spdlog::info("Stopping PlantListener please wait... ");

  // Wait until the state changes to init meaning it stopped.
  cv_.wait(lck, [&] { return state_ == State::INITALIZED; });
  spdlog::info("Stopped PlantListener!");
  return {};
}
