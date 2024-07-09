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

#include <memory>
#include <plantlistener/plant_listener.hpp>
#include <plantlistener/sensor/humidity_sensor.hpp>
#include <plantlistener/sensor/light_sensor.hpp>
#include <plantlistener/sensor/moisture_sensor.hpp>
#include <plantlistener/sensor/temp_sensor.hpp>

using plantlistener::Error;
using plantlistener::Expected;
using plantlistener::core::PlantListener;
using plantlistener::core::PlantListenerConfig;
using plantlistener::core::Sensor;

using planttracker::grpc::LightSensorData;
using planttracker::grpc::MoistureSensorData;
using planttracker::grpc::PlantData;
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

Error PlantListener::addSensor(const plantlistener::core::SensorConfig& cfg) {
  spdlog::info("Adding sensor (id: {}, dev_name {}, dev_port {}, type {})", cfg.id, cfg.device_name, cfg.device_port, static_cast<int>(cfg.type));

  std::unique_ptr<Sensor> sensor = nullptr;
  std::shared_ptr<plantlistener::device::Device> sensor_dev;

  auto dev_it = devices_.find(cfg.device_name);
  if (dev_it == devices_.end()) {
    return {Error::Code::ERROR_NOT_FOUND, fmt::format("Sensor requested device {}:{} but wasn't found", cfg.device_name, cfg.device_port)};
  }

  switch (cfg.type) {
    case SensorType::LIGHT:
      sensor = std::make_unique<plantlistener::core::LightSensor>(cfg, dev_it->second);
      break;
    case SensorType::TEMP:
      sensor = std::make_unique<plantlistener::core::TempSensor>(cfg, dev_it->second);
      break;
    case SensorType::HUMIDITY:
      sensor = std::make_unique<plantlistener::core::HumiditySensor>(cfg, dev_it->second);
      break;
    case SensorType::MOISTURE:
      sensor = std::make_unique<plantlistener::core::MoistureSensor>(cfg, dev_it->second);
      break;
    default:
      return {Error::Code::ERROR_INTERNAL, fmt::format("Tried to create unknown sensor type {} dev {}:{}!", static_cast<int>(cfg.type), cfg.device_name, cfg.device_port)};
  }
  // If the constructor failed just return the error code instead of the object.
  if (sensor->valid().isError()) {
    return sensor->valid();
  }
  sensors_.emplace_back(std::move(sensor));
  return {};
}

Error PlantListener::addPlant(const PlantConfig& cfg) {
  spdlog::info("Adding plant (id: {}, dev_name {}, dev_port {})", cfg.id, cfg.moisture_device_name, cfg.moisture_device_port);
  plants_.emplace_back(std::make_shared<Plant>(std::move(cfg)));

  // Non-Moisture sensors provide data to all plants so add them once all plants are added
  for (auto& sensor : sensors_) {
    if (sensor->getType() == SensorType::MOISTURE) continue;
    auto res = sensor->addPlant(plants_.back());
    if (res.isError()) {
      return res;
    }
  }

  // Now create a sensor for the plant
  SensorConfig sensor_cfg {};
  sensor_cfg.device_name = cfg.moisture_device_name;
  sensor_cfg.device_port = cfg.moisture_device_port;
  sensor_cfg.type = SensorType::MOISTURE;
  sensor_cfg.id = cfg.id; // Associate the sensor ID with the plant.

  auto res = addSensor(sensor_cfg);
  if (res.isError()) {
    return res;
  }

  // now associated the last sensor with the plant.
  return sensors_.back()->addPlant(plants_.back());
}

Error PlantListener::init() {
  std::lock_guard<std::mutex> lck(mutex_);

  if (state_ != State::NOT_INITALIZED) {
    return {plantlistener::Error::Code::ERROR_AGAIN, "PlantListener is already initalized."};
  }

  device_loader_ = std::make_unique<plantlistener::core::DeviceLoader>();

  // load devices
  for (const auto& dev_cfg : cfg_.devices) {
    spdlog::debug("Loading device {} from {}", dev_cfg.name, dev_cfg.lib);
    auto dev_res = device_loader_->createDevice(dev_cfg);
    if (dev_res.isError()) {
      return dev_res;
    }

    devices_.emplace(std::make_pair(dev_cfg.name, dev_res.getValue()));
  }

  // load pre-defined sensors
  for (const auto& sensor_cfg : cfg_.sensors) {
    auto res = addSensor(sensor_cfg);
    if (res.isError()) {
      return res;
    }
  }

  state_ = State::INITALIZED;
  return {};
}

