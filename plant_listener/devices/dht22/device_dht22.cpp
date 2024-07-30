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

#include "device_dht22.hpp"

using plantlistener::device::Device;
using plantlistener::device::DeviceDHT22;
using plantlistener::device::DeviceType;

DeviceDHT22::DeviceDHT22(const nlohmann::json& j, const std::string& name, const DeviceType type,
                             const uint8_t ports)
    : Device(name, type, ports) {
      // TODO add check that this exists
      dev_ = init_dht22(j["pin"].get<int>());
    }

DeviceDHT22::~DeviceDHT22() {
  free_dht22(&dev_);
}


double DeviceDHT22::readPort(const uint8_t port) {
  DHT22Data data = read_dht22(&dev_);
  if (data.err == 0) {
    humidity = static_cast<double>(data.humidity);
    temp = static_cast<double>(data.temp);
  }

  if (port == HUMIDITY_PORT) {
    return humidity;
  } else if (port == TEMP_PORT) {
    return temp;
  } else {
    return -1;
  }
}

/**
 * Device loader function
 */
extern "C" std::shared_ptr<Device> createDevice(const nlohmann::json& j, const std::string& name, const DeviceType type,
                                                const uint8_t ports) {
  return std::shared_ptr<Device>(new DeviceDHT22(j, name, type, ports));
}
