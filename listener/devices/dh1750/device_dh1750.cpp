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

#include "device_dh1750.hpp"

using plantlistener::device::Device;
using plantlistener::device::DeviceConfig;
using plantlistener::device::DeviceDH1750;
using plantlistener::device::DeviceType;

DeviceDH1750::DeviceDH1750(const DeviceConfig& cfg) : Device(cfg) {}

double DeviceDH1750::readPort(const uint8_t port) { return static_cast<double>(dev_.read()); }

/**
 * Device loader function
 */
extern "C" std::shared_ptr<Device> createDevice(const DeviceConfig& cfg) {
  return std::shared_ptr<Device>(new DeviceDH1750(cfg));
}
