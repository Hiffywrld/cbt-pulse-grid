package com.cbtpulsegrid.backend.examination.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;

public record ExamActor(
		UUID userId,
		UUID institutionId,
		Set<String> roles
) {

	public ExamActor {
		roles = roles == null ? Set.of() : Set.copyOf(roles);
	}

	public boolean canManage() {
		return roles.contains("INSTITUTION_ADMIN") || roles.contains("EXAMINER");
	}

	public boolean isExaminer() {
		return roles.contains("EXAMINER") && !roles.contains("INSTITUTION_ADMIN");
	}

	public boolean isInvigilator() {
		return roles.contains("INVIGILATOR");
	}

	public boolean isSuperAdmin() {
		return roles.contains("SUPER_ADMIN");
	}

	public static ExamActor from(Jwt jwt) {
		List<String> roleClaims = jwt.getClaimAsStringList("roles");
		Set<String> roles = roleClaims == null
				? Set.of()
				: roleClaims.stream().collect(Collectors.toUnmodifiableSet());
		String institutionClaim = jwt.getClaimAsString("institutionId");
		UUID institutionId = institutionClaim == null ? null : UUID.fromString(institutionClaim);
		return new ExamActor(UUID.fromString(jwt.getSubject()), institutionId, roles);
	}
}
