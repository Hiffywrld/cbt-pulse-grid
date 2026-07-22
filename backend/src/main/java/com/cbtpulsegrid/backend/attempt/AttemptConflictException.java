package com.cbtpulsegrid.backend.attempt;

import com.cbtpulsegrid.backend.identity.ApiConflictException;

public class AttemptConflictException extends ApiConflictException {

	public AttemptConflictException(String message) {
		super(message);
	}
}
