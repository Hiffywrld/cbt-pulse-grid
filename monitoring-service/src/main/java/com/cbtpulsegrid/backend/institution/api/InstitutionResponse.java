package com.cbtpulsegrid.backend.institution.api;

import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.institution.InstitutionStatus;

public record InstitutionResponse(
		UUID id,
		String name,
		String code,
		InstitutionStatus status,
		Instant createdAt,
		Instant updatedAt,
		long version
) {
}
