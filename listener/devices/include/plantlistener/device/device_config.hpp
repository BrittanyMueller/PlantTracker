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
#pragma once

#include <nlohmann/json.hpp>
#include <plantlistener/expected.hpp>
#include <string>

namespace plantlistener::device {

enum class DeviceType { ADC, TEMP_AND_HUMIDITY, PH_LEVEL, LIGHT };

// since only ADC support multiple ports instead hardcode
// port configuration for other device types.
#define TEMP_PORT 0
#define HUMIDITY_PORT 1

#define LIGHT_PORT 0

#define PH_PORT 0

struct DeviceConfig {
  std::string name;
  DeviceType type;
  std::string lib;
  uint32_t ports;
  int64_t min_value = 0;
  int64_t max_value = 256;
  nlohmann::json cfg;
};

Expected<DeviceType> strToDeviceType(const std::string& dev_type);

}  // namespace plantlistener::device
