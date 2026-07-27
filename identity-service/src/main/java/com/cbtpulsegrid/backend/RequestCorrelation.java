package com.cbtpulsegrid.backend;

import java.util.Optional;
import java.util.UUID;

public final class RequestCorrelation {

	public static final String HEADER_NAME = "X-Request-Id";
	public static final String REQUEST_ATTRIBUTE = RequestCorrelation.class.getName() + ".requestId";
	public static final String MDC_KEY = "requestId";

	private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

	private RequestCorrelation() {
	}

	public static Optional<UUID> currentId() {
		return Optional.ofNullable(CURRENT.get());
	}

	static void set(UUID requestId) {
		CURRENT.set(requestId);
	}

	static void clear() {
		CURRENT.remove();
	}
}
