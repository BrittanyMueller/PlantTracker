/*
 * (C) Copyright 2023 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */
#pragma once

#include <memory>
#include <mutex>
#include <plantlistener/device/device.hpp>
#include <plantlistener/device/device_loader.hpp>
#include <plantlistener/error.hpp>
#include <plantlistener/plant/plant.hpp>
#include <plantlistener/plant_listener_config.hpp>
#include <plantlistener/sensor/sensor.hpp>
#include <unordered_map>
#include <vector>
#include <condition_variable>

namespace plantlistener::core {

/**
 * PlantListener is the main work loop of the application and will be responsible for handing all incoming requires
 * as well as supplying data to the PlantTracker server.
 *
 * The work loop will all work around sensors which will be responsible getting data from devices, and updating the
 * plant data so it can be sent out.
 */
class PlantListener {
 private:
  enum class State { NOT_INITALIZED, INITIALIZING, INITALIZED, STARTED, STOPPING };

  PlantListenerConfig cfg_{};
  State state_{State::NOT_INITALIZED};

  std::vector<std::unique_ptr<Sensor>> sensors_{};
  std::vector<std::shared_ptr<Plant>> plants_{};

  // map of "dev_name" -> device
  std::unordered_map<std::string, std::shared_ptr<plantlistener::device::Device>> devices_{};
  std::unique_ptr<DeviceLoader> device_loader_{};

  std::mutex mutex_{};
  std::condition_variable cv_{};

 public:
  PlantListener(PlantListenerConfig&& cfg);
  ~PlantListener();

  // Disable due to this being passed to work thread.
  PlantListener(const PlantListener&) = delete;
  PlantListener(PlantListener&&) = delete;
  PlantListener& operator=(const PlantListener&) = delete;
  PlantListener& operator=(PlantListener&&) = delete;

  /**
   * Loads configuration and initializes the application meaning
   *    1. Devices are initialized based on device config.
   *    2. Sensors are created and mapped to devices.
   *    3. Plants are created and added as consumers of required Sensors.
   */
  Error init();

  /**
   * Starts running the application. This won't return until stop is called.
   *
   * The following will be done when start is called.
   * 1. Starts the connection between PlantTracker and this PlantListener.
   * 2. sends Init command to become in sync with the database
   * 3. loops through the sensor based on configuration and sends data to the PlantTracker server
   *
   * @returns Error The exit code of the server
   *    OK stop was called and it exitted cleanly
   *    TODO res of error codes
   */
  Error start();

  /**
   * Stops the application. This won't return until the application could be stopped.
   *
   * @returns Error if the operation was successful.
   *    OK PlantListener was stopped.
   */
  Error stop();

 private:
  friend class PlantListenerTester;
  Error addSensor(const SensorConfig& cfg);
  Error addPlant(const PlantConfig& cfg);

};
}  // namespace plantlistener::core