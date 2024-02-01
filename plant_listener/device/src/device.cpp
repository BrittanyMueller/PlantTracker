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

#include <plantlistener/device/device.hpp>

using plantlistener::device::Device;

Device::Device(const std::string& name, const int64_t device_id, const uint8_t ports, uint64_t max_value,
               uint64_t min_value)
    : name_(name), id_(device_id), ports_(ports), max_value_(max_value), min_value_(min_value){};
