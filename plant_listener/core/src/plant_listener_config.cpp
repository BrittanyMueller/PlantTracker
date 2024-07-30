/*
 * (C) Copyright 2023 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */
#include <fstream>
#include <nlohmann/json.hpp>
#include <plantlistener/device/device_config.hpp>
#include <plantlistener/expected.hpp>
#include <plantlistener/plant_listener_config.hpp>
#include <unordered_map>
#include <utility>

using plantlistener::Error;
using plantlistener::Expected;
using plantlistener::core::PlantListenerConfig;
using plantlistener::core::SensorConfig;
using plantlistener::core::SensorType;
using plantlistener::device::DeviceConfig;

using nlohmann::json;

SensorConfig PlantListenerConfig::parseSensor(const json& j) {
  SensorConfig sensor_cfg;

  if (!j.is_object()) {
    throw ParseException(Error::Code::ERROR_INVALID_TYPE, "Expected Object when parsing sensor");
  }

  std::string sensor_type_str;
  parseValue<std::string>(j, "type", sensor_type_str);
  parseValue<std::string>(j, "device", sensor_cfg.device_name);

  auto type_res = plantlistener::core::strToSensorType(sensor_type_str);
  if (type_res.isError()) {
    throw ParseException(type_res.code, type_res.msg);
  }
  sensor_cfg.type = type_res.getValue();

  // Depending on the type it might have a pre-configured port.
  switch (sensor_cfg.type) {
    case SensorType::HUMIDITY:
      sensor_cfg.device_port = HUMIDITY_PORT;
      break;
    case SensorType::TEMP:
      sensor_cfg.device_port = TEMP_PORT;
      break;
    default:
      parseValue<int64_t>(j, "port", sensor_cfg.device_port);
  }

  return sensor_cfg;
}

Error PlantListenerConfig::load() {
  json cfg;

  auto config_fp = std::ifstream(config_path);
  if (!config_fp.is_open()) {
    return {Error::Code::ERROR_NOT_FOUND, fmt::format("Failed to open config {}", config_path)};
  }

  try {
    cfg = json::parse(config_fp);
  } catch (const json::parse_error& err) {
    return {Error::Code::ERROR_IO, fmt::format("Failed to parse config with: {}", err.what())};
  }

  Error res;
  try {
    parseValue<std::string>(cfg, "name", name);

    // Parse server
    auto server_it = cfg.find("server");
    if (server_it == cfg.end()) {
      throw ParseException(Error::Code::ERROR_NOT_FOUND, "Missing key server");
    }
    parseValue<std::string>(*server_it, "address", address);
    parseValue<std::string>(*server_it, "uuid", uuid, true);
    parseValue<uint16_t>(*server_it, "port", port);
    parseValue<int32_t>(*server_it, "retry_count", retry_count);

    int32_t timeout;
    parseValue<int32_t>(*server_it, "retry_timeout", timeout);
    retry_timeout = std::chrono::seconds(timeout);

    // TODO(qawse3dr) this should be a helper function
    std::string log_level_str;
    parseValue<std::string>(*server_it, "log_level", log_level_str);
    if (strcmp(log_level_str.c_str(), "ERR") == 0) {
      log_level = spdlog::level::err;
    } else if (strcmp(log_level_str.c_str(), "WARN") == 0) {
      log_level = spdlog::level::warn;
    } else if (strcmp(log_level_str.c_str(), "STAT") == 0) {
      log_level = spdlog::level::info;
    } else if (strcmp(log_level_str.c_str(), "INFO") == 0) {
      log_level = spdlog::level::info;
    } else if (strcmp(log_level_str.c_str(), "DBG") == 0) {
      log_level = spdlog::level::debug;
    } else {
      return {Error::Code::ERROR_INVALID_VALUE, "Invalid log level must be one of (ERR, WARN, STAT, INFO, DBG)"};
    }
    spdlog::set_level(log_level);

    // Parse light sensor
    auto sensor_it = cfg.find("sensors");
    if (sensor_it == cfg.end() || !sensor_it->is_array()) {
      return {Error::Code::ERROR_NOT_FOUND, "Missing key sensor or is not array"};
    }
    for (auto& sensor : *sensor_it) {
      sensors.emplace_back(parseSensor(sensor));
      spdlog::debug("Found sensor {} in config", sensors.back().device_name);
    }

    // Parse devices TODO parse safer
    std::unordered_map<std::string, std::string> libs;
    for (const auto& dev : cfg["devices"]["libs"]) {
      std::string lib_name, lib_path;
      parseValue<std::string>(dev, "name", lib_name);
      parseValue<std::string>(dev, "lib_path", lib_path);
      libs.emplace(std::make_pair(lib_name, lib_path));
      spdlog::debug("Found dev_lib {}={} in config", lib_name, lib_path);
    }

    for (const auto& dev : cfg["devices"]["instances"]) {
      DeviceConfig dev_cfg;
      std::string lib_name, dev_type;

      dev_cfg.cfg = dev["cfg"];
      parseValue<std::string>(dev, "name", dev_cfg.name);
      parseValue<uint32_t>(dev, "ports", dev_cfg.ports);

      parseValue<std::string>(dev, "lib", lib_name);
      parseValue<std::string>(dev, "type", dev_type);

      dev_cfg.lib = libs[lib_name];  // todo verify the lib was found.

      auto dev_res = plantlistener::device::strToDeviceType(dev_type);
      if (dev_res.isError()) {
        return dev_res;
      }
      dev_cfg.type = dev_res.getValue();

      spdlog::debug("Found dev {} in config", dev_cfg.name);
      devices.emplace_back(std::move(dev_cfg));
    }
  } catch (const ParseException& e) {
    return e.getError();
  }

  return {};
}

Error PlantListenerConfig::setUUID(uuid_t uu) {
  uuid.resize(UUID_STR_LEN-1); // minus 1 as c++ strings will account for the null term
  uuid_unparse(uu, uuid.data());
  spdlog::info("Generated UUID: {}", uuid);
  
  // Now save it to the config.
  nlohmann::ordered_json cfg;

  {
    auto config_fp = std::ifstream(config_path);
    if (!config_fp.is_open()) {
      return {Error::Code::ERROR_NOT_FOUND, fmt::format("Failed to open config {}", config_path)};
    }

    try {
      cfg = nlohmann::ordered_json::parse(config_fp);
    } catch (const json::parse_error& err) {
      return {Error::Code::ERROR_IO, fmt::format("Failed to parse config with: {}", err.what())};
    }
  }

  {
    // Set the uuid and write it back to the file
    cfg.at("server")["uuid"] = uuid;
    auto config_fp = std::ofstream(config_path);
    if (!config_fp.is_open()) {
      return {Error::Code::ERROR_NOT_FOUND, fmt::format("Failed to open config {}", config_path)};
    }
    config_fp << cfg.dump(4) << std::endl;
  }

  return {};
}
