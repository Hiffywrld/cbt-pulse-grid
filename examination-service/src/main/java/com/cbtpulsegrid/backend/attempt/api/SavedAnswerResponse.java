package com.cbtpulsegrid.backend.attempt.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SavedAnswerResponse(
		UUID attemptQuestionId,
		Set<UUID> selectedOptionIds,
		long clientSequence,
		Instant answeredAt
) {
	public SavedAnswerResponse {
		selectedOptionIds = Set.copyOf(selectedOptionIds);
	}
}
