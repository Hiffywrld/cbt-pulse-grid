package com.cbtpulsegrid.backend.questionbank.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSubjectRequest(
		@NotBlank @Size(max = 50) String code,
		@NotBlank @Size(max = 150) String name,
		@Size(max = 4000) String description
) {
}
