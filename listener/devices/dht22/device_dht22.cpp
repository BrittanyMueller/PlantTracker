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

#include <spdlog/spdlog.h>

#include <cerrno>
#include <chrono>
#include <thread>

using plantlistener::device::Device;
using plantlistener::device::DeviceConfig;
using plantlistener::device::DeviceDHT22;
using plantlistener::device::DeviceType;

DeviceDHT22::DeviceDHT22(const DeviceConfig& cfg) : Device(cfg) {
  // TODO add check that this exists
  dev_ = init_dht22(cfg.cfg["pin"].get<int>());
}

DeviceDHT22::~DeviceDHT22() { free_dht22(&dev_); }

double DeviceDHT22::readPort(const uint8_t port) {
  for (int i = 0; i < 5; i++) {
    DHT22Data data = read_dht22(&dev_);
    if (data.err == 0) {
      humidity_ = static_cast<double>(data.humidity);
      temp_ = static_cast<double>(data.temp);
      break;
    }
    if (i < 4) {
      // spdlog::warn("Failed to read dht22 port retry {}", i);
      std::this_thread::sleep_for(std::chrono::seconds(3));
    }
  }

  if (port == HUMIDITY_PORT) {
    return humidity_;
  } else if (port == TEMP_PORT) {
    return temp_;
  } else {
    return -1;
  }
}

/**
 * Device loader function
 */
extern "C" std::shared_ptr<Device> createDevice(const DeviceConfig& cfg) {
  return std::shared_ptr<Device>(new DeviceDHT22(cfg));
}
