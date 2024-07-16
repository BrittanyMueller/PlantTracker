# Plant Listener
Plant Listener will act as a single node the network of plant sensor stations.

## Responsibilities
Plant Listener will be responsible for maintaining the configuration of the hardware and connected plants which is saved in a config file with the default location
of `~/.config/PlantTracker/listener_config.json`.

## Introduction

### Client
Client will be the main component of Plant Listener and will act as the middle man between the actual hardware sensors, and the Plant Tracker server. It uses GRPC and ProtoBufs to send and receive data from the Plant Tracker server and will interface with `sensor's` and `device's` to read and convert data.

### Sensors
Currently Plant Listener supports 2 types of Sensors, moisture sensors, and light sensors. In the future this can be expanded to temperature sensors, or any other data points. The only requirement of a sensor is it should be able to supply its data though a single data pin read by an ADC

### Devices
Devices are ADC or the like devices that translate data from a port and supplies it to a sensor to be rectified and passed to the client.
Devices will take the form of shared libraries that are can be loaded in when starting up the client.

## Pre-requisites
- Protobufs
- pigpio (For ADC not needed if building tests)
- gtest (for unit tests)
- cmake (3.20 or greater)

## Building
If building on host machine you can only build unit tests as devices will require gpio libraries. instead a mock sensor will be build
```bash
mkdir build_tests
cd build_tests
cmake .. -DCMAKE_BUILD_TYPE=test
make -j4
# To run the unit tests
make test
```

## Config
The configure requies the following fields. and must be in a rw location as the server will update it
```json
{
    "name": "Living Room",
    "server": {
        "address": "127.0.0.1",
        "port": 5051,
        "log_level": "DBG",
        "retry_count": -1,
        "retry_timeout": 5
    },
    "sensors": [
        {
            "type": "light",
            "device": "dev1",
            "port": 1
        },
        {
            "type": "temp",
            "device": "temp_and_hum_dev"
        },
        {
            "type": "humidity",
            "device": "temp_and_hum_dev"
        }
    ],

    "devices": {
        "instances": [
            {
                "name": "dev1",
                "type": "ADC",
                "lib": "mock_dev",
                "ports" : 8,
                "cfg": {
                    "min": 0,
                    "max": 255
                }
            },
            {
                "name": "temp_and_hum_dev",
                "type": "TEMP_AND_HUMIDITY",
                "lib": "mock_dev",
                "ports": 2,
                "cfg": {
                    "min": 0,
                    "max": 100
                }
            }
        ],
        "libs": [
            {
                "name": "mock_dev",
                "lib_path": "lib/libplantlistener-device-mock-dev.so"
            }
        ]
    }
}
```