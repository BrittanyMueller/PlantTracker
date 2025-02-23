package ca.planttracker.data.models;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
        private boolean mockData = false;
        private boolean lastDataSet;

    public Plant(PlantInfo data) {
        this.id = data.getId();
        this.name = data.getName();
        this.imageUrl = data.getImageUrl();
        this.lightLevel = data.getLightLevel();
        this.minMoisture = data.getMinMoisture();
        this.minHumidity = data.getMinHumidity();
        this.pid = data.getPid();
        this.lastDataSet = data.hasLastReport();

        if (lastDataSet) {
            lastLight = data.getLastReport().getLight().getLumens();
            lastHumidity = data.getLastReport().getHumidity();
            lastMoisture = data.getLastReport().getMoisture().getMoistureLevel() * 100;
        }
    }

    // Mock data model
    public Plant(int id, @NonNull String name, String imageUrl, LightLevel level) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.id = id;
        this.lightLevel = level;
        this.lastDataSet = true;
        this.lastMoisture = 40.2;
        this.lastLight = 200;
        this.lastHumidity = 30.2;
        this.mockData = true;
    }

    public long getId() { return id; }

    public String getName() { return name; }

    public String getImageUrl() { return imageUrl; }

    public LightLevel getLightLevel() { return lightLevel; };

    public int getMinMoisture() { return minMoisture; }
    public int getMinHumidity() { return minMoisture; }

    public boolean hasLastData() { return lastDataSet; }

    public StorageReference getStorageReference() {
        if (mockData) return null;
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        return FirebaseStorage.getInstance().getReference().child(imageUrl);
    }
    public double getLastMoisture() { return lastMoisture; }
    public double getLastHumidity() { return lastHumidity; }
    public double getLastLight() { return lastLight; }

}
