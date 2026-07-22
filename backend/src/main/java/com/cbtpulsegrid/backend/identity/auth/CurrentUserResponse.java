package com.cbtpulsegrid.backend.identity.auth;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
		UUID id,
		String email,
		UUID institutionId,
		List<String> roles
) {
}
