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

#include <plantlistener/error.hpp>
#include <sstream>

using Code = plantlistener::Error::Code;
using plantlistener::Error;

Error::Error() : code(Code::OK) {}
Error::Error(Code c, const std::string& m) : code(c), msg(m) {}

static const char* errorCodeToString(plantlistener::Error::Code code) {
  switch (code) {
    case Code::OK:
      return "OK";
    case Code::ERROR_INTERNAL:
      return "ERROR_INTERNAL";
    case Code::ERROR_INVALID_ARG:
      return "ERROR_INVALID_ARG";
    case Code::ERROR_INVALID_VALUE:
      return "ERROR_INVALID_VALUE";
    case Code::ERROR_INVALID_TYPE:
      return "ERROR_INVALID_TYPE";
    case Code::ERROR_MISSING:
      return "ERROR_MISSING";
    case Code::ERROR_IO:
      return "ERROR_IO";
    case Code::ERROR_AGAIN:
      return "ERROR_AGAIN";
    case Code::ERROR_NETWORKING:
      return "ERROR_NETWORKING";
    case Code::ERROR_NOT_CONNECTED:
      return "ERROR_NOT_CONNECTED";
    case Code::ERROR_NOT_INIT:
      return "ERROR_NOT_INIT";
    default:
      return "UNKNOWN";
  }
}

std::string Error::toStr() const {
  std::ostringstream ss;
  ss << *this;
  return ss.str();
}

std::ostream& plantlistener::operator<<(std::ostream& out, const plantlistener::Error& err) {
  return out << errorCodeToString(err.code) << ": " << err.msg;
}