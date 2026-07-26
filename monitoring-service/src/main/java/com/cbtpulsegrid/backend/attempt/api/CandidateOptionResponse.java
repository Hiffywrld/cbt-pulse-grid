package com.cbtpulsegrid.backend.attempt.api;

import java.util.UUID;

public record CandidateOptionResponse(
		UUID id,
		String optionText,
		int displayOrder
) {
}
