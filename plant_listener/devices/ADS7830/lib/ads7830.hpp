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

// Created based on data sheet found here
// https://pdf1.alldatasheet.com/datasheet-pdf/view/82673/BURR-BROWN/ADS7830.html

class ADS7830 {
 private:
  int handle_ = 0;

 public:
  ADS7830(uint8_t bus = 1, uint8_t address = 0x4b);
  ~ADS7830();

  inline bool valid() const { return handle_ >= 0; }

  uint8_t read(uint8_t pin) const;
};