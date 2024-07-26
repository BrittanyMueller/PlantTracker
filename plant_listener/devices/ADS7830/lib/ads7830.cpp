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

#include "ads7830.hpp"

#include <limits.h>
#include <pigpio.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>

#include <iostream>

ADS7830::ADS7830(uint8_t bus, uint8_t address) {
  if (gpioInitialise() < 0) {
    std::cout << "Failed to init gpio" << std::endl;
    return;
  }

  handle_ = i2cOpen(bus, address, 0);
  if (handle_ < 0) {
    std::cout << "Failed to get handle" << std::endl;
    return;
  }

  // Write a 0 byte to make sure it works
  auto res = i2cWriteByte(handle_, 0);
  if (res == PI_BAD_HANDLE) {
    std::cout << "Failed to write BAD_HANDLE" << std::endl;
    handle_ = -1;
    return;
  } else if (res == PI_BAD_PARAM) {
    std::cout << "Failed to write BAD_PARAM" << std::endl;
    handle_ = -1;
    return;
  } else if (res == PI_I2C_WRITE_FAILED) {
    std::cout << "Failed to write BAD_WRITE_FAILED" << std::endl;
    handle_ = -1;
    return;
  }
}

ADS7830::~ADS7830() { i2cClose(handle_); }

uint8_t ADS7830::read(uint8_t pin) const {
  if (pin < 0 || pin >= 8) {
    return -1;
  }

  // clang-format off
  // 2,3,4 are the channel selection from 0-7
  // The math ain't working out chief IDK what they were thinking with this selection mapping...
  // maybe they to big brain for me so here is a static array to map the key to the right value
  static int selection_mapping[] = {
    0b000,
    0b100,
    0b001,
    0b101,
    0b010,
    0b110,
    0b011,
    0b111
  };
  // clang-format on

  // Command has the first bit set to 1 to be in single ended mode
  // 3,4 last bit set to 01 to turn on A/D converter ON
  int base_cmd = 0b10000100;
  i2cWriteByte(handle_, base_cmd | (selection_mapping[pin] << 4));
  return i2cReadByte(handle_);
}
