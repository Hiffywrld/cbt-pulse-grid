package com.cbtpulsegrid.backend.result.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionType;

public record StaffQuestionReviewResponse(
		UUID attemptQuestionId,
		int position,
		String questionText,
		QuestionType questionType,
		BigDecimal marks,
		BigDecimal awardedMarks,
		List<StaffOptionReviewResponse> options
) {
}
