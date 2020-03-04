package com.iisquare.fs.web.admin.mvc;

public class PermitException extends Exception {

    public PermitException() {
    }

    public PermitException(String message) {
        super(message);
    }

    public PermitException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermitException(Throwable cause) {
        super(cause);
    }

    public PermitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
