package ca.planttracker;

import java.util.ArrayList;
import java.util.List;

public class MoistureDevice {
    private long id;
    private String name;
    private List<Integer> availablePorts;

    public MoistureDevice(long id, String name, List<Integer> availablePorts) {
        this.id = id;
        this.name = name;
        this.availablePorts = availablePorts;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getAvailablePorts() {
        return availablePorts;
    }

    @Override
    public String toString() {
        return name;    // Name to be displayed in dropdown
    }
}
