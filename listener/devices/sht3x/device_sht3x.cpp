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

#include "device_sht3x.hpp"

#include <cerrno>
#include <chrono>
#include <thread>

using plantlistener::device::Device;
using plantlistener::device::DeviceConfig;
using plantlistener::device::DeviceSHT3X;
using plantlistener::device::DeviceType;

DeviceSHT3X::DeviceSHT3X(const DeviceConfig& cfg) : Device(cfg) {}

double DeviceSHT3X::readPort(const uint8_t port) {
  auto now = std::chrono::steady_clock::now();
  if (lastRead_ > (now - std::chrono::seconds(1))) {
    lastRead_ = now;
    data_ = dev_.read();
  }

  if (port == HUMIDITY_PORT) {
    return data_.humidity;
  } else if (port == TEMP_PORT) {
    return data_.temp;
  } else {
    return -1;
  }
}

/**
 * Device loader function
 */
extern "C" std::shared_ptr<Device> createDevice(const DeviceConfig& cfg) {
  return std::shared_ptr<Device>(new DeviceSHT3X(cfg));
}
