/*
 * (C) Copyright 2023-2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */
#include <spdlog/spdlog.h>

#include <csignal>
#include <plantlistener/device/device_loader.hpp>
#include <plantlistener/plant_listener.hpp>
#include <thread>

#include "command_line.hpp"

using plantlistener::core::DeviceLoader;

void sigHandler(sigset_t sig_mask, plantlistener::core::PlantListener* plant_listener) {
  while (1) {
    auto sig = sigwaitinfo(&sig_mask, NULL);
    if (sig == -1) continue;
    
    if (sig == SIGUSR1) {
      return; // Just exit a cancel request was made. 
    }
    spdlog::info("Stop requested trying to stop PlantListener");
    auto res = plant_listener->stop();
    if (res.isError()) {
      spdlog::error("Failed to stop PlantListener with {}", res.toStr());
    }
    return;
  }
}

int main(int argc, char* argv[]) {
  sigset_t sig_mask;
  sigemptyset(&sig_mask);
  sigaddset(&sig_mask, SIGINT);
  sigaddset(&sig_mask, SIGTERM);
  sigaddset(&sig_mask, SIGUSR1);
  pthread_sigmask(SIG_BLOCK, &sig_mask, NULL);

  auto config_ret = plantlistener::client::parseArguments(argc, argv);
  if (config_ret.isError()) {
    spdlog::error("Failed to read config with: {}", config_ret.toStr());
    return 1;
  }
  plantlistener::core::PlantListener plant_listener(std::move(*config_ret));

  auto res = plant_listener.init();
  if (res.isError()) {
    spdlog::error("Failed to init with: {}", res.toStr());
  }

  auto sig_thread = std::thread(sigHandler, sig_mask, &plant_listener);

  res = plant_listener.start();
  if (res.isError()) {
    spdlog::error(res.toStr());
  }

  if (sig_thread.joinable()) {
    // Send user signal to notify exit.
    pthread_kill(sig_thread.native_handle(), SIGUSR1);
    sig_thread.join();
  }

  return res.isError() ? 1 : 0;
}