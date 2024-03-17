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

#include <plantlistener/device/device.hpp>

namespace plantlistener::device {

class MockDevice : public Device {
 public:
  bool use_rand_value_ = true;
  uint64_t fake_value_ = 128;

  MockDevice(const std::string& name = "mock_device", const int64_t device_id = 0, const uint8_t ports = 8);
  uint64_t readPort(const uint8_t port) override;
};
}  // namespace plantlistener::device