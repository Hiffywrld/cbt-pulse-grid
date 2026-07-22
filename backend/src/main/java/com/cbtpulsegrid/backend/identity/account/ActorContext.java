package com.cbtpulsegrid.backend.identity.account;

import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.Role;

public record ActorContext(
		UUID userId,
		UUID institutionId,
		Set<Role> roles
) {

	public ActorContext {
		roles = roles == null ? Set.of() : Set.copyOf(roles);
	}

	public boolean isSuperAdmin() {
		return roles.contains(Role.SUPER_ADMIN);
	}

	public boolean isInstitutionAdmin() {
		return roles.contains(Role.INSTITUTION_ADMIN);
	}
}
