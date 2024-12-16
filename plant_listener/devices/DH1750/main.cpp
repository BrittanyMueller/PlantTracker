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

#include <dh1750.hpp>
#include <iostream>

int main() {
  DH1750 dev;

  if (!dev.valid()) {
    std::cout << "DEV isn't valid exiting ..." << std::endl;
    exit(1);
  }

  while (true) {
    sleep(1);
    std::cout << dev.read() << std::endl;
  }
}
