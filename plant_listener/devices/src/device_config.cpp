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

#include <plantlistener/device/device_config.hpp>

using plantlistener::Error;
using plantlistener::Expected;
using plantlistener::device::DeviceType;

Expected<DeviceType> plantlistener::device::strToDeviceType(const std::string& dev_type) {
  if (dev_type == "ADC") {
    return {DeviceType::ADC};
  } else if (dev_type == "TEMP_AND_HUMIDITY") {
    return {DeviceType::TEMP_AND_HUMIDITY};
  } else if (dev_type == "PH_LEVEL") {
    return {DeviceType::PH_LEVEL};
  } else {
    return {Error::Code::ERROR_INVALID_VALUE,
            "DeviceType must be one of (ADC, TEMP_AND_HUMIDITY, or PH_LEVEL) "
            "but got " +
                dev_type};
  }
}
