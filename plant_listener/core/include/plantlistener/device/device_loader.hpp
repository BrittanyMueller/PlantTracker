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
#include <string>
#include <vector>

namespace plantlistener::core {

class DeviceLoader {
 private:
  struct DeviceLoaderHandler {
    std::string lib_name;
    void* lib_handler;
  };

  nlohmann::json config_;
  std::unordered_map<std::string, void*> device_libs_;

 public:
  DeviceLoader(const nlohmann::json& config);
  ~DeviceLoader();

  /**
   * Gets the device from the loaded device config
   */
  std::vector<std::unique_ptr<device::Device>> getDevices();

  nlohmann::json dump();
};
}  // namespace plantlistener::core