package com.cbtpulsegrid.backend.identity.auth;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Safe profile for the authenticated user")
public record CurrentUserResponse(
		UUID id,
		String email,
		@Schema(description = "User's given name")
		String firstName,
		@Schema(description = "User's family name")
		String lastName,
		@Schema(nullable = true, description = "Institution registration number when assigned")
		String registrationNumber,
		@Schema(nullable = true, description = "Institution identifier, or null for a platform-scoped account")
		UUID institutionId,
		@Schema(nullable = true, description = "Institution name, or null for a platform-scoped account")
		String institutionName,
		@Schema(nullable = true, description = "Institution code, or null for a platform-scoped account")
		String institutionCode,
		List<String> roles
) {
}
