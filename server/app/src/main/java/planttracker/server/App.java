/*
 * (C) Copyright 2023-2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTINES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */
package planttracker.server;

import java.sql.SQLException;

public class App {

  public static void main(String[] args) throws SQLException {
    PlantListenerServer server = new PlantListenerServer();
    
    // Connect to database and create tables if needed.
    Database db = new Database();
    db.init();
  
    try {
      server.start();
      server.blockUntilShutdown();
    } catch (Exception e) {
      System.err.println("Failed to run with " + e.toString());
    }
  }
}
