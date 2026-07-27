package com.cbtpulsegrid.backend.institution.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateInstitutionRequest(
		@NotBlank @Size(max = 160) String name
) {
}
