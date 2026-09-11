package net.wanhe.edusystem.exception;

public class StuException extends Exception {

    public StuException() {
    }

    public StuException(String message) {
        super(message);
    }

    public StuException(String message, Throwable cause) {
        super(message, cause);
    }

    public StuException(Throwable cause) {
        super(cause);
    }

    public StuException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
