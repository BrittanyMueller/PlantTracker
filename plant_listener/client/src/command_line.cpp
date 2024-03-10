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

#include <getopt.h>

using plantlistener::Error;
using plantlistener::Expected;
using plantlistener::core::PlantListenerConfig;

void plantlistener::client::helpMenu(std::ostream& out) {
  out << "Usage: plant_listener --config <config>.json [OPTION]...\n"
      << "Monitor for plants, collecting sensor data and reporting it back to the plant tracker server.\n"
      << "\n"
      << "-h, --help                     Output this menu and exit.\n"
      << "-c, --config       <cfg>.json  Configuration of plant tracker [required]"
      << "-a, --tracker-ip   <address>   Override PlantTracker server address in config.\n"
      << "-p, --tracker-port <port>      Override PlantTracker server port in config.\n"
      << "-l, --log-level   <level>      Log level must be one of (ERR, WARN, STAT, INFO, DBG).\n"
      << "-f, --log-file    <file>       Logging file. If it already exists it will\n"
      << "                               be appendend.\n"
      << "-q, --quiet                    Won't log anything to the console and only the log file\n"
      << "                               if provided." << std::endl;
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
        spdlog::set_level(cfg.log_level.value());
        break;
    }
  }

  option_index = 0;
  while ((c = getopt_long(argc, argv, short_args, long_options, &option_index)) != -1) {
    switch (c) {
      case 'a':
        break;
      case 'p':
        break;
      case 'f':
        break;
      default:
        plantlistener::client::helpMenu();
        exit(1);  // TODO change to expected return.
    }
  }

  return {cfg};
}
