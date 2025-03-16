package ca.planttracker.data.models;

import androidx.annotation.NonNull;

import java.util.List;

public class MoistureDevice {
    private final long id;
    private final String name;
    private final List<Integer> availablePorts;

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

    @NonNull
    @Override
    public String toString() {
        // Name to be displayed in dropdown
        return name;
    }
}
