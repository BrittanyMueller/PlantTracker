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

import planttracker.server.exceptions.PlantTrackerException;

public class Database {

    public Connection connection;

    private static String[] createTableQueries = {
        """
        CREATE TABLE IF NOT EXISTS pi (
        id SERIAL PRIMARY KEY,
        mac VARCHAR(20) NOT NULL UNIQUE,
        location VARCHAR(32)
        );""",
        """
        CREATE TABLE IF NOT EXISTS moisture_devices (
        id SERIAL PRIMARY KEY,
        name VARCHAR(20) NOT NULL,
        num_sensors INT NOT NULL,
        pid INT,
        FOREIGN KEY (pid) REFERENCES pi(id)
        );""",
        """
        CREATE TABLE IF NOT EXISTS plants (
        id SERIAL PRIMARY KEY,
        name VARCHAR(20) NOT NULL,
        img_path VARCHAR(50),
        moisture_sensor_device_id INT,
        moisture_sensor_port INT,
        light_level INT, -- checked value 0, 1, 2? enum?
        min_moisture INT, -- checked value 1-10
        min_humidity INT, -- checked value 0-100
        pid INT,
        FOREIGN KEY (moisture_sensor_device_id) REFERENCES moisture_devices(id),
        FOREIGN KEY (pid) REFERENCES pi(id)
        );""",
        """
        CREATE TABLE IF NOT EXISTS plant_data (
        plant_id INT,
        ts TIMESTAMP NOT NULL,
        moisture REAL,
        light REAL, -- lumens? lux? what actually is this
        temp REAL,
        humidity REAL,
        FOREIGN KEY (plant_id) REFERENCES plants(id),
        PRIMARY KEY (plant_id, ts)
        );"""
    };

    private static String[] insertTestQueries = {

    };

    public Database(PlantTrackerConfig config) throws PlantTrackerException {
        try {
            // User required to create database beforehand
            connection = DriverManager.getConnection(config.dbHost + config.dbName, config.dbUser, config.dbPass);
            System.out.println("Connected to Postgres");
        } catch (SQLException e) {
            throw new PlantTrackerException("Failed to connect to database.", e);
        } 
    }

    public void createTables() throws PlantTrackerException {
        Statement st = null;
        try {
            st = connection.createStatement();
            for (String sql : createTableQueries) {
                st.execute(sql);
            }
            st.close();
        } catch (SQLException e) {
            throw new PlantTrackerException("Failed to create tables.", e);
        }
    }

    public void close() throws SQLException {
        connection.close();
    }
}
