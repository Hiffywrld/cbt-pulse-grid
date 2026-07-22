package com.cbtpulsegrid.backend.identity;

/**
 * Shared API conflict signal for state conflicts that are not validation errors.
 */
public class ApiConflictException extends RuntimeException {

	public ApiConflictException(String message) {
		super(message);
	}
}
