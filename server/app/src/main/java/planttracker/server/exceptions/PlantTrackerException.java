package planttracker.server.exceptions;

public class PlantTrackerException extends Exception {

    public PlantTrackerException() {
        super();
    }

    public PlantTrackerException(String message) {
        super(message);
    }

    public PlantTrackerException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlantTrackerException(Throwable cause) {
        super(cause);
    }
}
