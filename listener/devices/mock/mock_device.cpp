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

#include "mock_device.hpp"

#include <memory>
#include <nlohmann/json.hpp>

using plantlistener::device::Device;
using plantlistener::device::DeviceConfig;
using plantlistener::device::DeviceType;
using plantlistener::device::MockDevice;

MockDevice::MockDevice(const DeviceConfig& cfg) : Device(cfg) {}

double MockDevice::readPort(const uint8_t port) {
  if (port < 0 || port >= ports_) return -1;

  if (use_rand_value_) {
    return rand() % max_value_;
  } else {
    return fake_value_;
  }
}

/**
 * Device loader function
 */
extern "C" {
std::shared_ptr<Device> createDevice(const DeviceConfig& cfg) { return std::shared_ptr<Device>(new MockDevice(cfg)); }
}
