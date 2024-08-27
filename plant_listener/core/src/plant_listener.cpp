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

#include <memory>
#include <cstdlib>
#include <ctime>

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
using planttracker::grpc::PlantSensorData;
using planttracker::grpc::PlantSensorDataList;

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
  spdlog::info("Adding sensor (id: {}, dev_name {}, dev_port {}, type {})", cfg.id, cfg.device_name, cfg.device_port,
               static_cast<int>(cfg.type));

  std::unique_ptr<Sensor> sensor = nullptr;
  std::shared_ptr<plantlistener::device::Device> sensor_dev;

  auto dev_it = devices_.find(cfg.device_name);
  if (dev_it == devices_.end()) {
    return {Error::Code::ERROR_NOT_FOUND,
            fmt::format("Sensor requested device {}:{} but wasn't found", cfg.device_name, cfg.device_port)};
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
      return {Error::Code::ERROR_INTERNAL, fmt::format("Tried to create unknown sensor type {} dev {}:{}!",
                                                       static_cast<int>(cfg.type), cfg.device_name, cfg.device_port)};
  }
  // If the constructor failed just return the error code instead of the object.
  if (sensor->valid().isError()) {
    return sensor->valid();
  }
  sensors_.emplace_back(std::move(sensor));
  return {};
}

Error PlantListener::addPlant(const PlantConfig& cfg) {
  spdlog::info("Adding plant (id: {}, dev_name {}, dev_port {})", cfg.id, cfg.moisture_device_name,
               cfg.moisture_device_port);
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
  SensorConfig sensor_cfg{};
  sensor_cfg.device_name = cfg.moisture_device_name;
  sensor_cfg.device_port = cfg.moisture_device_port;
  sensor_cfg.type = SensorType::MOISTURE;
  sensor_cfg.id = cfg.id;  // Associate the sensor ID with the plant.

  auto res = addSensor(sensor_cfg);
  if (res.isError()) {
    return res;
  }

  // now associated the last sensor with the plant.
  return sensors_.back()->addPlant(plants_.back());
}

Error PlantListener::removePlant(int64_t id) {
  auto plantIt =
      std::find_if(plants_.begin(), plants_.end(), [&id](const auto& plant) -> bool { return plant->getId() == id; });

  if (plantIt == plants_.end()) {
    return Error(Error::Code::ERROR_NOT_FOUND, "Plant couldn't be removed.");
  }

  // remove the plant from the sensor
  std::vector<std::unique_ptr<plantlistener::core::Sensor>>::iterator moistureSensor = sensors_.end();
  for (auto sensorIt = sensors_.begin(); sensorIt < sensors_.end(); ++sensorIt) {
    if ((*sensorIt)->hasPlant(id)) {
      if ((*sensorIt)->getType() == SensorType::MOISTURE) {
        moistureSensor = sensorIt;
      } else {
        auto res = (*sensorIt)->removePlant(id);
        if (res.isError()) {
          return res;
        }
      }
    }
  }

  if (moistureSensor == sensors_.end()) {
    spdlog::warn("removed plant but couldn't find MoistureSensor");
  } else {
    sensors_.erase(moistureSensor);
  }
  plants_.erase(plantIt);
  return {};
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

  if (state_ == State::STARTED || state_ == State::RECONNECTING) {
    return {plantlistener::Error::Code::ERROR_AGAIN, "PlantListener is already running."};
  } else if (state_ != State::INITALIZED) {
    return {plantlistener::Error::Code::ERROR_NOT_INIT, "PlantListener not initalized."};
  }
  spdlog::info("Starting PlantListener please wait... ");

  planttracker::grpc::PlantListenerConfig cfg;

  cfg.set_name(cfg_.name);
  cfg.set_uuid(cfg_.uuid);

  for (const auto& [name, dev] : devices_) {
    if (dev->getType() != plantlistener::device::DeviceType::ADC) {
      continue;  // We only care about ADC's
    }

    auto* new_dev = cfg.add_devices();
    new_dev->set_name(name);

    auto port_count = dev->getPortCount();
    for (auto& sensor : sensors_) {
      if (sensor->getType() == SensorType::LIGHT) {
        port_count--;  // All lights must be at the end.
      }
    }
    new_dev->set_num_sensors(port_count);
  }

  Error res;
  std::thread plant_event_thread;
  do {
    res = {};

    plants_.clear();

    // Start GRPC client.
    spdlog::info("Connecting too {}:{}", cfg_.address, cfg_.port);

    // Init the data with the server
    client_ = std::move(makeClientFtn_(cfg_.address, cfg_.port, grpc::InsecureChannelCredentials()));
    
    grpc::ClientContext client_context;
    planttracker::grpc::InitializeResponse response;

    auto status = client_->initialize(&client_context, cfg, &response);
    if (!status.ok()) {
      res = {Error::Code::ERROR_NETWORKING, fmt::format("Failed to initialize with: {}", status.error_message())};
      break;
    } else if (response.res().return_code() != 0) {
      res = {Error::Code::ERROR_NETWORKING, fmt::format("Failed to initialize with: {}", response.res().error())};
      break;
    }

    // Adds all plants based on the return.
    for (const auto& plant : response.plants()) {
      PlantConfig plant_cfg;
      plant_cfg.id = plant.plant_id();
      plant_cfg.moisture_device_name = plant.device_name();
      plant_cfg.moisture_device_port = plant.sensor_port() - 1;  // Moisture sensors are offset by 1 in the db.

      res = addPlant(plant_cfg);
      if (res.isError()) {
        break;
      }
    }

    if (res.isError()) break;
    

    grpc::ClientContext event_thread_client_context;
    plant_event_thread = std::thread(&PlantListener::plantEventWorkLoop, this, std::ref(event_thread_client_context));

    spdlog::info("PlantListener started!");
    state_ = State::STARTED;

    // Poll until we are told to stop
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
      PlantSensorDataList plant_data_list;
      planttracker::grpc::Result report_res;
      for (const auto& plant : plants_) {
        const auto& data = plant->getPlantData();
        auto* plant_data = plant_data_list.add_data();

        plant_data->set_plant_id(plant->getId());
        plant_data->set_humidity(data.humidity_data);
        plant_data->set_temp(data.temp_data);

        float lumens = data.light_data * 1.023f;  // TODO replace a with a real coefficient.
        float moisture = 1 - (data.moisture_data / 255.0f);  // needs to be in a separate var due to rpi.
    
        LightSensorData* lightData = new LightSensorData;
        lightData->set_sensor_value(data.light_data);
        lightData->set_lumens(lumens);  

        MoistureSensorData* moistureData = new MoistureSensorData;
        moistureData->set_sensor_value(data.moisture_data);
        moistureData->set_moisture_level(moisture);
        spdlog::info("Moisture {}  sensor_value: {}", moisture, moistureData->sensor_value());

        plant_data->set_allocated_light(lightData);
        plant_data->set_allocated_moisture(moistureData);
      }

      {
        grpc::ClientContext client_context;
        auto status = client_->reportSensor(&client_context, plant_data_list, &report_res);
        if (!status.ok()) {
          res = {Error::Code::ERROR_NETWORKING, fmt::format("Failed to report sensor with: {}", status.error_message())};
          break;
        }
      }

      cv_.wait_until(lck, next_poll);
    }

    if (state_ == State::STARTED) {
      spdlog::warn("Connection error: {}", res.toStr());
    }

    // Shut down GRPC client, but the work loop must be joined before we shut down the client.
    lck.unlock();
    event_thread_client_context.TryCancel();
    plant_event_thread.join();
    plant_event_thread = {};
    client_.reset();
    lck.lock();

    // Check if we should try and reconnect
    if (cfg_.retry_count-- > 0 && state_ == State::STARTED) {
      if (res.isError()) {
        spdlog::warn("Connection error: {}", res.toStr());
      } else {
        spdlog::warn("Reconnect requested.");
      }
      std::this_thread::sleep_for(cfg_.retry_timeout);
      spdlog::info("Starting reconnection.");
      continue;
    }
  } while(false);

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

