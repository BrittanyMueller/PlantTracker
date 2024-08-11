package ca.planttracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.Manifest;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;

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

    // TODO(qawse3dr) we probably want to move these and make it prettier
    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {

                }
            });

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {

            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void connectToGrpc() {
        client = new Client("192.168.1.88", 5050);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plants_listview_layout);

        askNotificationPermission();
        FirebaseMessaging.getInstance().subscribeToTopic("plant-id-1")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        String msg = "Subscribed";
                        if (!task.isSuccessful()) {
                            msg = "Subscribe failed";
                        }
                        Toast.makeText(ViewPlants.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });


        new Thread(() -> {
            // TODO testing grpc client
            plants = client.getPlants(false);

            // Create list view with plant data
            PlantListAdapter plantAdapter = new PlantListAdapter(this, plants);
            ListView listView = findViewById(R.id.plants_listview);
            listView.setAdapter(plantAdapter);

            // Automatically displays message when plant list is empty
            TextView emptyView = findViewById(R.id.plants_empty_message);
            listView.setEmptyView(emptyView);
        }).start();

        FloatingActionButton fab = findViewById(R.id.addPlantButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewPlants.this, AddPlantActivity.class);
                startActivity(intent);
            }
        });
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
