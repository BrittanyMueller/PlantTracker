#include <dht22.h>
#include <pigpio.h>
#include <stdio.h>
#include <unistd.h>

int main() {
  if (gpioInitialise() < 0) return 1;
  struct DHT22 dht22 = init_dht22(4);
  while (1) {
    struct DHT22Data data = read_dht22(&dht22);
    if (data.err) {
      printf("check sums failed!\n");
    } else {
      printf("temp: %lf, hum:%lf\n", data.temp, data.humidity);
    }
    sleep(2);
  }
}