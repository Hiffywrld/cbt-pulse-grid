package com.cbtpulsegrid.backend.questionbank.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import com.cbtpulsegrid.backend.questionbank.QuestionStatus;
import com.cbtpulsegrid.backend.questionbank.QuestionType;

/**
 * Staff-only authoring response. Candidate exam-delivery contracts must use a separate DTO that omits correct answers.
 */
public record StaffQuestionResponse(
		UUID id,
		UUID institutionId,
		UUID subjectId,
		UUID createdBy,
		String questionText,
		QuestionType type,
		QuestionDifficulty difficulty,
		BigDecimal marks,
		QuestionStatus status,
		List<StaffQuestionOptionResponse> options,
		Instant createdAt,
		Instant updatedAt,
		long version
) {
}
