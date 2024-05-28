package ca.planttracker;

import androidx.annotation.NonNull;

public class Plant {
    private int id;
    private String name;
    private String imageUrl;

    // TODO add ref to sensors & light/moisture settings?

    public Plant(int id, @NonNull String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() { return name; }

    public String getImageUrl() { return imageUrl; }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
