/*
 * (C) Copyright 2023-2024 Brittany Mueller and Larry Milne (https://www.larrycloud.ca)
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

import planttracker.server.exceptions.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.cli.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import java.util.logging.*;

public class App {

    private final static Logger logger = Logger.getGlobal(); 

    public static void main(String[] args) {

        Database db = null;

        try {
            // Take config from cmd line args
            PlantTrackerConfig config = parseArgs(args);

            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.ALL);    // Allow handler to output any level of log
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);

            if (!config.logFile.isEmpty()) {
                try {
                    FileHandler fileHandler = new FileHandler(config.logFile);
                    fileHandler.setLevel(Level.ALL);
                    logger.addHandler(fileHandler);
                } catch(Exception e) {
                    throw new PlantTrackerException("Failed to open log file " + config.logFile, e);
                }
            }

            // Set global loggers level, map our log levels to Java's
            switch (config.logLevel) {
                case "MAX":
                case "DEBUG":
                    logger.setLevel(Level.ALL);
                    break;
                case "INFO":
                    logger.setLevel(Level.INFO);
                    break;
                case "WARN":
                    logger.setLevel(Level.WARNING);
                    break;
                case "ERR":
                    logger.setLevel(Level.SEVERE);
                    break;
                default:
                    throw new PlantTrackerException("Invalid log level, must be one of MAX, DEBUG, INFO, WARN, ERR.");
            }
            // Initialize singleton database with config
            Database.init(config);
            db = Database.getInstance();
            db.createTables();  // Create tables if not exists


            // Configure Firebase
            FirebaseOptions options;
            try {
                FileInputStream credentialFile = new FileInputStream(config.firebaseCredentialFile);
                options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(credentialFile)).build();
            } catch (IOException e) {
                throw new PlantTrackerException("Failed to initialize firebase", e);
            }
            FirebaseApp.initializeApp(options);

            
            PlantListenerServer listener = new PlantListenerServer(config);
            listener.start();
            
            PlantTrackerServer tracker = new PlantTrackerServer(config, listener);
            tracker.start();

            tracker.blockUntilShutdown();
            listener.blockUntilShutdown();
            
        } catch (PlantTrackerException e) {
            logger.severe(e.getMessage());
            if (e.getCause() != null) {
                logger.severe(e.getCause().getMessage());
            }
            System.exit(1);
        } finally {
            // stop servers?
            if (db != null) {
                db.close();
            }
        }
    }

    private static JSONObject parseConfig(String filePath) throws PlantTrackerException {
        try {
            InputStream inputStream = new FileInputStream(filePath);
            JSONTokener tokener = new JSONTokener(inputStream);
            return new JSONObject(tokener);
        } catch (IOException e) {
            throw new PlantTrackerException("Failed to read config file.", e);
        } catch (JSONException e) {
            throw new PlantTrackerException("Failed to parse config file.", e);
        }
    }

    private static PlantTrackerConfig parseArgs(String[] args) throws PlantTrackerException {

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

        if (cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("plant-tracker.jar -c <config> [options]", options);
            System.exit(0);
        }

        if (!cmd.hasOption('c') || cmd.hasOption('c') && cmd.getOptionValue('c') == null) {
            // Throw custom exception, config arg required 
            throw new PlantTrackerException("Missing required argument -c/--config <config>");

        } 
        // Start server with provided config 
        String configPath = cmd.getOptionValue('c');
        JSONObject configObj = parseConfig(configPath); 
        PlantTrackerConfig config = new PlantTrackerConfig(configObj.getJSONObject("planttracker"));

        if (cmd.hasOption('l')) {
            if (cmd.getOptionValue('l') == null) {
                throw new PlantTrackerException("Option '-l' requires an argument.");
            }
            config.logLevel = cmd.getOptionValue('l');
        }
        return config;
    }
}
