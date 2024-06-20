/*
 * (C) Copyright 2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */
#include <gtest/gtest.h>

#include <chrono>
#include <plantlistener/plant_listener.hpp>
#include <plantlistener/test/test_plant_server.hpp>
#include <vector>

using plantlistener::test::TestPlantServer;

namespace plantlistener::core {
class PlantListenerTester {
 public:
  PlantListener listener;

  PlantListenerTester(PlantListenerConfig&& cfg) : listener(std::move(cfg)) {}
  const std::vector<std::unique_ptr<Sensor>>& getSensors() { return listener.sensors_; }
  const std::vector<std::shared_ptr<Plant>>& getPlants() { return listener.plants_; }
  const std::unordered_map<std::string, std::shared_ptr<plantlistener::device::Device>>& getDevices() {
    return listener.devices_;
  }
};
}  // namespace plantlistener::core

using plantlistener::core::PlantListenerConfig;
using plantlistener::core::PlantListenerTester;
using plantlistener::core::Sensor;
using plantlistener::core::SensorConfig;
using plantlistener::core::SensorType;
using plantlistener::device::Device;
using plantlistener::device::DeviceConfig;
using plantlistener::device::DeviceType;

TEST(LiveTests, start_stop_test) {
  PlantListenerConfig cfg;
  cfg.address = "127.0.0.1";
  cfg.port = 5051;
  cfg.poll_rate = std::chrono::seconds(1);  // poll every 1 seconds so we should get a good amount of events.

  // Define default sensor for light and humidity.
  SensorConfig light_sensor_cfg;
  light_sensor_cfg.device_name = "mock_dev_adc";
  light_sensor_cfg.device_port = 1;
  light_sensor_cfg.type = SensorType::LIGHT;
  cfg.sensors.emplace_back(std::move(light_sensor_cfg));

  SensorConfig humidity_sensor_cfg;
  humidity_sensor_cfg.device_name = "mock_dev_tmp_humidity";
  humidity_sensor_cfg.type = SensorType::HUMIDITY;
  humidity_sensor_cfg.device_port = HUMIDITY_PORT;
  cfg.sensors.emplace_back(std::move(humidity_sensor_cfg));

  SensorConfig temp_sensor_cfg;
  temp_sensor_cfg.device_name = "mock_dev_tmp_humidity";
  temp_sensor_cfg.type = SensorType::TEMP;
  temp_sensor_cfg.device_port = TEMP_PORT;
  cfg.sensors.emplace_back(std::move(temp_sensor_cfg));

  // Define default devices for light/plant, temp/humidity.
  DeviceConfig temp_humidity;
  temp_humidity.lib = "lib/libplantlistener-device-mock-dev.so";
  temp_humidity.name = "mock_dev_tmp_humidity";
  temp_humidity.ports = 2;
  temp_humidity.type = DeviceType::TEMP_AND_HUMIDITY;
  cfg.devices.emplace_back(std::move(temp_humidity));

  DeviceConfig adc_dev;
  adc_dev.lib = "lib/libplantlistener-device-mock-dev.so";
  adc_dev.name = "mock_dev_adc";
  adc_dev.ports = 8;
  adc_dev.type = DeviceType::ADC;
  cfg.devices.emplace_back(std::move(adc_dev));

  // Start up the test server
  TestPlantServer server;
  PlantListenerTester tester(std::move(cfg));

  auto res = tester.listener.init();
  ASSERT_FALSE(res.isError()) << res.toStr();

  // Start the server
  server.start();

  // Start the listener.
  std::thread listener_thread([&] {
     auto res = tester.listener.start();
     EXPECT_FALSE(res.isError()) << res.toStr();
   });

  std::this_thread::sleep_for(std::chrono::seconds(10));

  // Stop the listener.
   res = tester.listener.stop();
   EXPECT_FALSE(res.isError()) << res.toStr();


   listener_thread.join();
  // Stop the server
  res = server.stop();
  EXPECT_FALSE(res.isError()) << res.toStr();

  // Check that at least 10 data points were sent for 10 second of runtime with 1 second timeout on reads.
  ASSERT_GE(server.data.size(), 10);
  for (const auto& data: server.data) {
    ASSERT_EQ(data.plant_id(), 1);
  }
  
  // Check the device was added
  ASSERT_EQ(server.devices.size(), 1); // Only the ADC should be sent over
  ASSERT_EQ(server.devices[0].name(), "mock_dev_adc");
  ASSERT_EQ(server.devices[0].num_sensors(), 8); // TODO(lmilne) This should be 7 because one is used for light.


  // Check to make sure the plants are added
  ASSERT_EQ(tester.getPlants().size(), 1);
  ASSERT_EQ(tester.getPlants()[0]->getId(), 1);
}
