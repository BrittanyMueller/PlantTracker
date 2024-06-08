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

#include "dht22.h"

#include <limits.h>
#include <pigpio.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>

// TODO(qawse3dr) make this not seconds.
#define DHT22_COOLDOWN 2
//(CLOCKS_PER_SEC * 2)

#define MICROSECONDS(t) (t.tv_sec * (int)1e6 + t.tv_usec)

struct DHT22Data read_dht22(struct DHT22* dht22) {
  struct DHT22Data data = {0, 0, 0};


  // If someone is already reading wait for their result.
  pthread_mutex_lock(&dht22->mutex);
  while (dht22->reading) pthread_cond_wait(&dht22->cv, &dht22->mutex);

  // The DHT22 chip needs a bit of time before reading so if required just wait.
  time_t now = time(NULL);
  if (now - dht22->last_read_ts < DHT22_COOLDOWN) {
    data = dht22->last_read_data;
    goto read_exit; // Too early to read, return the last result.
  }

  // Reset the reading data.
  dht22->reading = 1;
  dht22->finished_reading = 0;
  memset((void*)&dht22->current_read, 0, sizeof(struct DHT22ReadData));

  // Write high then low for 3ms, this will notify DHT22 that we want to read.
  gpioSetMode(dht22->pin, PI_OUTPUT);
  gpioWrite(dht22->pin, 1);
  usleep(3000);
  gpioWrite(dht22->pin, 0);
  usleep(3000);
  
  struct timeval cur_time, old_time, timeout;
  gettimeofday(&cur_time, NULL);
  timeout = cur_time;


  // change to input to get the data
  gpioSetMode(dht22->pin, PI_INPUT);

  // Read the signal from the ping calculating the time between state changes.
  int count = 0;
  int old_value = 1;
  for (int i = 0; count < 40; i++) {
    while (gpioRead(dht22->pin) == old_value) {
      gettimeofday(&timeout, NULL);
      if (MICROSECONDS(timeout) - MICROSECONDS(cur_time) > 400) {
        data.err = 2;
        goto timeout_exit;
      }
    }

    old_time = cur_time;
    gettimeofday(&cur_time, NULL);

    // Ignore the first 2 state changes; the data sheet says that's the handshake. We also only care about falling edges as that will be where the data is set.
    if (old_value == 1 && i > 2) {
      uint8_t byte = count++ / 8;
      dht22->current_read.data[byte] <<= 1;
      dht22->current_read.data[byte] |= (MICROSECONDS(cur_time) - MICROSECONDS(old_time) >= 50);
    }
    old_value = !old_value;
  }

  // Calculate humidity and temp with the retrievd data.
  data.humidity = ((dht22->current_read.data[0] << 8) + dht22->current_read.data[1]) / 10.0f;
  data.temp = (((dht22->current_read.data[2] & 0x7F) << 8) + dht22->current_read.data[3]) / 10.0f;
  
  // Check sums is all the bits added together compared to the last one.
  if (((dht22->current_read.data[0] + dht22->current_read.data[1] + dht22->current_read.data[2] +
        dht22->current_read.data[3]) & 0xFF) != dht22->current_read.data[4]) {
    data.err = 1;
  }

timeout_exit:
  // Wake up anyone else who is waiting for the result.
  dht22->reading = 0;
  dht22->last_read_ts = now;
  pthread_cond_broadcast(&dht22->cv);
read_exit:
  pthread_mutex_unlock(&dht22->mutex);
  return data;
}

struct DHT22 init_dht22(int pin) {
  struct DHT22 dht22;
  dht22.last_read_ts = 0;
  dht22.pin = pin;
  dht22.reading = 0;
  memset((void*)&dht22.current_read, 0, sizeof(struct DHT22ReadData));
  pthread_mutex_init(&dht22.mutex, NULL);
  pthread_cond_init(&dht22.cv, NULL);

  return dht22;
}

void free_dht22(struct DHT22* dht22) {
    pthread_mutex_destroy(&dht22->mutex);
    pthread_cond_destroy(&dht22->cv);
}
