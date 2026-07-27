package com.cbtpulsegrid.backend.examination.api;

import java.math.BigDecimal;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;

public record ExamPoolRuleResponse(
		UUID id,
		QuestionDifficulty difficulty,
		int questionCount,
		BigDecimal marksPerQuestion
) {
}
