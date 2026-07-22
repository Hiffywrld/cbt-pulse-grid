package com.cbtpulsegrid.backend.institution.api;

import com.cbtpulsegrid.backend.institution.InstitutionStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeInstitutionStatusRequest(
		@NotNull InstitutionStatus status
) {
}
