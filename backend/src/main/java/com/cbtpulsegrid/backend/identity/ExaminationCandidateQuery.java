package com.cbtpulsegrid.backend.identity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ExaminationCandidateQuery {

	Map<UUID, CandidateProfile> requireActiveStudents(UUID institutionId, Set<UUID> userIds);

	Map<UUID, CandidateProfile> findByIds(Set<UUID> userIds);

	record CandidateProfile(
			UUID id,
			UUID institutionId,
			String firstName,
			String lastName,
			String email,
			String registrationNumber,
			UserStatus status
	) {
	}
}
