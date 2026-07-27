package com.cbtpulsegrid.backend.attempt.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.AttemptStatus;

public record AttemptPackageResponse(
		UUID attemptId,
		UUID examId,
		String examCode,
		String title,
		String instructions,
		AttemptStatus status,
		Instant serverTime,
		Instant expiresAt,
		long remainingSeconds,
		List<CandidateQuestionResponse> questions,
		List<SavedAnswerResponse> answers
) {
	public AttemptPackageResponse {
		questions = List.copyOf(questions);
		answers = List.copyOf(answers);
	}
}
