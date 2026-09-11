package net.wanhe.dorm.exception;

public class StayException extends Throwable {

    public StayException() {
    }

    public StayException(String message) {
        super(message);
    }

    public StayException(String message, Throwable cause) {
        super(message, cause);
    }

    public StayException(Throwable cause) {
        super(cause);
    }

    public StayException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
