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
#pragma once

#include <nlohmann/json.hpp>
#include <string>

namespace plantlistener::device {

enum class DeviceTypes { ADC, TEMPATURE_AND_HUMIDITY, PH_LEVEL };

// since only ADC support multiple ports instead hardcode
// port configuration for other device types.
#define TEMP_PORT 0;
#define HUMIDITY_PORT 1;

#define PH_PORT 0;

struct DeviceConfig {
  std::string name;
  DeviceTypes type;
  std::string lib;
  int64_t device_id;
  uint32_t ports;
  nlohmann::json cfg;
};

}  // namespace plantlistener::device
