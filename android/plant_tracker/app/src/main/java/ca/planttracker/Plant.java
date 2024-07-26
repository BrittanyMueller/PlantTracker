package ca.planttracker;

import androidx.annotation.NonNull;
import planttracker.server.LightLevel;
import planttracker.server.PlantInfo;

public class Plant {
    private long id;
    private String name;
    private String imageUrl;
    private LightLevel lightLevel;
    private int minMoisture;
    private int minHumidity;
    private int pid;

    public Plant(PlantInfo plant) {
        this.id = plant.getId();
        this.name = plant.getName();
        this.imageUrl = plant.getImageUrl();

    }
    public Plant(int id, @NonNull String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getName() { return name; }

    public String getImageUrl() { return imageUrl; }
}
