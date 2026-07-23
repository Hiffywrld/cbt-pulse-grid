package com.cbtpulsegrid.backend.result.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StaffAttemptResultResponse(
		UUID attemptId,
		UUID examId,
		String examCode,
		String examTitle,
		UUID candidateId,
		String firstName,
		String lastName,
		String email,
		String registrationNumber,
		CandidateResultStatus status,
		BigDecimal score,
		BigDecimal maximumScore,
		BigDecimal percentage,
		Boolean passed,
		Instant startedAt,
		Instant submittedAt,
		boolean reviewAvailable,
		List<StaffQuestionReviewResponse> questions
) {
}
