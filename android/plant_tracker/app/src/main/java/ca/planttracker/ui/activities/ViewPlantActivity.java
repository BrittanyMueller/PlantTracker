package ca.planttracker.ui.activities;

import static java.lang.Double.max;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.StorageReference;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import ca.planttracker.PlantTrackerClient;
import ca.planttracker.ui.graph.BarGraph;
import ca.planttracker.ui.graph.GraphBase;
import ca.planttracker.ui.graph.LineGraph;
import ca.planttracker.data.models.Plant;
import ca.planttracker.R;
import planttracker.server.PlantSensorData;

public class ViewPlantActivity extends BaseActivity {

    private List<String> days = new ArrayList<>();
    private Plant plant;
    private TextView lightText;
    private TextView moistureText;
    private TextView humidityText;

    // graphs
    private BarGraph lightGraph;
    private LineGraph moistureGraph;

    private List<BarGraph.DataPoint> lightData = new ArrayList<>();

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plant_activity);

        plant = (Plant) getIntent().getSerializableExtra("plant");
        assert plant != null;
        initCustomToolbar(false, plant.getName(), Optional.of(R.menu.plant_menu));

        // Use Glide to load the image from Firebase
        ImageView plantImage = findViewById(R.id.plant_image_view);
        Glide.with(getBaseContext())
                .load(plant.getStorageReference())
                .placeholder(R.drawable.plant_placeholder)  // Fallback image
                .into(plantImage);

        lightText = findViewById(R.id.light_level);
        moistureText = findViewById(R.id.moisture_level);
        humidityText = findViewById(R.id.humidity_level);
        lightGraph = findViewById(R.id.light_bar_graph);
        moistureGraph = findViewById(R.id.moisture_graph);

        refreshData();
    }

    private void refreshData() {
        if (plant.hasLastData()) {
            // Needed for v1. v2 is actually good lol
            // int R = 1000;
            // float Vin = 3.3f;
            // float conversionFactor = 3.3f/255;
            // float Vout = (float)plant.getLastLight() * conversionFactor;
            // float Rout = R * (Vin / Vout - 1);
            // float lux = 100 * 1/(Rout/ 100000);

            lightText.setText(String.format("%d \nLux", (int)max(0, plant.getLastLight())));
            moistureText.setText(String.format("%.2f%%\nMoisture", plant.getLastMoisture()));
            humidityText.setText(String.format("%.2f%%\nHumidity", plant.getLastHumidity()));
        } else {
            lightText.setText("Unknown\nLumens");
            moistureText.setText("Unknown\nMoisture");
            humidityText.setText("Unknown\nHumidity");
        }

        switch(plant.getLightLevel()) {
            case LOW:
                lightGraph.setDataTarget(4);
                break;
            case MED:
                lightGraph.setDataTarget(6);
                break;
            case HIGH:
                lightGraph.setDataTarget(8);
                break;
        }


        // Fetch and calculate the data in another thread.
        new Thread(() -> {
            Instant start = LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant end = Instant.now();
            List<PlantSensorData> sensorDataList = PlantTrackerClient.getInstance().getPlantSensorData(plant.getId(), start, end);

            Calendar cal = Calendar.getInstance();
            cal.get(Calendar.DAY_OF_WEEK);
            LocalDate day = LocalDate.now().minusDays(6);

            List<BarGraph.DataPoint> lightData = new ArrayList<>();
            List<LineGraph.DataPoint> moistureData = new ArrayList<>();

            for (int i = 0; i < 7; i++) {
                days.add(day.getDayOfWeek().name().substring(0, 1));
                lightData.add(new BarGraph.DataPoint(days.get(days.size() - 1), 0));
                day = day.plusDays(1);

                // Have a data point every 12 hours, so 2 points per day
                moistureData.add(new GraphBase.DataPoint(days.get(days.size() -1), 0));
                moistureData.add(new GraphBase.DataPoint(days.get(days.size() -1) + ".5", 0));
            }

            if (!sensorDataList.isEmpty()) {
                // First we need to figure out how many data pointer were above our threshold
                // and what the average time between the data points is.
                long averageSum = 0;
                long lastTs = sensorDataList.get(0).getEpochTs();
                LocalDate nowDay = LocalDate.now();
                for (PlantSensorData d: sensorDataList) {
                    averageSum += d.getEpochTs() - lastTs;
                    lastTs = d.getEpochTs();

                    Instant curTs = Instant.ofEpochMilli(d.getEpochTs());

                    // todo change to UTC when server is updated
                    int curDay = (int)(curTs.atZone(ZoneOffset.systemDefault()).toLocalDate().toEpochDay() - nowDay.toEpochDay() + 6);
                    int curHour = curTs.atZone(ZoneOffset.systemDefault()).toLocalTime().getHour();
                    // TODO get threshold based on light level.
                    if (d.getLight().getLumens() > 500) {
                        lightData.get(curDay).value += 1;
                    }

                    // To ensure we don't error out be careful with incoming TS as timestamps are annoying
                    int i = (int)(curDay*2 + ((curHour <= 12) ? 0 : 1));
                    if (i >= moistureData.size()) {
                        Log.e("PlantActivity", "Timestamp curHour " + String.valueOf(curHour) + " Went out of bounds");
                        continue; // bad ts
                    }
                    moistureData.get(i).value = d.getMoisture().getMoistureLevel() * 100;
                }

                // Now calculate how many milliseconds each data point above the threshold is worth.
                long timeMilliModifier = averageSum / sensorDataList.size();
                Log.i("averageSum", String.valueOf(timeMilliModifier));

                // Finally multiply the data by the lightModify and convert it into hours
                for (int i = 0; i < lightData.size(); i++) {
                    // 1 hours = 3600000 milli
                    lightData.get(i).value = (lightData.get(i).value * timeMilliModifier) / 3600000.0;

                }
            }

            // If it is before 12 remove the last data point as it doesn't exist yet.
            // maybe this should be in the graph code...
            if (LocalDateTime.now().getHour() < 12) {
                moistureData.remove(moistureData.size()-1);
            }

            runOnUiThread(() -> {
                lightGraph.setData(lightData);
                moistureGraph.setData(moistureData);
            });
        }).start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.plant_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.refresh_menu_item) {
            refreshData();
        } else if (item.getItemId() == R.id.delete_menu_item) {
            PlantTrackerClient.getInstance().deletePlant(plant.getId());
            // TODO(qawse3dr) add toast on failure.
            finish();
        } else if (item.getItemId() == R.id.edit_menu_item) {
            // Pass intent to populate edit form
            Intent intent = new Intent(this, PlantFormActivity.class);
            intent.putExtra("plant", plant);
            startActivity(intent);
        }
        return true;
    }
}
