package com.cbtpulsegrid.backend.attempt.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SyncAnswersRequest(
		@NotNull UUID syncId,
		@NotEmpty @Size(max = 500) List<@Valid SyncAnswerRequest> answers
) {
	public SyncAnswersRequest {
		answers = answers == null ? null : List.copyOf(answers);
	}
}
