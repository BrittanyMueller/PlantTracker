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

#include "command_line.hpp"

#include <fmt/format.h>
#include <getopt.h>
#include <spdlog/sinks/basic_file_sink.h>

#include <optional>

using plantlistener::Error;
using plantlistener::Expected;
using plantlistener::core::PlantListenerConfig;

void plantlistener::client::helpMenu(std::ostream& out) {
  out << "Usage: plant_listener --config <config>.json [OPTION]...\n"
      << "Monitor for plants, collecting sensor data and reporting it back to the plant tracker server.\n"
      << "\n"
      << "  -h, --help                     Output this menu and exit.\n"
      << "  -c, --config       <cfg>       Configuration of plant tracker [required].\n"
      << "  -a, --tracker-ip   <address>   Override PlantTracker server address in config.\n"
      << "  -p, --tracker-port <port>      Override PlantTracker server port in config.\n"
      << "  -l, --log-level    <level>     Log level must be one of (ERR, WARN, STAT, INFO, DBG).\n"
      << "  -f, --log-file     <file>      Logging file. If it already exists it will\n"
      << "                                 be appendend to the existing log.\n"
      << "  -q, --quiet                    Won't log anything to the console and only the log file\n"
      << "                                 if provided." << std::endl;
}

static Expected<int32_t> parseInt(const std::string& val, const std::string& arg_name, std::optional<int32_t> min = {},
                                  std::optional<int32_t> max = {}) {
  int32_t i;

  try {
    std::size_t pos = 0;
    i = std::stoi(val, &pos);
    if (pos != val.length()) {
      std::stringstream err;
      err << "Invalid number " << std::quoted(val) << " for " << std::quoted(arg_name);
      return {Error::Code::ERROR_INVALID_VALUE, err.str()};
    }
  } catch (const std::invalid_argument& e) {
    std::stringstream err;
    err << "Failed to parse " << std::quoted(val) << " With: invalid argument.";
    return {Error::Code::ERROR_INVALID_VALUE, err.str()};
  } catch (const std::out_of_range& e) {
    std::stringstream err;
    err << "Failed to parse " << std::quoted(val) << " With: out of range";
    return {Error::Code::ERROR_OUT_OF_RANGE, err.str()};
  }

  if ((min.has_value() && min.value() > i) || (max.has_value() && max.value() < i)) {
    std::stringstream err;
    err << "Failed to parse " << std::quoted(arg_name) << " expected value between ["
        << (min.has_value() ? std::to_string(min.value()) : "-infinity") << ", "
        << (max.has_value() ? std::to_string(max.value()) : "infinity") << "] but got "
        << std::quoted(std::to_string(i));
    return {Error::Code::ERROR_OUT_OF_RANGE, err.str()};
  }
  return {i};
}

Expected<PlantListenerConfig> plantlistener::client::parseArguments(int argc, char* argv[]) {
  PlantListenerConfig cfg;

  int quiet_flag;
  const char* short_args = "qha:p:l:f:c:";
  option long_options[] = {{.name = "tracker-ip", .has_arg = required_argument, .flag = 0, .val = 'a'},
                           {.name = "config", .has_arg = required_argument, .flag = 0, .val = 'c'},
                           {.name = "tracker-port", .has_arg = required_argument, .flag = 0, .val = 'p'},
                           {.name = "log-level", .has_arg = required_argument, .flag = 0, .val = 'l'},
                           {.name = "log-file", .has_arg = required_argument, .flag = 0, .val = 'f'},
                           {.name = "quiet", .has_arg = required_argument, .flag = &quiet_flag, .val = 'q'},
                           {.name = "help", .has_arg = no_argument, .flag = 0, .val = 'h'},
                           {.name = nullptr, .has_arg = 0, .flag = nullptr, .val = 0}};

  char c;
  int option_index = 0;

  // First find the config and log level so we can load that in before the command line
  while ((c = getopt_long(argc, argv, short_args, long_options, &option_index)) != -1) {
    switch (c) {
      case 'h':
        plantlistener::client::helpMenu();
        exit(0);
        break;
      case 'c':
        cfg.config_path = optarg;
        break;
      case 'l':
        if (strcmp(optarg, "ERR") == 0) {
          cfg.log_level = spdlog::level::err;
        } else if (strcmp(optarg, "WARN") == 0) {
          cfg.log_level = spdlog::level::warn;
        } else if (strcmp(optarg, "STAT") == 0) {
          cfg.log_level = spdlog::level::info;
        } else if (strcmp(optarg, "INFO") == 0) {
          cfg.log_level = spdlog::level::info;
        } else if (strcmp(optarg, "DBG") == 0) {
          cfg.log_level = spdlog::level::debug;
        } else {
          return {Error::Code::ERROR_INVALID_VALUE, "Invald log level must be one of (ERR, WARN, STAT, INFO, DBG)"};
        }
        spdlog::set_level(cfg.log_level);
        break;
    }
  }

  optind = 1;
  option_index = 0;
  while ((c = getopt_long(argc, argv, short_args, long_options, &option_index)) != -1) {
    switch (c) {
      case 'a':
        cfg.address = optarg;
        break;
      case 'p': {
        auto val = parseInt(optarg, "--tracker-port", 0, 65535);
        if (val.isError()) {
          return val;
        }
        cfg.port = val.getValue();
        break;
      }
      case 'f':
        try {
          spdlog::default_logger()->sinks().emplace_back(
              std::make_shared<spdlog::sinks::basic_file_sink_mt>(optarg));
        } catch (const spdlog::spdlog_ex& e) {
          return {Error::Code::ERROR_IO, fmt::format("Failed to open log file '{}' with: {}", optarg, e.what())};
        }
        break;
      case '?':
        plantlistener::client::helpMenu();
        return {Error::Code::ERROR_INVALID_ARG, fmt::format("Error on {}", argv[opterr])};
      default:
        plantlistener::client::helpMenu();
    }
  }

  return {cfg};
}
