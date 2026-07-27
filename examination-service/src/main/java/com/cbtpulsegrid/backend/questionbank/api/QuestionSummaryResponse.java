package com.cbtpulsegrid.backend.questionbank.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import com.cbtpulsegrid.backend.questionbank.QuestionStatus;
import com.cbtpulsegrid.backend.questionbank.QuestionType;

public record QuestionSummaryResponse(
		UUID id,
		UUID institutionId,
		UUID subjectId,
		UUID createdBy,
		String questionText,
		QuestionType type,
		QuestionDifficulty difficulty,
		BigDecimal marks,
		QuestionStatus status,
		Instant createdAt,
		Instant updatedAt,
		long version
) {
}
