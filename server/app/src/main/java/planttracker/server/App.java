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

import planttracker.server.exceptions.*;
import planttracker.server.exceptions.MissingArgumentException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import org.apache.commons.cli.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


public class App {

    public static void main(String[] args) throws SQLException {

        Database db = null;
        
        try {
            // take config from cmd line args
            // String configPath = parseArgs(args);
            String configPath = "/workspaces/PlantTracker/server/app/src/main/resources/config.json";
            JSONObject configObj = parseConfig(configPath); 

            // TODO parse config file, instantiate config for session
            PlantTrackerConfig config = new PlantTrackerConfig(configObj.getJSONObject("planttracker"));

            // server needs config
            PlantListenerServer server = new PlantListenerServer(config);

            // Create db connection with config
            // Connect to database and create tables if needed.
            db = new Database(config);
            db.init();
  
            server.start();
            server.blockUntilShutdown();

        // TODO close db
        } catch (PlantTrackerException e) {
            System.err.println("Failed to run with " + e.toString());
            System.exit(1);
        } finally {
            // stop servers?
            if (db != null) {
                db.close();
            }
        }
    }

    private static JSONObject parseConfig(String filePath) {
        try {
            InputStream inputStream = new FileInputStream(filePath);
            JSONTokener tokener = new JSONTokener(inputStream);
            return new JSONObject(tokener);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String parseArgs(String[] args) throws PlantTrackerException {

        Options options = new Options();
        options.addOption("c", "config", true, "File path of desired server config.");
        options.addOption("l", "log-level", true, "Set log level of server, defaults to INFO.");
        options.addOption("h", "help", false, "Display this help message and exit.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            throw new PlantTrackerException("Failed to parse cli", e);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("plant-tracker.jar -c <config> [options]", options);
            System.exit(0);
        }

        if (cmd.hasOption("c") && cmd.getOptionValue("c") == null) {
            // Throw custom exception, config arg required 
            throw new MissingArgumentException("test");

        }
        // Start server with provided config 
        String configPath = cmd.getOptionValue("c");
        return configPath;
    }
}
