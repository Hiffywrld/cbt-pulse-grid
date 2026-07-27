package com.cbtpulsegrid.backend.questionbank.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionOptionRequest(
		@NotBlank @Size(max = 4000) String optionText,
		boolean correct,
		@Min(1) int displayOrder
) {
}
