package ca.planttracker.data.models;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;

import planttracker.server.LightLevel;
import planttracker.server.PlantInfo;

public class Plant implements Serializable {
        private final long id;
        private String name;
        private LightLevel lightLevel;
        private int minMoisture;
        private int minHumidity;
        private long pid;
        private long moistureDeviceId;
        private int sensorPort;
        private double lastMoisture;
        private double lastHumidity;
        private double lastLight;
        private final boolean lastDataSet;
        private final String imageUrl;

    public Plant(PlantInfo data) {
        this.id = data.getId();
        this.name = data.getName();
        this.lightLevel = data.getLightLevel();
        this.minMoisture = data.getMinMoisture();
        this.minHumidity = data.getMinHumidity();
        this.pid = data.getPid();
        this.moistureDeviceId = data.getMoistureDeviceId();
        this.sensorPort = data.getSensorPort();
        this.lastDataSet = data.hasLastReport();
        this.imageUrl = data.getImageUrl();

        if (lastDataSet) {
            lastLight = data.getLastReport().getLight().getLumens();
            lastHumidity = data.getLastReport().getHumidity();
            lastMoisture = data.getLastReport().getMoisture().getMoistureLevel() * 100;
        }
    }

    // Mock data model
    public Plant(int id, @NonNull String name, LightLevel level) {
        this.id = id;
        this.name = name;
        this.lightLevel = level;
        this.lastDataSet = true;
        this.lastMoisture = 40.2;
        this.lastLight = 200;
        this.lastHumidity = 30.2;
        this.imageUrl = null;
    }

    public long getId() { return id; }

    public String getName() { return name; }

    public LightLevel getLightLevel() { return lightLevel; };

    public int getMinMoisture() { return minMoisture; }
    public int getMinHumidity() { return minHumidity; }

    public long getPid() { return pid; }

    public boolean hasLastData() { return lastDataSet; }

    public String getImageUrl() { return imageUrl; }

    public StorageReference getStorageReference() {
        if (imageUrl != null) {
            try {
                return FirebaseStorage.getInstance().getReference().child(imageUrl);
            } catch (Exception e) {
                Log.e("PlantStorageReference", "failed to get image ref");
            }
        }
        return null;
    }
    public double getLastMoisture() { return lastMoisture; }
    public double getLastHumidity() { return lastHumidity; }
    public double getLastLight() { return lastLight; }
    public long getMoistureDeviceId() { return  moistureDeviceId;}
    public int getSensorPort() { return sensorPort;}
}
