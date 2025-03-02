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

#include "mcp3008.hpp"

#include <limits.h>
#include <pigpio.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>

#include <iostream>

// This code is based on my RPI pico mcp3008 lib.

MCP3008::MCP3008(uint8_t bus, unsigned baud) {
  if (gpioInitialise() < 0) {
    std::cout << "Failed to init gpio" << std::endl;
    return;
  }

  handle_ = spiOpen(bus, baud, 0);
  if (handle_ < 0) {
    std::cout << "Failed to get handle" << std::endl;
    return;
  }
}

MCP3008::~MCP3008() { spiClose(handle_); }

uint32_t MCP3008::read(uint8_t pin) const {
  if (pin < 0 || pin >= 8) {
    return -1;
  }

  // MCP3008 requires a 3-byte transfer
  uint8_t buf_in[3] = {0, 0, 0};
  uint8_t buf_out[3] = {0, 0, 0};
  buf_out[0] = 0b00000001;
  buf_out[1] = 0b10000000 | (pin << static_cast<uint8_t>(4));
  // SPI transfer
  int res = spiXfer(handle_, (char*)buf_out, (char*)buf_in, 3);
  if (res < 0) {
    std::cerr << "SPI transfer failed" << std::endl;
    return -1;
  }

  uint32_t result = (buf_in[0] & 0x01) << 9;
  result |= buf_in[1] << 8;
  result |= buf_in[2];
  return result;
}
