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

#include <spdlog/spdlog.h>

#include <chrono>
#include <optional>
#include <plantlistener/device/device_config.hpp>
#include <plantlistener/error.hpp>
#include <plantlistener/plant/plant_config.hpp>
#include <plantlistener/sensor/sensor_config.hpp>
#include <string>
#include <vector>

namespace plantlistener::core {

class PlantListenerConfig {
 public:
  std::string name{};
  std::string config_path{};

  spdlog::level::level_enum log_level = spdlog::level::warn;
  std::string address = "127.0.0.1";
  uint16_t port = 1234;
  int32_t retry_count = -1;
  std::chrono::seconds retry_timeout = std::chrono::seconds(30);
  std::chrono::seconds poll_rate = std::chrono::seconds(10);

  std::vector<plantlistener::device::DeviceConfig> devices = {};
  std::vector<SensorConfig> sensors = {};

  PlantListenerConfig() {}

  /**
   * Loads config using config_path.
   */
  Error load();

};
}  // namespace plantlistener::core