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

#include <plantlistener/plant/plant_config.hpp>

namespace plantlistener::core {

class Plant {
 public:
  struct PlantData {
    int64_t light_data = -1;
    int64_t moisture_data = -1;
    double temp_data = -1;
    double humidity_data = -1;
  };

 private:
  const int64_t id_;
  PlantData data_{};

 public:
  Plant(const PlantConfig& config) : id_(config.id) {}

  inline const PlantData& getPlantData() const { return data_; }
  inline const int64_t getId() const { return id_; }

  inline void setLight(int64_t data) { data_.light_data = data; }
  inline void setMoisture(int64_t data) { data_.moisture_data = data; }
  inline void setHumidity(double data) { data_.humidity_data = data; }
  inline void setTemp(double data) { data_.temp_data = data; }
};
}  // namespace plantlistener::core