package com.cbtpulsegrid.backend.identity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Narrow identity-module contract used to revalidate WebSocket principals.
 */
public interface MonitoringPrincipalQuery {

	Optional<PrincipalView> findActive(UUID userId);

	record PrincipalView(
			UUID userId,
			UUID institutionId,
			Set<String> roles
	) {
		public PrincipalView {
			roles = Set.copyOf(roles);
		}
	}
}
