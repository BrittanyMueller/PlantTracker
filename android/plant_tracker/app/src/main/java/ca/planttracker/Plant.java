package ca.planttracker;

public class Plant {
    private int id;
    private String name;
    private String imageUrl;

    // TODO add ref to sensors & light/moisture settings?

    public Plant(int id, String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
