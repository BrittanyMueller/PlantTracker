/*
 * (C) Copyright 2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
 *
 * This code is distributed on "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author: qawse3dr a.k.a Larry Milne
 * @author: BrittanyMueller
 */
package planttracker.server;

import java.sql.*;
import java.util.Properties;

public class Database {

    // TODO throw custom exceptions
    // TODO consider making function that takes query as string, wraps statement stuff 

    public Connection connection;

    public Database(PlantTrackerConfig config) throws SQLException {
        if (config == null) {
            // TODO error checking config, throw custom errors
            // where to do config validation? should this never happen since PlantTrackerConfig throws error?
            connection = null;
            return;
        }
        try {
            Properties props = new Properties();
            props.setProperty("user", config.dbUser);
            props.setProperty("password", config.dbPass);
            connection = DriverManager.getConnection(config.dbHost + config.dbName, props);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to database");
        }
        
    }

    public void init() throws SQLException {
        Statement st = connection.createStatement();
        st.execute("CREATE TABLE IF NOT EXISTS pi ("
            + "id SERIAL PRIMARY KEY,"
            + "mac VARCHAR(20) NOT NULL UNIQUE,"
            + "location VARCHAR(32))");
    }

    public void close() throws SQLException {
        connection.close();
    }
}
