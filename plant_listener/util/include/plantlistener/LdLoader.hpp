/*
 * (C) Copyright 2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
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

#include <fmt/format.h>

#include <nlohmann/json.hpp>
#include <plantlistener/expected.hpp>
#include <string>
#include <unordered_map>
#include <vector>

namespace plantlistner {

template <typename U>
class LdLoader {
 public:
  using LdLoaderHandler = void*;

 private:
  std::unordered_map<std::string, LdLoaderHandler> loaded_libs_{};
  std::string access_ftn_{};

 public:
  LdLoader(const std::string& access_ftn) : access_ftn_(access_ftn) {}
  virtual ~LdLoader() {
    for (const auto& lib : loaded_libs_) {
      dlclose(lib.second);
    }
  }

  expected<U> getHandler(const std::string& name, const std::string& path) {
    void* handler = nullptr;

    // Check if this lib is already loaded
    auto& lib_itr = loaded_libs_.find(name);
    if (lib_itr == loaded_libs_.end()) {
      handler = dlopen(lib.value().get<std::string>().c_str(), RTLD_LAZY);
      if (!handler) {
        return {Error::Code::ERROR_IO, fmt::format("Failed to open {} with: {}", path, dlerror())};
      }
      loaded_libs_.emplace(std::make_pair(name.get<std::string>(), handler));
    } else {
      handler = lib_itr.second;
    }

    // Grab the function from the loaded lib.
    U create_ftn = reinterpret_cast<U>(dlsym(handler, access_ftn_.c_str()));
    if (!create_ftn) {
      return {Error::Code::ERROR_MISSING,
              fmt::format("Failed to get {} from {} with: {}", access_ftn_, path, dlerror())};
    }
    return {create_ftn};
  }

  virtual nlohmann::json dump() { nlohmann::json d = nlohmann::json::object(); }
};
}  // namespace plantlistner