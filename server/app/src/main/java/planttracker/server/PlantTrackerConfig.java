package planttracker.server;

import org.json.JSONException;
import org.json.JSONObject;

import planttracker.server.exceptions.PlantTrackerException;

public class PlantTrackerConfig {

    public String host;
    public int trackerPort;
    public int listenerPort;
    public String logLevel;

    public String dbHost;
    public String dbUser;
    public String dbPass;
    public String dbName;

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
        } catch (JSONException e) {
            throw new PlantTrackerException("Missing expected config value.", e);
        }
    }
}
