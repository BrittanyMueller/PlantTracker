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

#include <plantlistener/sensor/sensor_config.hpp>

using plantlistener::core::SensorType;
using plantlistener::Expected;
using plantlistener::Error;


Expected<SensorType> plantlistener::core::strToSensorType(const std::string& sensor_type) {
    if (sensor_type == "light") {
        return {SensorType::LIGHT};
    } else if (sensor_type == "moisture") {
        return {SensorType::MOISTURE};
    } else if (sensor_type == "humidity") {
        return {SensorType::HUMIDITY};
    } else if (sensor_type == "temp") {
        return {SensorType::TEMP};
    } else {
        return {Error::Code::ERROR_INVALID_VALUE, "SensorType must be one of (moisture, temp, humidity, light) but got " + sensor_type};
    }
}
