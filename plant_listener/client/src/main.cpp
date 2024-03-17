/*
 * (C) Copyright 2023-2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */

#include <spdlog/spdlog.h>

#include <fstream>
#include <nlohmann/json.hpp>
#include <plantlistener/device/device_loader.hpp>
#include <plantlistener/error.hpp>
#include <plantlistener/expected.hpp>
#include <plantlistener/sensor/sensor.hpp>

#include "command_line.hpp"

using plantlistener::core::DeviceLoader;

int main(int argc, char* argv[]) {
  auto config = plantlistener::client::parseArguments(argc, argv);

  plantlistener::Error err(plantlistener::Error::Code::ERROR_MISSING, "Missing stuff");

  plantlistener::Expected<int> val(10);
  plantlistener::Expected<int> val2(plantlistener::Error::Code::ERROR_INTERNAL, "foo");

  spdlog::error("{}", err.toStr());

  return -1;
  // std::ifstream config("./config.json");
  // auto cfg = nlohmann::json::parse(config);

  // DeviceLoader dev_loader(cfg["devices"]);
  // PlantLoader sen_loader(cfg["sensors"]);

  // {
  //   auto devices = dev_loader.getDevices();
  //   std::cout << "device count" << devices.size() << std::endl;
  //   std::cout << std::get<1>(devices[0]->getRange()) << std::endl;
  //   std::cout << "read " << devices[0]->readPort(2) << std::endl;
  // }
  return 0;
}