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

#include <plantlistener/error.hpp>
#include <grpcpp/grpcpp.h>

namespace plantlistener::test {

class TestPlantServer {
 private:
  enum class State { NOT_INITALIZED, INITIALIZING, INITALIZED, STARTED, STOPPING };

  State state_{State::NOT_INITALIZED};

  std::unique_ptr<grpc::Server> server{};

 public:
  TestPlantServer();
  ~TestPlantServer();

  TestPlantServer(const TestPlantServer&) = delete;
  TestPlantServer(TestPlantServer&&) = delete;
  TestPlantServer& operator=(const TestPlantServer&) = delete;
  TestPlantServer& operator=(TestPlantServer&&) = delete;

  /**
   * Starts running the application. This won't return until stop is called.
   *
   * The following will be done when start is called.
   * Starts the application
   * @returns Error The exit code of the server
   *    OK stop was called and it exitted cleanly
   *    TODO res of error codes
   */
  Error start();

  /**
   * Stops the application. This won't return until the application could be stopped.
   *
   * @returns Error if the operation was successful.
   *    OK PlantListener was stopped.
   */
  Error stop();

  // TODO do I need an add/remove plant
};

}  // namespace plantlistener::test::server
