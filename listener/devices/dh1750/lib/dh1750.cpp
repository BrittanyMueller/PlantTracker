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

#include "dh1750.hpp"

#include <limits.h>
#include <pigpio.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>

#include <iostream>

static const uint8_t CONTINUOUS_HIGH_RES_MODE = 0x10;

DH1750::DH1750(uint8_t bus, uint8_t address) {
  if (gpioInitialise() < 0) {
    std::cout << "Failed to init gpio" << std::endl;
    return;
  }

  handle_ = i2cOpen(bus, address, 0);
  if (handle_ < 0) {
    std::cout << "Failed to get handle" << std::endl;
    return;
  }
  i2cWriteByte(handle_, CONTINUOUS_HIGH_RES_MODE);
}

DH1750::~DH1750() { i2cClose(handle_); }

double DH1750::read() const {
  uint8_t val[2] = {0};
  int res = i2cReadDevice(handle_, (char*)val, 2);
  if (res == -1) {
    return -1;
  }
  int lux = val[0] << 8;
  lux |= val[1];

  return lux / 1.2;
}
