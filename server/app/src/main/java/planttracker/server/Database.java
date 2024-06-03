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
    // db constructor  

    public void init() throws SQLException {
        // TODO move to config host, db, user, pass
        String url = "jdbc:postgresql://127.0.0.1/planttracker";
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "planttracker");

        Connection conn = DriverManager.getConnection(url, props);

        Statement st = conn.createStatement();
        st.execute("CREATE TABLE IF NOT EXISTS pi ("
            + "id SERIAL PRIMARY KEY,"
            + "mac VARCHAR(20) NOT NULL UNIQUE,"
            + "location VARCHAR(32))");
    }
}
