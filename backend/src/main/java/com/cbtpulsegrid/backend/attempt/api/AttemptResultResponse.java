package com.cbtpulsegrid.backend.attempt.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.AttemptStatus;

public record AttemptResultResponse(
		UUID attemptId,
		AttemptStatus status,
		Instant submittedAt,
		BigDecimal score,
		BigDecimal maximumScore,
		BigDecimal percentage,
		Boolean passed
) {
}
