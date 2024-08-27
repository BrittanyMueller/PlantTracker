package ca.planttracker;

import androidx.annotation.NonNull;

import java.io.Serializable;

import planttracker.server.LightLevel;
import planttracker.server.PlantInfo;

public class Plant implements Serializable {
        private long id;
        private String name;
        private String imageUrl;
        private LightLevel lightLevel;
        private int minMoisture;
        private int minHumidity;
        private long pid;
        private double lastMoisture;
        private double lastHumidity;
        private double lastLight;

        private boolean lastDataSet;

    public Plant(PlantInfo plant) {
        this.id = plant.getId();
        this.name = plant.getName();
        this.imageUrl = plant.getImageUrl();
        this.lightLevel = plant.getLightLevel();
        this.minMoisture = plant.getMinMoisture();
        this.minHumidity = plant.getMinHumidity();
        this.pid = plant.getPid();
        this.lastDataSet = plant.hasLastReport();

        if (lastDataSet) {
            lastLight = plant.getLastReport().getLight().getLumens();
            lastHumidity = plant.getLastReport().getHumidity();
            lastMoisture = plant.getLastReport().getMoisture().getMoistureLevel() * 100;
        }
    }
    public Plant(int id, @NonNull String name, String imageUrl, LightLevel level) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.id = id;
        this.lightLevel = level;
        this.lastDataSet = true;
        this.lastMoisture = 40.2;
        this.lastLight = 200;
        this.lastHumidity = 30.2;

    }

    public long getId() { return id; }

    public String getName() { return name; }

    public String getImageUrl() { return imageUrl; }

    public LightLevel getLightLevel() { return lightLevel; };

    public boolean hasLastData() { return lastDataSet; }

    public double getLastMoisture() { return lastMoisture;}
    public double getLastHumidity() { return lastHumidity;}
    public double getLastLight() { return lastLight;}
}
