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

#include <plantlistener/sensor/sensor.hpp>
namespace plantlistener::core {

class HumiditySensor : public Sensor {
 public:
  HumiditySensor(const SensorConfig& cfg, std::shared_ptr<plantlistener::device::Device> dev) : Sensor(cfg, dev){};

  void updatePlant(const std::shared_ptr<Plant>& plant, double data) override { plant->setHumidity(data); }
};
}  // namespace plantlistener::core
