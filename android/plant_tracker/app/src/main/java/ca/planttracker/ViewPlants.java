package ca.planttracker;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ViewPlants extends AppCompatActivity {

    private Client client;
    private List<Plant> plants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plants_listview_layout);

        // TODO testing grpc client
        client = new Client("10.0.2.2", 5050);
        plants = client.getPlantsByPi(2, false);

        // Create list view with plant data
        PlantListAdapter plantAdapter = new PlantListAdapter(this, plants);
        ListView listView = findViewById(R.id.plants_listview);
        listView.setAdapter(plantAdapter);

        // Automatically displays message when plant list is empty
        TextView emptyView = findViewById(R.id.plants_empty_message);
        listView.setEmptyView(emptyView);
    }

    private List<Plant> getPlantData() {
        List<Plant> plantList = new ArrayList<>();
        try {
            JSONArray objArray = parseJSONArray("plants.json");
            if (objArray != null) {
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
        return plantList;
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

    public void clickMe(View view) {
        EditText input = findViewById(R.id.inputField);
        Toast.makeText(this, input.getText().toString(), Toast.LENGTH_SHORT).show();
    }
}
