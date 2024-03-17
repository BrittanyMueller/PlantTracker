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

#include <dlfcn.h>

#include <iostream>
#include <plantlistener/device/device_loader.hpp>

using plantlistener::core::DeviceLoader;
using plantlistener::device::Device;

DeviceLoader::DeviceLoader(const nlohmann::json& config) : config_(config) {}

DeviceLoader::~DeviceLoader() {
  for (const auto& lib : device_libs_) {
    dlclose(lib.second);
  }
}

std::vector<std::unique_ptr<Device>> DeviceLoader::getDevices() {
  std::vector<std::unique_ptr<Device>> devices;
  for (const auto& dev : config_) {
    auto name = dev.find("name");
    if (name == dev.end()) {
      std::cerr << "ERROR: Missing \"name\" in dev config" << std::endl;
      exit(-1);
    } else if (!name.value().is_string()) {
      std::cerr << "ERROR: Expected string for \"name\" in dev config" << std::endl;
      exit(-1);
    }
    auto ports = dev.find("ports");
    if (ports == dev.end()) {
      std::cerr << "ERROR: Missing \"ports\" in dev config" << std::endl;
      exit(-1);
    } else if (!ports.value().is_number_integer()) {
      std::cerr << "ERROR: Expected int for \"ports\" in dev config" << std::endl;
      exit(-1);
    }
    auto lib = dev.find("lib_name");
    if (lib == dev.end()) {
      std::cerr << "ERROR: Missing \"lib_name\" in dev config" << std::endl;
      exit(-1);
    } else if (!lib.value().is_string()) {
      std::cerr << "ERROR: Expected string for \"lib_name\" in dev config" << std::endl;
      exit(-1);
    }

    auto id = dev.find("device_id");
    if (id == dev.end()) {
      std::cerr << "ERROR: Missing \"device_id\" in dev config" << std::endl;
      exit(-1);
    } else if (!id.value().is_number_integer()) {
      std::cerr << "ERROR: Expected int for \"device_id\" in dev config" << std::endl;
      exit(-1);
    }

    void* handler = dlopen(lib.value().get<std::string>().c_str(), RTLD_LAZY);
    if (!handler) {
      std::cerr << "ERROR: Failed to open dev lib " << lib.value() << " with: " << dlerror() << std::endl;
      exit(-1);
    }
    createDeviceftn createDev = reinterpret_cast<createDeviceftn>(dlsym(handler, PLANTLISTENER_CREATE_DEVICE_NAME));
    if (!createDev) {
      std::cerr << "ERROR: Failed to get ftn from " << lib.value() << " with: " << dlerror() << std::endl;
      exit(-1);
    }
    device_libs_.emplace(std::make_pair(lib.value().get<std::string>(), handler));
    devices.emplace_back(createDev(dev, name->get<std::string>(), id->get<int64_t>(), ports->get<int32_t>()));
  }
  return devices;
}