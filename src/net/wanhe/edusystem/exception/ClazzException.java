package net.wanhe.edusystem.exception;

public class ClazzException extends Throwable {

    public ClazzException() {
    }

    public ClazzException(String message) {
        super(message);
    }

    public ClazzException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClazzException(Throwable cause) {
        super(cause);
    }

    public ClazzException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
