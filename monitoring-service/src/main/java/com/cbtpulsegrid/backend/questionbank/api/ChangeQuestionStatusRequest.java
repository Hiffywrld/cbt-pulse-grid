package com.cbtpulsegrid.backend.questionbank.api;

import com.cbtpulsegrid.backend.questionbank.QuestionStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeQuestionStatusRequest(
		@NotNull QuestionStatus status
) {
}
