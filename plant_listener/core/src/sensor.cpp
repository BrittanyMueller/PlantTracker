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

#include <fmt/format.h>

#include <plantlistener/sensor/sensor.hpp>

using plantlistener::Error;
using plantlistener::core::Sensor;

Sensor::Sensor(const SensorConfig& cfg, std::shared_ptr<plantlistener::device::Device> dev)
    : dev_(dev), dev_port_(cfg.device_port) {}

Error Sensor::addPlant(const std::shared_ptr<plantlistener::core::Plant>& plant) {
  auto itr = plants_.find(plant->getName());
  if (itr != plants_.end()) {
    return {Error::Code::ERROR_AGAIN, fmt::format("Plant {} already added to sensor {}", plant->getName(), id_)};
  }

  plants_.insert(std::make_pair(plant->getName(), plant));
  return {};
}

Error Sensor::removePlant(const std::string& plant_name) {
  auto itr = plants_.find(plant_name);
  if (itr == plants_.end()) {
    return {Error::Code::ERROR_MISSING, fmt::format("Plant {} wasn't found in sensor {}", plant_name, id_)};
  }
  plants_.erase(itr);
  return {};
}

Error Sensor::updatePlants() {
  auto data = dev_->readPort(dev_port_);
  for (const auto& plant : plants_) {
    updatePlant(plant.second, data);
  }
  return {};
}