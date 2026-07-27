package com.cbtpulsegrid.backend.examination.api;

import java.time.Instant;
import java.util.UUID;

public record ExamCandidateResponse(
		UUID assignmentId,
		UUID userId,
		String firstName,
		String lastName,
		String email,
		String registrationNumber,
		String status,
		UUID assignedBy,
		Instant assignedAt
) {
}
