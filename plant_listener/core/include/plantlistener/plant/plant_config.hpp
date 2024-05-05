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
struct PlantConfig {
  std::string name{};
  int64_t id = -1;
  int64_t light_id = -1;
  int64_t moisture_id = -1;
};
}  // namespace plantlistener::core