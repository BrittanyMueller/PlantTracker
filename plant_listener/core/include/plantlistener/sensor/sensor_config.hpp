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
#pragma once

namespace plantlistener::core {

enum SensorType { LightSensor, MoistureSensor };
struct SensorConfig {
  SensorType type;
  int64_t id;
  std::string device_name{};
  int64_t device_port = 1;
};
}  // namespace plantlistener::core