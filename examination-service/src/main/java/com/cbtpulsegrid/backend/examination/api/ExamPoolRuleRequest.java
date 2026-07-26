package com.cbtpulsegrid.backend.examination.api;

import java.math.BigDecimal;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExamPoolRuleRequest(
		@NotNull QuestionDifficulty difficulty,
		@Min(1) int questionCount,
		@NotNull @DecimalMin(value = "0.0", inclusive = false)
		@Digits(integer = 8, fraction = 2) BigDecimal marksPerQuestion
) {
}
