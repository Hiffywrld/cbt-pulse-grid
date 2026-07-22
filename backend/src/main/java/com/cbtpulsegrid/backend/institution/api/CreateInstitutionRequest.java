package com.cbtpulsegrid.backend.institution.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInstitutionRequest(
		@NotBlank @Size(max = 160) String name,
		@NotBlank @Size(max = 32) String code
) {
}
