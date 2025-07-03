/*
 * (C) Copyright 2023 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */

#include <plantlistener/device/device.hpp>

using plantlistener::device::Device;

Device::Device(const DeviceConfig& cfg)
    : name_(cfg.name), type_(cfg.type), ports_(cfg.ports), max_value_(cfg.max_value), min_value_(cfg.min_value){};

nlohmann::json Device::dump() {
  nlohmann::json dev = nlohmann::json::object();

  dev["name"] = name_;
  dev["ports"] = ports_;
  dev["max"] = max_value_;
  dev["min"] = min_value_;
  return dev;
}
