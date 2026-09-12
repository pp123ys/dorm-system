package net.wanhe.dormsystem.exception;

public class RoomException extends Throwable {

    public RoomException() {
    }

    public RoomException(String message) {
        super(message);
    }

    public RoomException(String message, Throwable cause) {
        super(message, cause);
    }

    public RoomException(Throwable cause) {
        super(cause);
    }

    public RoomException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
