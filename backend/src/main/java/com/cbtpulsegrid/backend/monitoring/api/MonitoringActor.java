package com.cbtpulsegrid.backend.monitoring.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;

public record MonitoringActor(
		UUID userId,
		UUID institutionId,
		Set<String> roles
) {

	public MonitoringActor {
		roles = roles == null ? Set.of() : Set.copyOf(roles);
	}

	public boolean isStudent() {
		return roles.contains("STUDENT");
	}

	public boolean isStaff() {
		return roles.contains("INSTITUTION_ADMIN")
				|| roles.contains("EXAMINER")
				|| roles.contains("INVIGILATOR");
	}

	public boolean isSuperAdmin() {
		return roles.contains("SUPER_ADMIN");
	}

	public static MonitoringActor from(Jwt jwt) {
		List<String> roleClaims = jwt.getClaimAsStringList("roles");
		Set<String> roles = roleClaims == null
				? Set.of()
				: roleClaims.stream().collect(Collectors.toUnmodifiableSet());
		String institutionClaim = jwt.getClaimAsString("institutionId");
		UUID institutionId = institutionClaim == null ? null : UUID.fromString(institutionClaim);
		return new MonitoringActor(UUID.fromString(jwt.getSubject()), institutionId, roles);
	}
}