std::unique_ptr<PlantListener::ClientType> PlantListener::defaultClientMaker(
    const std::string& address, uint16_t port, const std::shared_ptr<grpc::ChannelCredentials>& cred) {
  std::shared_ptr<grpc::Channel> channel = grpc::CreateChannel(fmt::format("{}:{}", address, port), cred);
  return planttracker::grpc::PlantListener::NewStub(channel);
}

void PlantListener::plantEventWorkLoop(grpc::ClientContext& client_context) {
  std::unique_lock<std::mutex> lck(mutex_);
  spdlog::info("starting poll requests...");

  lck.unlock();
  planttracker::grpc::PollRequest req;
  req.set_uuid(cfg_.uuid);
  auto duplex = client_->poll(&client_context, req);
  spdlog::info("poll request started...");

  lck.lock();

  planttracker::grpc::ListenerRequest request;
  planttracker::grpc::Result res;

  bool done_reading = false;
  while (state_ == State::STARTED && !done_reading) {
    Error err;
    // First we need to get the request don't lock as this is a long poll
    lck.unlock();
    done_reading = !duplex->Read(&request);
    spdlog::debug("got request...");
    lck.lock();

    // Break out if we are done reading;
    if (done_reading) continue;

    // Now we need to do whatever the request wants.
    spdlog::info("Got request with type: {}, for plant: id={} dev_name={} dev_port={}",
                 planttracker::grpc::ListenerRequestType_Name(request.type()), request.plant().plant_id(),
                 request.plant().device_name(), request.plant().sensor_port());
    switch (request.type()) {
      case planttracker::grpc::ListenerRequestType::NEW_PLANT: {
        auto plant = request.plant();
        PlantConfig plant_cfg;
        plant_cfg.id = plant.plant_id();
        plant_cfg.moisture_device_name = plant.device_name();
        plant_cfg.moisture_device_port = plant.sensor_port();
        err = addPlant(plant_cfg);
        break;
      }
      case planttracker::grpc::ListenerRequestType::UPDATE_PLANT: {
        // An update is just a remove and readd with the new data because I am lazy.
        auto plant = request.plant();
        err = removePlant(plant.plant_id());
        if (err.isError()) break;

        PlantConfig plant_cfg;
        plant_cfg.id = plant.plant_id();
        plant_cfg.moisture_device_name = plant.device_name();
        plant_cfg.moisture_device_port = plant.sensor_port();
        err = addPlant(plant_cfg);
        break;
      }
      case planttracker::grpc::ListenerRequestType::DELETE_PLANT:
        err = removePlant(request.plant_id());
        break;
      case planttracker::grpc::ListenerRequestType::SHUTDOWN:
        spdlog::info("Shutdown Requested");
        done_reading = true;
        break;
      default:
        err = {Error::Code::ERROR_INVALID_ARG, fmt::format("Invalid request {}", static_cast<int>(request.type()))};
    }

    if (err.isError()) {
      spdlog::info("Request Failed with: {}", err.toStr());
    } else {
      spdlog::info("Request Success!");
    }
  }
  spdlog::debug("Exiting poll");

  // Change the state to reconnecting as our connection to the server is no longer valid.
  state_ = State::RECONNECTING;
  cv_.notify_all();
}
