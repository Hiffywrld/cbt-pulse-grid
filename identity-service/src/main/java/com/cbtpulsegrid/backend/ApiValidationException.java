package com.cbtpulsegrid.backend;

/**
 * A validation failure whose message is deliberately safe to return to an API client.
 */
public class ApiValidationException extends RuntimeException {

	public ApiValidationException(String message) {
		super(message);
	}

	public ApiValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
