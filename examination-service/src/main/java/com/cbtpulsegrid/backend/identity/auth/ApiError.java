package com.cbtpulsegrid.backend.identity.auth;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		UUID requestId,
		Map<String, String> validationErrors
) {
}
