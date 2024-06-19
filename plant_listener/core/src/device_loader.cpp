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

#include <iostream>
#include <plantlistener/device/device_loader.hpp>

using plantlistener::core::DeviceLoader;
using plantlistener::device::Device;
using plantlistener::device::DeviceConfig;
using plantlistener::device::DeviceType;

DeviceLoader::DeviceLoader() : LdLoader<createDeviceftn>(PLANTLISTENER_CREATE_DEVICE_NAME) {}

plantlistener::Expected<std::shared_ptr<Device>> DeviceLoader::createDevice(const DeviceConfig& cfg) {
  auto ftn_res = getHandler(cfg.name, cfg.lib);
  if (ftn_res.isError()) {
    return ftn_res;
  }

  return {(*ftn_res)(cfg.cfg, cfg.name, cfg.type, cfg.ports)};
}

nlohmann::json DeviceLoader::dump() {
  nlohmann::json j;
  return j;
}
