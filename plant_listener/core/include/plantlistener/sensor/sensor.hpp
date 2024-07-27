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
#pragma once

#include <iostream>
#include <memory>
#include <plantlistener/device/device.hpp>
#include <plantlistener/error.hpp>
#include <plantlistener/plant/plant.hpp>
#include <plantlistener/sensor/sensor_config.hpp>
#include <unordered_map>

namespace plantlistener::core {

class Sensor {
 private:
  // ID is sensor specific and set to -1 if it has no meaning.
  int64_t id_;
  std::shared_ptr<plantlistener::device::Device> dev_;
  uint64_t dev_port_;
  SensorType type_;

  // Holds the result of the constructor.
  Error ctr_error{};

 protected:
  // Holds map of plant_id to plant.
  std::unordered_map<int64_t, std::shared_ptr<Plant>> plants_;

 public:
  Sensor(const SensorConfig& cfg, std::shared_ptr<plantlistener::device::Device> dev);

  inline Error valid() const { return ctr_error; };
  inline const int64_t getId() const { return id_; }
  inline const SensorType getType() const { return type_; }

  Error addPlant(const std::shared_ptr<Plant>& plant);
  Error removePlant(const int64_t plant_id);
  inline bool hasPlant(const int64_t plant_id) { return plants_.find(plant_id) != plants_.end(); };

  /**
   * Updates all of the plants with the data from the sensors
   */
  Error updatePlants();

 protected:
  // Function used to update the correct plant field.
  virtual void updatePlant(const std::shared_ptr<Plant>& plant, double data) = 0;
};

}  // namespace plantlistener::core
