package com.iisquare.fs.web.core.rbac;

public class PermitException extends Exception {

    public static final String NO_PACKAGE = "class has no package";
    public static final String NAME_TOO_LONG = "name length great than 3";
    public static final String NAME_TOO_SHORT = "name length less than 1";
    public static final String REQUIRED_LOGIN = "required login";
    public static final String PERMIT_DENIED = "permit denied or resource is disabled";

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
