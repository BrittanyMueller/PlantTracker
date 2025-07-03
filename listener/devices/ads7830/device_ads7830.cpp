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

#include "device_ads7830.hpp"

using plantlistener::device::Device;
using plantlistener::device::DeviceADS7830;
using plantlistener::device::DeviceConfig;
using plantlistener::device::DeviceType;

DeviceADS7830::DeviceADS7830(const DeviceConfig& cfg) : Device(cfg) {}

double DeviceADS7830::readPort(const uint8_t port) { return static_cast<double>(dev_.read(port)); }

/**
 * Device loader function
 */
extern "C" std::shared_ptr<Device> createDevice(const DeviceConfig& cfg) {
  return std::shared_ptr<Device>(new DeviceADS7830(cfg));
}
