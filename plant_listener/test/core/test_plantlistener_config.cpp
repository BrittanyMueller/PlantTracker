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

#include <gtest/gtest.h>

#include <nlohmann/json.hpp>
#include <plantlistener/plant_listener_config.hpp>

namespace plantlistener::core {

class PlantListenerConfigTester {
 public:
  template <typename T>
  static void parseValue(const nlohmann::json& j, const std::string& key, T& value) {
    return PlantListenerConfig::parseValue<T>(j, key, value);
  }

  static SensorConfig parseSensor(const nlohmann::json& j) { return PlantListenerConfig::parseSensor(j); }
};

}  // namespace plantlistener::core

using plantlistener::Error;
using plantlistener::core::PlantListenerConfig;
using plantlistener::core::PlantListenerConfigTester;

class PlantListenerConfigTest : public ::testing::Test {
 public:
  void expectThrowWithCode(std::function<void(void)> ftn, Error::Code code) {
    try {
      ftn();
      FAIL() << "Expected throw with code " << static_cast<int>(code);
    } catch (const PlantListenerConfig::ParseException& e) {
      EXPECT_EQ(e.getError().code, code);
    }
  }
};

TEST_F(PlantListenerConfigTest, parseValue_not_found) {
  int val;
  expectThrowWithCode([&] { PlantListenerConfigTester::parseValue<int>({}, "dne", val); },
                      Error::Code::ERROR_NOT_FOUND);
}

TEST_F(PlantListenerConfigTest, parseValue_wrong_type) {
  int val;
  expectThrowWithCode(
      [&] {
        PlantListenerConfigTester::parseValue<int>({{"int", "foo"}}, "int", val);
      },
      Error::Code::ERROR_INVALID_TYPE);
}

TEST_F(PlantListenerConfigTest, parseValue_success) {
  int val;
  PlantListenerConfigTester::parseValue<int>({{"int", 10}}, "int", val);
  ASSERT_EQ(val, 10);
}
