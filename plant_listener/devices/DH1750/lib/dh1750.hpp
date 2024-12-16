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

#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>
#include <stdint.h>

class DH1750 {
 private:
  int handle_ = 0;

 public:
  DH1750(uint8_t bus = 0, uint8_t address = 0x23);
  ~DH1750();

  inline bool valid() const { return handle_ >= 0; }

  double read() const;
};
