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

#include <nlohmann/json.hpp>
#include <fstream>
#include "plantlistener/sensor/sensor.hpp"
#include "plantlistener/device/device_loader.hpp"

using plantlistener::device::DeviceLoader;

int main() {
  std::ifstream config("./config.json");
  auto cfg = nlohmann::json::parse(config);

  DeviceLoader loader(cfg["devices"]);
  {
    auto devices = loader.getDevices();
    std::cout << "device count" << devices.size() << std::endl;
    std::cout << std::get<1>(devices[0]->getRange()) << std::endl;
    std::cout << "read " << devices[0]->readPort(2) << std::endl;
  }
  return 0;
}