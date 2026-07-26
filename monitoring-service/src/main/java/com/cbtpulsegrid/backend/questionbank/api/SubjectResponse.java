package com.cbtpulsegrid.backend.questionbank.api;

import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.SubjectStatus;

public record SubjectResponse(
		UUID id,
		UUID institutionId,
		String code,
		String name,
		String description,
		SubjectStatus status,
		Instant createdAt,
		Instant updatedAt,
		long version
) {
}
