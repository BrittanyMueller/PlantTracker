package ca.planttracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.Manifest;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
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

    private final Client client = Client.getInstance();
    private List<Plant> plants = new ArrayList<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_plants_activity);
        askNotificationPermission();

        SwipeRefreshLayout refresh = findViewById(R.id.swiperefresh);
        refresh.setOnRefreshListener(this::refreshViewPlants);
        refreshViewPlants();

        FloatingActionButton fab = findViewById(R.id.addPlantButton);
        fab.setOnClickListener((View v) -> {
                Intent intent = new Intent(ViewPlants.this, AddPlantActivity.class);
                startActivity(intent);
        });

        ImageView hamburger = findViewById(R.id.hamburger_menu);
        hamburger.setOnClickListener((View v) -> {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.openDrawer(GravityCompat.START);
        });

        NavigationView navView = findViewById(R.id.navigation);
        navView.setNavigationItemSelectedListener((MenuItem item) -> {
            Intent intent = null;
            switch(item.getItemId()) {
                case R.id.home_menu:
                    intent = new Intent(ViewPlants.this, ViewPlants.class);
                    break;
                case R.id.setting_menu:
                    intent = new Intent(ViewPlants.this, Settings.class);
            }
            startActivity(intent);
            return true; // TODO(qawse3dr) what does this return do
        });

    }


    private void refreshViewPlants() {
        new Thread(() -> {
            boolean error = false;
            try {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String serverAddress = pref.getString("server_ip", "10.0.2.2");
                int serverPort = Integer.parseInt(pref.getString("server_port", "5050"));

                if (serverAddress.equals("0.0.0.0")) {
                    Log.i("RefreshPlants", "Fetching JSON plants for 0.0.0.0");
                    plants = getPlantData();
                } else {
                    Log.i("RefreshPlants", "Connecting to server at " + serverAddress);
                    client.connect(serverAddress, serverPort);
                    plants = client.getPlants(false);
                }

                for (Plant plant: plants) {
                    // TODO(qawse3dr) Eventually this will come from settings.

                    // If the preference manager already has this no need to subscribe.
                    if (pref.contains("plant-id-" + plant.getId())) {
                        continue;
                    }

                    FirebaseMessaging.getInstance().subscribeToTopic("plant-id-" + plant.getId())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    String msg = "Subscribed";
                                    if (!task.isSuccessful()) {
                                        msg = "Subscribe failed";
                                    } else {
                                        SharedPreferences.Editor editor = pref.edit();
                                        editor.putBoolean("plant-id-" + plant.getId(), true);
                                        editor.apply();
                                    }
                                    Toast.makeText(ViewPlants.this, msg, Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } catch (Exception e) {
                // TODO this should be handled in Client
                Log.e("RefreshPlants", "Failed to connect to server with " + e);
                error = true;
            } finally {
                boolean finalError = error;
                runOnUiThread(() -> {
                    // Create list view with plant data
                    PlantListAdapter plantAdapter = new PlantListAdapter(this, plants);
                    ListView listView = findViewById(R.id.plants_listview);
                    listView.setAdapter(plantAdapter);

                    // Automatically displays message when plant list is empty
                    TextView emptyView = findViewById(R.id.plants_empty_message);
                    if (finalError) {
                        plants.clear();
                        emptyView.setText(R.string.connection_error);
                    } else {
                        emptyView.setText(R.string.no_plants_found);
                    }
                    listView.setEmptyView(emptyView);

                    // in case we are refreshing finally set it to remove the refresh icon
                    SwipeRefreshLayout refresh = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
                    refresh.setRefreshing(false);
                });
            }
        }).start();
    }

    private List<Plant> getPlantData() {
        List<Plant> plantList = new ArrayList<>();
        try {
            JSONArray objArray = parseJSONArray("plants.json");
            if (objArray != null) {
                for (int i = 0; i < objArray.length(); i++) {
                    JSONObject plantObj = objArray.getJSONObject(i);
                    Plant plant = new Plant(plantObj.getInt("id"), plantObj.getString("name"), plantObj.getString("imageUrl"));
                    plantList.add(plant);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Log.i("GetPlantsJSON", "Fetching JSON plants.");
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

}
