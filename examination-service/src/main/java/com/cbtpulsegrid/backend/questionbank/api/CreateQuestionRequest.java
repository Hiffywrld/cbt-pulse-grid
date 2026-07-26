package com.cbtpulsegrid.backend.questionbank.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import com.cbtpulsegrid.backend.questionbank.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
		@NotNull UUID subjectId,
		@NotBlank @Size(max = 10000) String questionText,
		@NotNull QuestionType type,
		@NotNull QuestionDifficulty difficulty,
		@NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal marks,
		@NotNull @Size(min = 2) List<@Valid QuestionOptionRequest> options
) {
}
