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
import org.apache.commons.cli.*;

public class App {

    public static void main(String[] args) throws SQLException {
        PlantListenerServer server = new PlantListenerServer();

        // take config from cmd line args
        try {
            parseArgs(args);
        } catch (ParseException e) {
            System.err.println("Parse exception poo");
        }
    
        // TODO parse config file, instantiate config for session
        PlantTrackerConfig config = new PlantTrackerConfig(null);

        // Create db connection with config
        // Connect to database and create tables if needed.
        Database db = new Database(config);
        db.init();
  
        try {
        server.start();
        server.blockUntilShutdown();

        // TODO close db
        } catch (Exception e) {
        System.err.println("Failed to run with " + e.toString());
        }
    }

    private static void parseArgs(String[] args) throws ParseException {

        Options options = new Options();
        options.addOption("c", "config", true, "File path of desired server config.");
        options.addOption("l", "log-level", true, "Set log level of server, defaults to INFO.");
        options.addOption("h", "help", false, "Display this help message and exit.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("planttracker.jar -c <config> [options]", options);
            System.exit(0);
        }

        if (cmd.hasOption("c") && cmd.getOptionValue("c") == null) {
            // Throw custom exception, config arg required 
        }
        // Start server with provided config 
        String configPath = cmd.getOptionValue("c");
    }

}
