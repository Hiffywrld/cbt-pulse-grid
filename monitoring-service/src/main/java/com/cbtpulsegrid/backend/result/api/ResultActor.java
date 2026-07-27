package com.cbtpulsegrid.backend.result.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;

public record ResultActor(UUID userId, UUID institutionId, Set<String> roles) {

	public ResultActor {
		roles = roles == null ? Set.of() : Set.copyOf(roles);
	}

	public boolean canReadStaffResults() {
		return institutionId != null
				&& (roles.contains("INSTITUTION_ADMIN") || roles.contains("EXAMINER"));
	}

	public boolean isExaminer() {
		return roles.contains("EXAMINER") && !roles.contains("INSTITUTION_ADMIN");
	}

	public static ResultActor from(Jwt jwt) {
		List<String> claims = jwt.getClaimAsStringList("roles");
		Set<String> roles = claims == null
				? Set.of()
				: claims.stream().collect(Collectors.toUnmodifiableSet());
		String institutionClaim = jwt.getClaimAsString("institutionId");
		return new ResultActor(
				UUID.fromString(jwt.getSubject()),
				institutionClaim == null ? null : UUID.fromString(institutionClaim),
				roles
		);
	}
}
