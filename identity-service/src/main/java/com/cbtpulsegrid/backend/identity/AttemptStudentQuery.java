package com.cbtpulsegrid.backend.identity;

import java.util.UUID;

/**
 * Narrow identity-module contract used by the attempt module.
 */
public interface AttemptStudentQuery {

	StudentProfile requireActiveStudent(UUID institutionId, UUID userId);

	record StudentProfile(UUID id, UUID institutionId) {
	}
}
