package com.cbtpulsegrid.backend.identity.account;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.UserStatus;

public record UserResponse(
		UUID id,
		String firstName,
		String lastName,
		String email,
		UUID institutionId,
		Set<Role> roles,
		String registrationNumber,
		UserStatus status,
		Instant createdAt,
		Instant updatedAt,
		long version
) {
}