Error PlantListener::start() {
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

  // Init the data with the server
  {
    grpc::ClientContext client_context;
    planttracker::grpc::PlantListenerConfig cfg;
    planttracker::grpc::InitializeResponse res;

    for (const auto& [name, dev] : devices_) {
      if (dev->getType() != plantlistener::device::DeviceType::ADC) {
        continue;  // We only care about ADC's
      }

      cfg.set_name(cfg_.name);
      auto* new_dev = cfg.add_devices();
      new_dev->set_name(name);
      new_dev->set_num_sensors(dev->getPortCount());  // TODO(qawse3dr) account for water sensor using a port.
    }

    auto status = client->Initialize(&client_context, cfg, &res);
    if (!status.ok()) {
      return {Error::Code::ERROR_NETWORKING, fmt::format("Failed to initialize with: {}", status.error_message())};
    } else if (res.res().return_code() != 0) {
      return {Error::Code::ERROR_NETWORKING, fmt::format("Failed to initialize with: {}", res.res().error())};
    }

    // Adds all plants based on the return.
    for (const auto& plant : res.plants()) {
      PlantConfig plant_cfg;
      plant_cfg.id = plant.id();
      plant_cfg.moisture_device_name = plant.device_name();
      plant_cfg.moisture_device_port = plant.device_port();

      auto res = addPlant(plant_cfg);
      if (res.isError()) {
        return res;
      }
    }
  }

  spdlog::info("PlantListener started!");
  state_ = State::STARTED;

  // Poll until we are told to stop
  Error res;
  while (state_ == State::STARTED) {
    auto next_poll = std::chrono::steady_clock::now() + cfg_.poll_rate;
    SPDLOG_DEBUG("POLLING SENSORS");

    for (const auto& sensor : sensors_) {
      res = sensor->updatePlants();
      if (res.isError()) {
        spdlog::error("Encountered error when updating plants: {}", res.toStr());
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

      plant_data->set_plant_id(1);  // Do they need an ID?
      plant_data->set_humidity(data.humidity_data);
      plant_data->set_temp(data.temp_data);

      LightSensorData* lightData = new LightSensorData;
      lightData->set_sensor_value(data.light_data);
      lightData->set_lumens(data.light_data * 1.023);  // TODO replace a with a real coefficient.

      MoistureSensorData* moistureData = new MoistureSensorData;
      moistureData->set_sensor_value(data.moisture_data);
      moistureData->set_moisture_level(data.moisture_data / 255.0);

      plant_data->set_allocated_light(lightData);
      plant_data->set_allocated_moisture(moistureData);
    }

    {
      grpc::ClientContext client_context;
      auto status = client->ReportSensor(&client_context, plant_data_list, &report_res);
      if (!status.ok()) {
        res = {Error::Code::ERROR_NETWORKING, fmt::format("Failed to report sensor with: {}", status.error_message())};
        break;
      }
    }

    cv_.wait_until(lck, next_poll);
  }

  // Shut down GRPC client.

  // We are no longer running so notify whoever is stopping us we are finished.
  state_ = State::INITALIZED;
  cv_.notify_all();
  return res;
}

Error PlantListener::stop() {
  std::unique_lock<std::mutex> lck(mutex_);
  if (state_ == State::STOPPING) {
    return {Error::Code::ERROR_AGAIN, "PlantListener stop already requested."};
  } else if (state_ != State::STARTED) {
    return {Error::Code::ERROR_NOT_INIT, "PlantListener not running"};
  }
  state_ = State::STOPPING;
  spdlog::info("Stopping PlantListener please wait... ");

  // Wait until the state changes to init meaning it stopped.
  cv_.wait(lck, [&] { return state_ == State::INITALIZED; });
  spdlog::info("Stopped PlantListener!");
  return {};
}
