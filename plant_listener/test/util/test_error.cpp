/*
 * (C) Copyright 2023 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */

#include <gtest/gtest.h>

#include <plantlistener/error.hpp>

using plantlistener::Error;

TEST(TestPlantError, test_isError) {
  // The default error should be OK
  Error ok;
  ASSERT_FALSE(ok.isError());
  ASSERT_EQ(ok.code, Error::Code::OK);

  Error err(Error::Code::ERROR_INTERNAL, "internal error");
  ASSERT_TRUE(err.isError());
  ASSERT_EQ(err.code, Error::Code::ERROR_INTERNAL);
}

TEST(TestPlantError, test_toStr) {
  // The default error should be OK
  Error ok;
  ASSERT_FALSE(ok.isError());
  ASSERT_EQ(ok.toStr(), "OK: ");

  Error err(Error::Code::ERROR_INTERNAL, "internal error");
  ASSERT_TRUE(err.isError());
  ASSERT_EQ(err.toStr(), "ERROR_INTERNAL: internal error");
}
