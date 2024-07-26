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
#pragma once

#include <optional>
#include <plantlistener/error.hpp>
#include <sstream>

namespace plantlistener {

template <typename U>
class Expected : public Error {
 public:
  class BadExpectedAccess : public std::exception {
   private:
    std::string err_ = {};

   public:
    BadExpectedAccess(const std::string& error_msg) : err_("Bad Expected access, has Error: " + error_msg) {}

    virtual const char* what() const noexcept { return err_.c_str(); }
  };

 private:
  std::optional<U> value_ = {};

 public:
  Expected() {}
  Expected(const U& value) : value_(value) {}
  Expected(U&& value) : value_(std::move(value)) {}
  Expected(Error::Code error, const std::string& msg) : Error(error, msg) {}
  Expected(const Error& e) : Error(e) {}

  /**
   * Access the value of the expected object.
   *
   * @return U& value of type Expected. If isError() is true or it doesn't
   * contain a value BadExpectedAccess will be thrown instead.
   *
   * @throws BadExpectedAccess if expected contains an error, or doesn't contain
   * a value.
   */
  U& getValue() {
    if (!value_.has_value() || isError()) {
      std::stringstream err;
      err << *this;
      throw BadExpectedAccess(err.str());
    }
    return *value_;
  }

  U& operator*() { return getValue(); }
};
}  // namespace plantlistener
