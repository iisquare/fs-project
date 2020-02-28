package com.iisquare.jwframe.exception;

public class ApplicationException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public ApplicationException() {
		super();
	}

	public ApplicationException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ApplicationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ApplicationException(String message) {
		super(message);
	}

	public ApplicationException(Throwable cause) {
		super(cause);
	}
}
