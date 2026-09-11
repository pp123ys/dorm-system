package net.wanhe.dorm.exception;

public class BuildingException extends Throwable {

    public BuildingException() {
    }

    public BuildingException(String message) {
        super(message);
    }

    public BuildingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BuildingException(Throwable cause) {
        super(cause);
    }

    public BuildingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
