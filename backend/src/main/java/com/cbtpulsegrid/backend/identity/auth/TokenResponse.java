package com.cbtpulsegrid.backend.identity.auth;

import java.time.Instant;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		Instant expiresAt
) {
}
