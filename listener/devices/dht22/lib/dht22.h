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

// Created based on
// https://github.com/adafruit/Adafruit_CircuitPython_DHT/tree/main
#include <pthread.h>
#include <stdint.h>
#include <time.h>

struct DHT22Data {
  float temp;
  float humidity;
  int err;
};

// Should read 40 bits
struct DHT22ReadData {
  unsigned char bit_number;  // Which bit we are reading.
  uint16_t data[5];
};

struct DHT22 {
  // Info about last read.
  time_t last_read_ts;
  struct DHT22Data last_read_data;

  // Info needed while reading the data.
  struct DHT22ReadData current_read;
  int pin;
  int reading;
  int finished_reading;

  pthread_mutex_t mutex;
  pthread_cond_t cv;
};

struct DHT22 init_dht22(int pin);
void free_dht22(struct DHT22* dht22);
struct DHT22Data read_dht22(struct DHT22* dht22);
