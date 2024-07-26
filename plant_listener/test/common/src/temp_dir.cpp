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

#include "temp_dir.hpp"

#include <cstdlib>
#include <cstring>
#include <filesystem>
#include <iostream>

TempDir::TempDir(std::string prefix) {
  char tmp[prefix.size() + 20] = "/tmp/";
  strncat(tmp, prefix.c_str(), prefix.size());
  strncat(tmp, "XXXXXX", 7);

  char* p = mkdtemp(tmp);
  if (!p) {
    std::cerr << "Failed to create temp directory" << std::endl;
    exit(1);
  }
  path = p;
}

TempDir::~TempDir() { std::filesystem::remove_all(path); }
