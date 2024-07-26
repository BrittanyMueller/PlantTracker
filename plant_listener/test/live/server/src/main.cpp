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
#include <spdlog/spdlog.h>

#include <chrono>
#include <csignal>
#include <plantlistener/test/test_plant_server.hpp>
#include <thread>

void sigHandler(sigset_t sig_mask, plantlistener::test::TestPlantServer* server) {
  while (1) {
    auto sig = sigwaitinfo(&sig_mask, NULL);
    if (sig == -1) continue;

    if (sig == SIGUSR1) {
      return;  // Just exit a cancel request was made.
    }
    spdlog::info("Stop requested trying to stop PlantListener");
    auto res = server->stop();
    if (res.isError()) {
      spdlog::error("Failed to stop PlantListener with %s", res.toStr());
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

  // Start the test server
  plantlistener::test::TestPlantServer server;

  auto sig_thread = std::thread(sigHandler, sig_mask, &server);

  auto res = server.start();

  if (!res.isError()) {
    res = server.wait();
  }

  if (sig_thread.joinable()) {
    // Send user signal to notify exit.
    pthread_kill(sig_thread.native_handle(), SIGUSR1);
    sig_thread.join();
  }

  return res.isError() ? 1 : 0;
}
