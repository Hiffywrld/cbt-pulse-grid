package com.cbtpulsegrid.backend.questionbank.api;

import com.cbtpulsegrid.backend.questionbank.SubjectStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeSubjectStatusRequest(
		@NotNull SubjectStatus status
) {
}
