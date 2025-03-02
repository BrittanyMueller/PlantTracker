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
#pragma once

#include <dlfcn.h>
#include <fmt/format.h>

#include <filesystem>
#include <nlohmann/json.hpp>
#include <plantlistener/expected.hpp>
#include <string>
#include <unordered_map>
#include <vector>

namespace plantlistener {

template <typename U>
class LdLoader {
 public:
  using LdLoaderHandler = void*;

  struct LibInfo {
    LdLoaderHandler handler;
    std::string path;
  };

 private:
  std::unordered_map<std::string, LibInfo> loaded_libs_{};
  std::string access_ftn_{};

 public:
  LdLoader(const std::string& access_ftn) : access_ftn_(access_ftn) {}
  virtual ~LdLoader() {
    for (const auto& lib : loaded_libs_) {
      dlclose(lib.second.handler);
    }
  }

  Expected<U> getHandler(const std::string& name, const std::string& path) {
    void* handler = nullptr;

    // Check if this lib is already loaded
    auto lib_itr = loaded_libs_.find(name);
    if (lib_itr == loaded_libs_.end()) {
      if (!std::filesystem::exists(path)) {
        return {Error::Code::ERROR_NOT_FOUND, fmt::format("Library could not be found at {}.", path)};
      }

      handler = dlopen(path.c_str(), RTLD_LAZY);
      if (!handler) {
        return {Error::Code::ERROR_IO, fmt::format("Failed to open {} with: {}", path, dlerror())};
      }
      loaded_libs_.emplace(std::make_pair(name, LibInfo{handler, path}));
    } else {
      if (!std::filesystem::equivalent(std::filesystem::path(path), std::filesystem::path(lib_itr->second.path))) {
        return {Error::Code::ERROR_AGAIN,
                fmt::format("Library already defined with name {}, path {}", name, lib_itr->second.path)};
      }
      handler = lib_itr->second.handler;
    }

    // Grab the function from the loaded lib.
    U create_ftn = reinterpret_cast<U>(dlsym(handler, access_ftn_.c_str()));
    if (!create_ftn) {
      return {Error::Code::ERROR_MISSING,
              fmt::format("Failed to get {} from {} with: {}", access_ftn_, path, dlerror())};
    }
    return {create_ftn};
  }

  virtual nlohmann::json dump() {
    nlohmann::json d = nlohmann::json::array();
    for (const auto& lib : loaded_libs_) {
      nlohmann::json l = nlohmann::json::object();
      l["lib_name"] = lib.first;
      l["lib_path"] = lib.second.path;
      d.push_back(l);
    }
    return d;
  }
};
}  // namespace plantlistener
