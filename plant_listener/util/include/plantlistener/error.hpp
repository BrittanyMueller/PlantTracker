/*
 * (C) Copyright 2023 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */
#pragma once

#include <iostream>

namespace plantlistener {

class Error {
 public:
  enum class Code {
    OK = 0,
    ERROR_INTERNAL,
    ERROR_INVALID_ARG,
    ERROR_INVALID_VALUE,
    ERROR_INVALID_TYPE,
    ERROR_MISSING,
    ERROR_FILE_NOT_FOUND,
    ERROR_IO,
    ERROR_AGAIN,
    ERROR_NETWORKING,
    ERROR_NOT_CONNECTED,
    ERROR_NOT_INIT,
    ERROR_OUT_OF_RANGE
  };

  Code code = Code::OK;
  std::string msg = {};

  Error();
  Error(Code c, const std::string& m);

  inline bool isError() const { return code != Code::OK; };
  std::string toStr() const;
  friend std::ostream& operator<<(std::ostream& out, const plantlistener::Error& err);
};

std::ostream& operator<<(std::ostream& out, const plantlistener::Error& err);
}  // namespace plantlistener
