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
        port = config.getInt("port");
        host = config.getString("host");
    }
}
