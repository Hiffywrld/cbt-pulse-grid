package com.cbtpulsegrid.backend.attempt.api;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SyncAnswerRequest(
		@NotNull UUID attemptQuestionId,
		@NotNull @Size(max = 20) Set<@NotNull UUID> selectedOptionIds,
		@PositiveOrZero long clientSequence
) {
	public SyncAnswerRequest {
		selectedOptionIds = selectedOptionIds == null ? null : Set.copyOf(selectedOptionIds);
	}
}
