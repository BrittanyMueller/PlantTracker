/*
 * (C) Copyright 2023 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
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

#include <memory>
#include <nlohmann/json.hpp>
#include <plantlistener/device/device.hpp>
#include <plantlistener/device/device_config.hpp>
#include <plantlistener/ld_loader.hpp>
#include <string>
#include <vector>

namespace plantlistener::core {

class DeviceLoader : protected LdLoader<createDeviceftn> {
 private:
  nlohmann::json config_;

 public:
  DeviceLoader();

  Expected<std::shared_ptr<plantlistener::device::Device>> createDevice(const device::DeviceConfig& cfg);

  nlohmann::json dump();
};
}  // namespace plantlistener::core
