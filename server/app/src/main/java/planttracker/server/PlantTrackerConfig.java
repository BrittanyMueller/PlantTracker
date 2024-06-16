package planttracker.server;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PlantTrackerConfig {

    public int port;
    public String host;
    public String dbHost;
    public String dbUser;
    public String dbPass;
    public String dbName;

    public PlantTrackerConfig(JSONObject config) {
        if (config == null) {
            System.out.println("null config");
            // TODO only for testing, throw custom exception if bad config obj
            port = 5050;
            host = "127.0.0.1";
        } else {
            port = config.getInt("port");
            host = config.getString("host");
            dbHost = config.getString("db-host");
            dbUser = config.getString("db-user");
            dbPass = config.getString("db-pass");
            dbName = config.getString("db-name");
        }
    }
}