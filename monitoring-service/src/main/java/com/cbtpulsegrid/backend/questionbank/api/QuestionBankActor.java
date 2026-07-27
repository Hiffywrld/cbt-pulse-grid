package com.cbtpulsegrid.backend.questionbank.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;

public record QuestionBankActor(
		UUID userId,
		UUID institutionId,
		Set<String> roles
) {

	public QuestionBankActor {
		roles = roles == null ? Set.of() : Set.copyOf(roles);
	}

	public boolean isInstitutionAdmin() {
		return roles.contains("INSTITUTION_ADMIN");
	}

	public boolean isExaminer() {
		return roles.contains("EXAMINER");
	}

	public boolean isSuperAdmin() {
		return roles.contains("SUPER_ADMIN");
	}

	public static QuestionBankActor from(Jwt jwt) {
		List<String> roleClaims = jwt.getClaimAsStringList("roles");
		Set<String> roles = roleClaims == null
				? Set.of()
				: roleClaims.stream().collect(Collectors.toUnmodifiableSet());
		String institutionClaim = jwt.getClaimAsString("institutionId");
		UUID institutionId = institutionClaim == null ? null : UUID.fromString(institutionClaim);
		return new QuestionBankActor(UUID.fromString(jwt.getSubject()), institutionId, roles);
	}
}
