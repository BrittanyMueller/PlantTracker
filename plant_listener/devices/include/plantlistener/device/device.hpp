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

#include <cstdint>
#include <memory>
#include <nlohmann/json.hpp>
#include <string>
#include <tuple>

#include "device_config.hpp"

namespace plantlistener::device {

class Device {
 protected:
  std::string name_;
  DeviceType type_;
  uint8_t ports_;
  uint64_t max_value_;
  uint64_t min_value_;

 public:
  /**
   * An device that abstracts away hardware devices that are used to read
   * sensors i.e., an ADC.
   *
   * @param name Human readable name of device
   * @param type The type of device.
   * @param max_value The max value expected out of this device.
   * @param min_value The min value expected out of this device. This should
   * normally be left as 0.
   */
  Device(const std::string& name, const DeviceType type, const uint8_t ports, uint64_t max_value = 255,
         uint64_t min_value = 0);
  virtual ~Device(){};

  inline const std::string& getName() const { return name_; }
  inline std::tuple<uint64_t, uint64_t> getRange() const { return {min_value_, max_value_}; }
  inline uint8_t getPortCount() const { return ports_; }
  inline const DeviceType getType() const { return type_; }

  /**
   * Reads the value from a specfic port. If the read fails for any reason -1
   * will be returned instead.
   *
   * @param port The port to be read.
   * @returns value of sensor between [min_value, max_value], or -1 on error.
   */
  virtual double readPort(const uint8_t port) = 0;

  virtual nlohmann::json dump();
};

}  // namespace plantlistener::device

extern "C" {
typedef std::shared_ptr<plantlistener::device::Device> (*createDeviceftn)(const nlohmann::json&,
                                                                          const std::string& name,
                                                                          const plantlistener::device::DeviceType type,
                                                                          const uint8_t ports);
}
#define PLANTLISTENER_CREATE_DEVICE_NAME "createDevice"
