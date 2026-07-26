package com.cbtpulsegrid.backend.audit.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;

public record AuditActor(UUID userId, UUID institutionId, Set<String> roles) {

	public AuditActor {
		roles = roles == null ? Set.of() : Set.copyOf(roles);
	}

	public boolean isInstitutionAdministrator() {
		return roles.contains("INSTITUTION_ADMIN") && institutionId != null;
	}

	public static AuditActor from(Jwt jwt) {
		List<String> claims = jwt.getClaimAsStringList("roles");
		Set<String> roles = claims == null
				? Set.of()
				: claims.stream().collect(Collectors.toUnmodifiableSet());
		String institutionClaim = jwt.getClaimAsString("institutionId");
		return new AuditActor(
				UUID.fromString(jwt.getSubject()),
				institutionClaim == null ? null : UUID.fromString(institutionClaim),
				roles
		);
	}
}
