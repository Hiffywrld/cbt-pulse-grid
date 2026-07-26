package com.cbtpulsegrid.backend.result.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CandidateResultResponse(
		UUID candidateId,
		String firstName,
		String lastName,
		String email,
		String registrationNumber,
		UUID attemptId,
		CandidateResultStatus attemptStatus,
		BigDecimal score,
		BigDecimal maximumScore,
		BigDecimal percentage,
		Boolean passed,
		Instant startedAt,
		Instant submittedAt
) {
}
