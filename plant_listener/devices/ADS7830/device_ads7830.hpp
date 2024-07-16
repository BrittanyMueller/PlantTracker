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

#include <pigpio.h>

namespace plantlistener::device {

class ADS7830 {
 private:
  int handle_ = 0;

 public:
  ADS7830(uint8_t bus = 0, uint8_t address = 0b10010);
  ~ADS7830();

  inline bool valid() const { return handle_ <= 0; }

  uint64_t readPort(const uint8_t port) override;
};
}  // namespace plantlistener::device
