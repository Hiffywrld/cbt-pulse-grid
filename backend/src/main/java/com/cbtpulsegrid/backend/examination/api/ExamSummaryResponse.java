package com.cbtpulsegrid.backend.examination.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.examination.ExamStatus;

public record ExamSummaryResponse(
		UUID id,
		UUID institutionId,
		UUID subjectId,
		String code,
		String title,
		int durationMinutes,
		Instant startsAt,
		Instant endsAt,
		boolean shuffleQuestions,
		boolean shuffleOptions,
		ExamStatus status,
		Instant createdAt,
		Instant updatedAt,
		long version,
		BigDecimal passMarkPercentage
) {

	public ExamSummaryResponse(
			UUID id,
			UUID institutionId,
			UUID subjectId,
			String code,
			String title,
			int durationMinutes,
			Instant startsAt,
			Instant endsAt,
			boolean shuffleQuestions,
			boolean shuffleOptions,
			ExamStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version
	) {
		this(
				id, institutionId, subjectId, code, title, durationMinutes, startsAt, endsAt,
				shuffleQuestions, shuffleOptions, status, createdAt, updatedAt, version,
				new BigDecimal("50.00")
		);
	}
}
