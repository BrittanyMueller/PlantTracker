package ca.planttracker;

public class MoistureDevice {
    private long id;
    private String name;
    private int availablePorts[];

    public MoistureDevice(long id, String name, int[] availablePorts) {
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

    public int[] getAvailablePorts() {
        return availablePorts;
    }

    @Override
    public String toString() {
        return name;    // Name to be displayed in dropdown
    }
}
