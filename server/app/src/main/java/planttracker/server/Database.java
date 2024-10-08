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
import java.util.concurrent.locks.ReentrantLock;

import java.util.logging.*;
import planttracker.server.exceptions.PlantTrackerException;

public class Database {

    private static Database instance = null;
    public Connection connection;

    private final static Logger logger = Logger.getGlobal(); 
    private final ReentrantLock dbLock = new ReentrantLock();

    // TODO more reliable way of reading this in?
    private static String[] createTableQueries = {
        """
        CREATE TABLE IF NOT EXISTS pi (
        id SERIAL PRIMARY KEY,
        uuid VARCHAR(40) NOT NULL UNIQUE,
        name VARCHAR(32)
        );""",
        """
        CREATE TABLE IF NOT EXISTS moisture_devices (
        id SERIAL PRIMARY KEY,
        name VARCHAR(20) NOT NULL UNIQUE,
        num_sensors INT NOT NULL,
        pid INT,
        FOREIGN KEY (pid) REFERENCES pi(id) ON DELETE CASCADE
        );""",
        """
        CREATE TABLE IF NOT EXISTS plants (
        id SERIAL PRIMARY KEY,
        name VARCHAR(20) NOT NULL,
        image_url VARCHAR(128),
        light_level INT, -- checked value 0, 1, 2? enum?
        min_moisture INT, -- checked value 1-10
        min_humidity INT, -- checked value 0-100
        pid INT,
        FOREIGN KEY (pid) REFERENCES pi(id) ON DELETE CASCADE
        );""",
        """
        CREATE TABLE IF NOT EXISTS sensors (
        moisture_device_id INT,
        sensor_port INT,
        plant_id INT NULL,
        FOREIGN KEY (moisture_device_id) REFERENCES moisture_devices(id) ON DELETE CASCADE,
        FOREIGN KEY (plant_id) REFERENCES plants(id) ON DELETE CASCADE,
        PRIMARY KEY (moisture_device_id, sensor_port)
        );""",
        """
        CREATE TABLE IF NOT EXISTS plant_sensor_data (
        plant_id INT,
        moisture REAL,
        light REAL, -- lumens? lux? what actually is this
        temp REAL,
        humidity REAL,
        ts TIMESTAMP NOT NULL,
        FOREIGN KEY (plant_id) REFERENCES plants(id),
        PRIMARY KEY (plant_id, ts)
        );"""
    };

    private Database()  {
        // Singleton, private constructor to prevent instantiation 
    }

    public synchronized static void init(PlantTrackerConfig config) throws PlantTrackerException {
        try {
            if (instance != null) {
                throw new PlantTrackerException("Database instance already initialized.");
            }
            instance = new Database();
            // User required to create database beforehand
            logger.info("Connecting to Postgres database " + config.dbHost + config.dbName);
            instance.connection = DriverManager.getConnection(config.dbHost + config.dbName, config.dbUser, config.dbPass);
            logger.info("Successfully connected to Postgres database " + config.dbHost + config.dbName);
        } catch (SQLException e) {
            throw new PlantTrackerException("Failed to connect to database.", e);
        } 
    }

    public synchronized static Database getInstance() throws PlantTrackerException {
        if (instance == null) {
            // Instance should not be accessed until configured with init() 
            throw new PlantTrackerException("Database instance not yet initialized. Must first configure connection with init()");
        }
        return instance;
    }

    public void createTables() throws PlantTrackerException {
        Statement st = null;
        lockDatabase();
        try {
            st = connection.createStatement();
            for (String sql : createTableQueries) {
                st.execute(sql);
            }
            st.close();
        } catch (SQLException e) {
            throw new PlantTrackerException("Failed to create tables.", e);
        } finally {
            unlockDatabase();
        }
    }

    public void lockDatabase() {
        logger.finest("Database LOCK.");
        dbLock.lock();
    }

    public void unlockDatabase() {
        logger.finest("Database UNLOCK.");
        dbLock.unlock();
    }

    public void resetAutoCommit() {
        try {
            if (connection != null && !connection.isClosed()) {
                // Re-enable auto commits after transactions
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.warning("Failed to reset auto commit: " + e.getMessage());
        }
    }

    public void rollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException e) {
            logger.warning("Error during rollback transaction: " + e.getMessage());
        }
    } 

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning("Failed to close database connection: " + e.getMessage());
        }
    }
}
