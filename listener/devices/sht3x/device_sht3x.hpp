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

#include "sht3x.hpp"

namespace plantlistener::device {

class DeviceSHT3X : public Device {
 private:
  int handle_ = 0;
  SHT3X dev_{};
  SHT3X::SHT3XData data_{};

  std::chrono::steady_clock::time_point lastRead_ = {};

 public:
  DeviceSHT3X(const DeviceConfig& cfg);

  /**
   * Reads the value from a specific port. If the read fails for any reason -1
   * will be returned instead.
   *
   * @param port The port to be read.
   * @returns value of sensor between [min_value, max_value], or -1 on error.
   */
  double readPort(const uint8_t port) override;
};
}  // namespace plantlistener::device
