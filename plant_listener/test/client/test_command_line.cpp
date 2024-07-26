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

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <command_line.hpp>
#include <fstream>
#include <nlohmann/json.hpp>
#include <sstream>
#include <vector>

#include "temp_dir.hpp"

using plantlistener::Error;
using plantlistener::client::parseArguments;
using namespace nlohmann::literals;
using nlohmann::json;

json default_config = R"(
  {
    "name": "Living Room",
    "server": {
        "address": "127.0.0.1",
        "port": 1234,
        "log_level": "DBG",
        "retry_count": -1,
        "retry_timeout": 5
    },
    "sensors": [],
    "devices": {
        "instances": [
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
)"_json;

class TestCommandLine : public testing::Test {
 public:
  TempDir tmp_dir_ = {"commandline"};
  std::string config_path = tmp_dir_.path + "/config.json";
  void SetUp() override {
    std::ofstream config(config_path);
    config << default_config.dump();
  }
};

TEST_F(TestCommandLine, helpMenu) {
  std::stringstream ss;
  plantlistener::client::helpMenu(ss);

  EXPECT_THAT(ss.str(), testing::HasSubstr("Usage: plant_listener --config"));
}

TEST_F(TestCommandLine, unknownOption) {
  std::cout << config_path << std::endl;
  std::vector<std::string> argv = {"plantlistener", "--config", config_path, "--foo"};
  char* argv_char[argv.size()];
  for (size_t i = 0; i < argv.size(); i++) argv_char[i] = argv[i].data();

  auto res = parseArguments(argv.size(), argv_char);
  EXPECT_TRUE(res.isError());
  EXPECT_EQ(res.code, Error::Code::ERROR_INVALID_ARG) << res.toStr();
}

TEST_F(TestCommandLine, config) {
  std::vector<std::string> argv = {"plantlistener", "--config", config_path};
  char* argv_char[argv.size()];
  for (size_t i = 0; i < argv.size(); i++) argv_char[i] = argv[i].data();

  auto res = parseArguments(argv.size(), argv_char);

  ASSERT_FALSE(res.isError());
  ASSERT_EQ(res.getValue().address, "127.0.0.1");
  ASSERT_EQ(res.getValue().log_level, spdlog::level::debug);
}
