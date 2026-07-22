package com.cbtpulsegrid.backend.examination.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateExamRequest(
		@NotBlank @Size(max = 50) String code,
		@NotNull UUID subjectId,
		@NotBlank @Size(max = 200) String title,
		@Size(max = 10000) String instructions,
		@Min(1) @Max(480) int durationMinutes,
		@NotNull Instant startsAt,
		@NotNull Instant endsAt,
		boolean shuffleQuestions,
		boolean shuffleOptions,
		@NotEmpty @Size(max = 3) List<@Valid ExamPoolRuleRequest> poolRules
) {
}
