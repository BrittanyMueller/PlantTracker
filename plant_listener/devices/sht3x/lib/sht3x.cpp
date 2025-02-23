/*
 * (C) Copyright 2026 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */

#include "sht3x.hpp"

#include <limits.h>
#include <pigpio.h>
#include <unistd.h>

#include <iostream>

namespace {
const char SHT3X_BREAK_CMD[] = {(char)0x30, (char)0x93};
const char SHT3X_SRESET_CMD[] = {(char)0x30, (char)0xA2};

}  // namespace

SHT3X::SHT3X(uint8_t bus, uint8_t address) {
  if (gpioInitialise() < 0) {
    std::cout << "Failed to init gpio" << std::endl;
    return;
  }

  handle_ = i2cOpen(bus, address, 0);
  if (handle_ < 0) {
    std::cout << "Failed to get handle" << std::endl;
    return;
  }

  const char* startCmds[] = {{SHT3X_BREAK_CMD}, {SHT3X_SRESET_CMD}};
  for (int i =0; i < 2; i++) {
    auto res = i2cWriteDevice(handle_, (char*)startCmds[i], 2);
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
    usleep(100);
  }
}

SHT3X::~SHT3X() { i2cClose(handle_); }

SHT3X::SHT3XData SHT3X::read() const {
  SHT3XData data;
  char signalShotCmd[] = {0x2C, 0x06};
  auto res = i2cWriteDevice(handle_, signalShotCmd, 2);
  if (res == -1) {
    data.crc = -1;
    return data;
  }
  char rawData[6] = {0};
  res = i2cReadDevice(handle_, rawData, 6);
  if (res == -1) {
    data.crc = -1;
    return data;
  }

  data.temp = -45 + 175.0f * static_cast<float>((rawData[0] << 8) | rawData[1])/(UINT16_MAX);
  data.humidity = 100.0f * static_cast<float>((rawData[3] << 8) | rawData[5])/(UINT16_MAX);
  data.crc = (rawData[2] << 8) | rawData[5];
  return data;
}
