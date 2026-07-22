package com.cbtpulsegrid.backend.attempt;

public class AttemptExpiredException extends AttemptConflictException {

	public AttemptExpiredException() {
		super("Attempt has expired and was submitted automatically");
	}
}
