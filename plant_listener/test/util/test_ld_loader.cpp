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

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <filesystem>
#include <plantlistener/ld_loader.hpp>

extern "C" {
typedef int (*addftn)(int a, int b);
}
#define ADD_NAME "add"

using plantlistener::LdLoader;
using testing::HasSubstr;

TEST(LdLoader, test_fileNotFound) {
  LdLoader<addftn> loader(ADD_NAME);

  auto res = loader.getHandler("foo", "path/doesnt/exist");
  ASSERT_TRUE(res.isError());
  EXPECT_THAT(res.toStr(), HasSubstr("Library could not be found at"));
}

TEST(LdLoader, test_differentPath) {
  LdLoader<addftn> loader(ADD_NAME);

  auto res = loader.getHandler("foo", "lib/libld_loader_lib.so");
  ASSERT_FALSE(res.isError());

  res = loader.getHandler("foo", "lib/libld_loader_lib2.so");
  ASSERT_TRUE(res.isError());
  EXPECT_THAT(res.toStr(), HasSubstr("Library already defined with name foo,"));
}

TEST(LdLoader, test_useHandle) {
  LdLoader<addftn> loader(ADD_NAME);

  auto res = loader.getHandler("foo", "lib/libld_loader_lib.so");
  std::cout << std::filesystem::current_path() << std::endl;
  ASSERT_FALSE(res.isError());

  ASSERT_EQ((*res)(10, 20), 30);
}

TEST(LdLoader, test_dump) {
  LdLoader<addftn> loader(ADD_NAME);

  auto res = loader.getHandler("foobar", "lib/libld_loader_lib.so");
  ASSERT_FALSE(res.isError());

  res = loader.getHandler("foobaz", "lib/libld_loader_lib2.so");
  ASSERT_FALSE(res.isError());

  EXPECT_THAT(loader.dump().dump(), HasSubstr("foobar"));
  EXPECT_THAT(loader.dump().dump(), HasSubstr("foobaz"));
  EXPECT_THAT(loader.dump().dump(), HasSubstr("lib/libld_loader_lib2.so"));
}
