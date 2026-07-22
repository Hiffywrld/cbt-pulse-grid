package com.cbtpulsegrid.backend.attempt.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.AttemptStatus;

public record SyncAnswersResponse(
		UUID acknowledgedSyncId,
		List<SavedAnswerResponse> savedAnswers,
		Instant lastSavedAt,
		AttemptStatus status
) {
	public SyncAnswersResponse {
		savedAnswers = List.copyOf(savedAnswers);
	}
}
