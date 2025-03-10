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

        // TODO storage ref not serializable, cant use like this
        private final StorageReference imageRef;

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

        if (lastDataSet) {
            lastLight = data.getLastReport().getLight().getLumens();
            lastHumidity = data.getLastReport().getHumidity();
            lastMoisture = data.getLastReport().getMoisture().getMoistureLevel() * 100;
        }
        if (data.getImageUrl() != null) {
            this.imageRef = FirebaseStorage.getInstance().getReference().child(data.getImageUrl());
        } else {
            this.imageRef = null;
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
        this.imageRef = null;
    }

    public long getId() { return id; }

    public String getName() { return name; }

    public LightLevel getLightLevel() { return lightLevel; };

    public int getMinMoisture() { return minMoisture; }
    public int getMinHumidity() { return minHumidity; }

    public long getPid() { return pid; }

    public boolean hasLastData() { return lastDataSet; }

    public StorageReference getStorageReference() {
        return imageRef;
    }
    public double getLastMoisture() { return lastMoisture; }
    public double getLastHumidity() { return lastHumidity; }
    public double getLastLight() { return lastLight; }
    public long getMoistureDeviceId() { return  moistureDeviceId;}
    public int getSensorPort() { return sensorPort;}
}
