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

#include <plantlistener/expected.hpp>
#include <string>
namespace plantlistener::core {

enum SensorType { LIGHT, MOISTURE, TEMP, HUMIDITY };
struct SensorConfig {
  SensorType type;
  std::string device_name{};
  int64_t device_port = 1;
  int64_t id = -1;  // ID for the associated plant. if it is -1 it's not for any given plant.
};

Expected<SensorType> strToSensorType(const std::string& dev_type);

}  // namespace plantlistener::core
