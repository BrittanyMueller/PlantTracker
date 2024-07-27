/*
 * (C) Copyright 2023-2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
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

#include <plantlistener/device/device.hpp>

#include "dht22.h"

namespace plantlistener::device {

class DeviceDHT22 : public Device {
 private:
  int handle_ = 0;
  struct DHT22 dev_{};

 public:
  DeviceDHT22(const nlohmann::json& json, const std::string& name, const DeviceType type, const uint8_t ports);
  ~DeviceDHT22();

  /**
   * Reads the value from a specific port. If the read fails for any reason -1
   * will be returned instead.
   *
   * @param port The port to be read.
   * @returns value of sensor between [min_value, max_value], or -1 on error.
   */
  uint64_t readPort(const uint8_t port) override;
};
}  // namespace plantlistener::device
