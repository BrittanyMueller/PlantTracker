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
#include <nlohmann/json.hpp>
#include <optional>
#include <plantlistener/device/device_config.hpp>
#include <plantlistener/error.hpp>
#include <plantlistener/plant/plant_config.hpp>
#include <plantlistener/sensor/sensor_config.hpp>
#include <string>
#include <vector>
#include <uuid/uuid.h>

namespace plantlistener::core {

class PlantListenerConfig {
 public:
  class ParseException : public std::exception {
    Error err_;

   public:
    ParseException(const plantlistener::Error::Code code, const std::string& msg) : err_(code, msg) {}
    const Error& getError() const { return err_; }
  };

 public:
  std::string name{};
  std::string config_path{};
  std::string uuid{};

  spdlog::level::level_enum log_level = spdlog::level::warn;
  std::string address = "127.0.0.1";
  uint16_t port = 5051;
  int32_t retry_count = -1;  // TODO(implement retry and time outs)
  std::chrono::seconds retry_timeout = std::chrono::seconds(30);
  std::chrono::seconds poll_rate = std::chrono::seconds(10);

  std::vector<plantlistener::device::DeviceConfig> devices = {};
  std::vector<SensorConfig> sensors = {};

  PlantListenerConfig() = default;

  /**
   * Loads config using config_path.
   */
  Error load();

  Error setUUID(uuid_t uuid);

 private:
  friend class PlantListenerConfigTester;

  static SensorConfig parseSensor(const nlohmann::json& j);

  template <typename T>
  static void parseValue(const nlohmann::json& j, const std::string& key, T& value, bool optional = false) {
    auto it = j.find(key);
    if (it == j.end()) {
      if (optional) return; // optional ignore missing.
      throw ParseException(Error::Code::ERROR_NOT_FOUND, fmt::format("Missing key {}", key));
    }

    try {
      value = it->get<T>();
    } catch (const nlohmann::json::type_error& e) {
      throw ParseException(Error::Code::ERROR_INVALID_TYPE,
                           std::format("Failed to parse key {} with: {}", key, e.what()));
    }
  }
};
}  // namespace plantlistener::core
