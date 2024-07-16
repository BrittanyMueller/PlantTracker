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

#include <ads7830.hpp>
#include <iostream>

int main() {
  ADS7830 adc;

  if (!adc.valid()) {
    std::cout << "ADC isn't valid exiting ..." << std::endl;
    exit(1);
  }

  while (true) {
    sleep(1);
    for (int i = 0; i < 8; i++) {
      std::cout << "pin=" << i << " value=" << static_cast<int>(adc.read(i)) << '\n';
    }
    std::cout << std::endl;
  }
}