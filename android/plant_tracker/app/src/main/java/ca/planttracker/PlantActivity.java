package ca.planttracker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

import planttracker.server.LightLevel;
import planttracker.server.PlantSensorData;

public class PlantActivity extends AppBarActivity {

    private List<String> days = new ArrayList<>();
    private Plant plant;

    private List<BarGraph.DataPoint> lightData = new ArrayList<BarGraph.DataPoint>();
    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plant_activity);

        Plant plant = (Plant) getIntent().getSerializableExtra("plant");

        assert plant != null;
        createAppBar(false, plant.getName());

        if (plant.getImageUrl() != null) {
            ImageView plantImage = findViewById(R.id.plant_image_view);
            Glide.with(getBaseContext())
                    .load((plant.getStorageReference() != null) ?  plant.getStorageReference() : plant.getImageUrl())
                    .placeholder(R.drawable.plant_placeholder) // Optional placeholder image? not sure if just while loading
                    .into(plantImage);
        }

        TextView lightText = findViewById(R.id.light_level);
        TextView moistureText = findViewById(R.id.moisture_level);
        TextView humidityText = findViewById(R.id.humidity_level);

        if (plant.hasLastData()) {
            lightText.setText(String.format("%.2f \nLumens", plant.getLastLight()));
            moistureText.setText(String.format("%.2f%%\nMoisture", plant.getLastMoisture()));
            humidityText.setText(String.format("%.2f%%\nHumidity", plant.getLastHumidity()));
        } else {
            lightText.setText("Unknown\nLumens");
            moistureText.setText("Unknown\nMoisture");
            humidityText.setText("Unknown\nHumidity");
        }

        BarGraph lightGraph = findViewById(R.id.light_bar_graph);

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
            Instant start = LocalDate.now().minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = LocalDate.now().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
            List<PlantSensorData> sensorDataList = Client.getInstance().getPlantSensorData(plant.getId(), start, end);

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
                Instant now = Instant.now();
                LocalDate nowDay = LocalDate.now();
                for (PlantSensorData d: sensorDataList) {
                    averageSum += d.getEpochTs() - lastTs;
                    lastTs = d.getEpochTs();

                    Instant curTs = Instant.ofEpochMilli(d.getEpochTs());

                    int curDay = (int)(curTs.atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() - nowDay.toEpochDay() + 6);
                    int curHour = curTs.atZone(ZoneOffset.UTC).toLocalTime().getHour();
                    // TODO get threshold based on light level.
                    if (d.getLight().getLumens() > 10) {
                        lightData.get(curDay).value += 1;
                    }
                    moistureData.get((int)(curDay*2 + ((curHour <= 12) ? 0 : 1))).value = d.getMoisture().getMoistureLevel() * 100;
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

            runOnUiThread(() -> {
                lightGraph.setData(lightData);
                LineGraph moistureGraph = findViewById(R.id.moisture_graph);
                moistureGraph.setData(moistureData);
            });
        }).start();

    }
}
