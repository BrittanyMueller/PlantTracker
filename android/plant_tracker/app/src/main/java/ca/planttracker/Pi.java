package ca.planttracker;

import java.util.ArrayList;
import java.util.List;

public class Pi {
    private long id;
    private String name;
    private List<MoistureDevice> moistureDevices;

    public Pi(long id, String name, List<MoistureDevice> moistureDevices) {
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

    public List<MoistureDevice> getMoistureDevices() {
        return moistureDevices;
    }

    @Override
    public String toString() {
        // ArrayAdapter uses toString to display item
        return name;
    }
}
