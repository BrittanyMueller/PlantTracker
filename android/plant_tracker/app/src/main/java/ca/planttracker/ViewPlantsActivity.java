package ca.planttracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.Manifest;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

public class ViewPlantsActivity extends AppBarActivity {

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

        createAppBar(true, getString(R.string.app_name));

        SwipeRefreshLayout refresh = findViewById(R.id.swiperefresh);
        refresh.setOnRefreshListener(this::refreshViewPlants);
        refreshViewPlants();

        FloatingActionButton fab = findViewById(R.id.addPlantButton);
        fab.setOnClickListener((View v) -> {
                Intent intent = new Intent(ViewPlantsActivity.this, AddPlantActivity.class);
                startActivity(intent);
        });

        ListView plantListView = findViewById(R.id.plants_listview);
        plantListView.setOnItemClickListener((AdapterView<?> l, View v, int position, long id) -> {
            Plant plant = (Plant) l.getAdapter().getItem(position);
            Intent intent = new Intent(ViewPlantsActivity.this, PlantActivity.class);
            intent.putExtra("plant", plant);
            startActivity(intent);
        });
    }


    private void refreshViewPlants() {
        new Thread(() -> {
            boolean error = false;
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String serverAddress = pref.getString("server_ip", "10.0.2.2");
            int serverPort = Integer.parseInt(pref.getString("server_port", "5050"));
            try {
                client.connect(getApplicationContext(), serverAddress, serverPort);
                plants = client.getPlants(false);
            } catch (Exception e) {
                Log.e("TAG", "Failed to connect to server with " + e);
                error = true;
            }


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

            // If we got any plants subscribe for notification
            for (Plant plant: plants) {
                // If the preference manager already has this no need to subscribe.
//                if (pref.contains("plant-id-" + plant.getId())) {
//                    continue;
//                }

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
                            }
                        });
            }
        }).start();
    }

}
