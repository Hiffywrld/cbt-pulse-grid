package com.cbtpulsegrid.backend.attempt.api;

import java.time.Instant;
import java.util.UUID;

public record StudentExamDetailResponse(
		UUID id,
		String code,
		String title,
		String instructions,
		int durationMinutes,
		Instant startsAt,
		Instant endsAt,
		ExamAvailability availability
) {
}
