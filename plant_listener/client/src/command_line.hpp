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

#include <iostream>
#include <plantlistener/expected.hpp>

#include <plantlistener/plant_listener_config.hpp>

namespace plantlistener::client {

void helpMenu(std::ostream& out = std::cout);
plantlistener::Expected<plantlistener::core::PlantListenerConfig> parseArguments(int argc, char* argv[]);
}  // namespace plantlistener::client
