package ca.planttracker;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_appbar);

        try {
            JSONArray objArray = parseJSONArray("plants.json");
            if (objArray != null) {
                ArrayList<Plant> plantList = new ArrayList<>();
                for (int i = 0; i < objArray.length(); i++) {
                    JSONObject plantObj = objArray.getJSONObject(i);
                    Log.i("TAG", plantObj.getString("name"));
                    Plant plant = new Plant(plantObj.getInt("id"), plantObj.getString("name"), plantObj.getString("imageUrl"));
                    plantList.add(plant);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private JSONArray parseJSONArray(String fileName) {
        AssetManager assets = getAssets();
        try {
            InputStream inputStream = assets.open(fileName);
            int fileSize = inputStream.available();
            byte[] buffer = new byte[fileSize];
            int bytes = inputStream.read(buffer);
            if (bytes == 0) {
                return null;
            }
            inputStream.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.getJSONArray("plants");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
