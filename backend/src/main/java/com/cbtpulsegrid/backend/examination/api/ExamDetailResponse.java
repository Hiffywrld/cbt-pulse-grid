package com.cbtpulsegrid.backend.examination.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.examination.ExamStatus;

public record ExamDetailResponse(
		UUID id,
		UUID institutionId,
		UUID subjectId,
		UUID createdBy,
		String code,
		String title,
		String instructions,
		int durationMinutes,
		Instant startsAt,
		Instant endsAt,
		boolean accessPinConfigured,
		boolean shuffleQuestions,
		boolean shuffleOptions,
		ExamStatus status,
		List<ExamPoolRuleResponse> poolRules,
		Instant createdAt,
		Instant updatedAt,
		long version
) {
}
