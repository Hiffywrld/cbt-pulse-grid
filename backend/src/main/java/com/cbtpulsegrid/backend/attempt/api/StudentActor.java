package com.cbtpulsegrid.backend.attempt.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;

public record StudentActor(
		UUID userId,
		UUID institutionId,
		Set<String> roles
) {

	public StudentActor {
		roles = roles == null ? Set.of() : Set.copyOf(roles);
	}

	public boolean isStudent() {
		return roles.contains("STUDENT");
	}

	public static StudentActor from(Jwt jwt) {
		List<String> roleClaims = jwt.getClaimAsStringList("roles");
		Set<String> roles = roleClaims == null
				? Set.of()
				: roleClaims.stream().collect(Collectors.toUnmodifiableSet());
		String institutionClaim = jwt.getClaimAsString("institutionId");
		UUID institutionId = institutionClaim == null ? null : UUID.fromString(institutionClaim);
		return new StudentActor(UUID.fromString(jwt.getSubject()), institutionId, roles);
	}
}
