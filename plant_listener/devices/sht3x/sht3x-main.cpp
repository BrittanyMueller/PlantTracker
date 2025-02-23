#include <sht3x.hpp>
#include <pigpio.h>
#include <stdio.h>
#include <unistd.h>

int main() {
  if (gpioInitialise() < 0) return 1;
  SHT3X dev;
  while (1) {
    SHT3X::SHT3XData data = dev.read();
    printf("temp: %lf, hum:%lf, crc:%d\n", data.temp, data.humidity, (int)data.crc);
    sleep(2);
  }
}
