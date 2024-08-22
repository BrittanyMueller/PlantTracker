package ca.planttracker;

public class Pi {
    private long id;
    private String name;
    private MoistureDevice[] moistureDevices;

    public Pi(long id, String name, MoistureDevice[] moistureDevices) {
        this.id = id;
        this.name = name;
        this.moistureDevices = moistureDevices;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MoistureDevice[] getMoistureDevices() {
        return moistureDevices;
    }

    @Override
    public String toString() {
        // ArrayAdapter uses toString to display item
        return name;
    }
}
