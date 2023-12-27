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

#include <string>

class PlantListenerConfig {
 private:
    std::string name_{};
    std::string config_path_{};

    // TODO(lmilne) add light and plant sensor lists

public:
    PlantListenerConfig(const std::string& config_path) {

    }

    void save() {

    }

    void load() {

    }
};