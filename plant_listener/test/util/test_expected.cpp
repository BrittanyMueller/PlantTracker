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

#include <gtest/gtest.h>

#include <plantlistener/expected.hpp>

using plantlistener::Error;
using plantlistener::Expected;

TEST(TestExpected, test_ErrorToExpected) {
  Error ok_err;
  Expected<int> ok(ok_err);
  ASSERT_FALSE(ok.isError());
  ASSERT_EQ(ok.code, Error::Code::OK);

  Error err_err(Error::Code::ERROR_INTERNAL, "internal error");
  Expected<int> err(err_err);
  ASSERT_TRUE(err.isError());
  ASSERT_EQ(err.code, Error::Code::ERROR_INTERNAL);
}

TEST(TestExpected, test_badAccess) {
  Expected<int> err(Error::Code::ERROR_INTERNAL, "internal error");
  EXPECT_THROW(err.getValue(), plantlistener::Expected<int>::BadExpectedAccess);
  EXPECT_THROW(*err, plantlistener::Expected<int>::BadExpectedAccess);
}

TEST(TestExpected, test_value) {
  Expected<int> ten(10);
  ASSERT_EQ(ten.getValue(), 10);
}
