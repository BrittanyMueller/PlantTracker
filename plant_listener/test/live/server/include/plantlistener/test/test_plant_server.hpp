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

#include <grpcpp/grpcpp.h>
#include <planttracker.grpc.pb.h>

#include <condition_variable>
#include <mutex>
#include <plantlistener/device/device.hpp>
#include <plantlistener/error.hpp>
#include <queue>

class PlantListenerServiceImpl;

namespace plantlistener::test {

class RequestQueue {
 private:
  std::queue<planttracker::grpc::ListenerRequest> requests_;
  std::condition_variable cv_{};
  std::mutex mtx_{};

 public:
  RequestQueue() = default;

  planttracker::grpc::ListenerRequest getRequest() {
    std::unique_lock<std::mutex> lck(mtx_);
    // If there are no events sleep until one shows up or stop is requested.
    if (requests_.empty()) {
      cv_.wait(lck, [&] { return !requests_.empty(); });
    }

    auto event = std::move(requests_.front());
    requests_.pop();
    return event;
  }

  void putRequest(planttracker::grpc::ListenerRequest request) {
    std::lock_guard<std::mutex> lck(mtx_);

    requests_.push(std::move(request));
    cv_.notify_one();
  }
};

class TestPlantServer {
 private:
  enum class State { NOT_INITALIZED, INITIALIZING, INITALIZED, STARTED, STOPPING };

  State state_{State::NOT_INITALIZED};

  std::unique_ptr<grpc::Server> server_{};
  std::unique_ptr<PlantListenerServiceImpl> service_{};

 public:
  TestPlantServer();
  ~TestPlantServer();

  TestPlantServer(const TestPlantServer&) = delete;
  TestPlantServer(TestPlantServer&&) = delete;
  TestPlantServer& operator=(const TestPlantServer&) = delete;
  TestPlantServer& operator=(TestPlantServer&&) = delete;

  // Saves the server data.
  std::vector<planttracker::grpc::MoistureDevice> devices;
  std::vector<planttracker::grpc::PlantSensorData> data;
  RequestQueue request_queue;

  /**
   * Starts running the application.
   *
   * The following will be done when start is called.
   * Starts the application
   * @returns Error The exit code of the server
   *    OK stop was called and it exitted cleanly
   *    TODO res of error codes
   */
  Error start();

  /**
   * Stops the application. This won't return until the application could be
   * stopped.
   *
   * @returns Error if the operation was successful.
   *    OK PlantListener was stopped.
   */
  Error stop();

  /** Waits for the application to stop.
   * This won't return until stop is called.
   */
  Error wait();

};

}  // namespace plantlistener::test
