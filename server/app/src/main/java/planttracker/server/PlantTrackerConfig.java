package planttracker.server;

import org.json.JSONException;
import org.json.JSONObject;

import planttracker.server.exceptions.PlantTrackerException;

public class PlantTrackerConfig {

    public String host;
    public int trackerPort;
    public int listenerPort;
    public String logLevel;
    public String logFile = ""; // If logFile is empty it won't be used.

    public String dbHost;
    public String dbUser;
    public String dbPass;
    public String dbName;

    public String firebaseCredentialFile;

    public PlantTrackerConfig(JSONObject config) throws PlantTrackerException {
        try {
            host = config.getString("host");
            trackerPort = config.getInt("tracker-port");
            listenerPort = config.getInt("listener-port");
            dbHost = config.getString("db-host");
            dbUser = config.getString("db-user");
            dbPass = config.getString("db-pass");
            dbName = config.getString("db-name");
            logLevel = config.getString("log-level");
            if (config.has("log-file")) {
                logFile = config.getString("log-file");
            }
            firebaseCredentialFile = config.getString("firebase-credentials");
        } catch (JSONException e) {
            throw new PlantTrackerException("Missing expected config value.", e);
        }
    }
}
