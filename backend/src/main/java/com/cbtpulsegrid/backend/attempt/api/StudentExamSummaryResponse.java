package com.cbtpulsegrid.backend.attempt.api;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

public record StudentExamSummaryResponse(
		UUID id,
		String code,
		String title,
		int durationMinutes,
		Instant startsAt,
		Instant endsAt,
		ExamAvailability availability,
		String participationStatus,
		BigDecimal score,
		BigDecimal maximumScore,
		BigDecimal percentage,
		Boolean passed
) {
	public StudentExamSummaryResponse(
			UUID id, String code, String title, int durationMinutes,
			Instant startsAt, Instant endsAt, ExamAvailability availability
	) {
		this(id, code, title, durationMinutes, startsAt, endsAt, availability,
				null, null, null, null, null);
	}
}
