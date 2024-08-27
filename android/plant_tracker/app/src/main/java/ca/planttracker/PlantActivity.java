package ca.planttracker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import planttracker.server.LightLevel;
import planttracker.server.PlantSensorData;

public class PlantActivity extends AppBarActivity {

    private List<String> days = new ArrayList<>();
    private Plant plant;

    private List<BarGraph.DataPoint> data = new ArrayList<BarGraph.DataPoint>();
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
                    .load(plant.getImageUrl())
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

        Calendar cal = Calendar.getInstance();
        cal.get(Calendar.DAY_OF_WEEK);

        LocalDate day = LocalDate.now().minusDays(6);

        List<BarGraph.DataPoint> data = new ArrayList<>();
        lightGraph.setDataMax(24);
        lightGraph.setDataMin(0);
        for (int i = 0; i < 7; i++) {
            days.add(day.getDayOfWeek().name().substring(0, 1));
            data.add(new BarGraph.DataPoint(days.get(days.size() - 1), 0));
            day = day.plusDays(1);
        }

        lightGraph.setData(data);
        lightGraph.setDataTarget(8);

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

        data.get(0).value = 2.4;
        data.get(1).value = 3.4;
        data.get(2).value = 5.4;
        data.get(3).value = 2.6;
        data.get(4).value = 3.4;
        data.get(5).value = 5.6;
        data.get(6).value = 2.9;

        // Fetch and calculate the data in another thread.
//        new Thread(() -> {
//            List<PlantSensorData> sensorDataList = Client.instance().getPlantSensorData(plant.getId(), LocalDateTime.now(), LocalDateTime.now().minusDays(7));
//            // Todo figure out a good light value for the plant (based on level???)
//            if (sensorDataList.isEmpty()) return;
//
//            // TODO find a better logical way to do this
//            PlantSensorData lastData = sensorDataList.get(0);
//            for (PlantSensorData d: sensorDataList) {
//
//               Instant.ofEpochSecond(d.getEpochTs());
//
//                lastData = d;
//            }
//            lightGraph.setData(data);
//        }).start();

    }
}
