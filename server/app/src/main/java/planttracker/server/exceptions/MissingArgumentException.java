package planttracker.server.exceptions;

public class MissingArgumentException extends PlantTrackerException {

    public MissingArgumentException() {
        super();
    }

    public MissingArgumentException(String message) {
        super(message);
    }

    public MissingArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingArgumentException(Throwable cause) {
        super(cause);
    }
}
