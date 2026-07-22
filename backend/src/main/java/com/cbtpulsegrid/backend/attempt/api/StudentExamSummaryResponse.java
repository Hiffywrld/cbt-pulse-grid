package com.cbtpulsegrid.backend.attempt.api;

import java.time.Instant;
import java.util.UUID;

public record StudentExamSummaryResponse(
		UUID id,
		String code,
		String title,
		int durationMinutes,
		Instant startsAt,
		Instant endsAt,
		ExamAvailability availability
) {
}
