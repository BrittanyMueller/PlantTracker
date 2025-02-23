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

#include <limits.h>
#include <pigpio.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>

// Created based on data sheet and circuit python found here
// https://github.com/adafruit/Adafruit_CircuitPython_SHT31D/blob/main/adafruit_sht31d.py
// and
// https://cdn-shop.adafruit.com/product-files/5064/5064_Sensirion_Humidity_Sensors_SHT3x_Datasheet_digital.pdf

class SHT3X {
 public:
  struct SHT3XData {
    float humidity;
    float temp;
    uint16_t crc;  // Currently not checked
  };

 private:
  int handle_ = -1;

 public:
  SHT3X(uint8_t bus = 0, uint8_t address = 0x44);
  ~SHT3X();

  inline bool valid() const { return handle_ >= 0; }

  SHT3XData read() const;

 private:
};
