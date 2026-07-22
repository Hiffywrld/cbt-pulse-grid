package com.cbtpulsegrid.backend.monitoring.webhook;

import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.api.MonitoringActor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
class WebhookAuthorization {

	UUID requireInstitutionAdministrator(MonitoringActor actor) {
		if (actor == null
				|| actor.isSuperAdmin()
				|| !actor.roles().contains("INSTITUTION_ADMIN")
				|| actor.institutionId() == null) {
			throw new AccessDeniedException("Institution webhook administration is denied");
		}
		return actor.institutionId();
	}

	void requireTenant(UUID institutionId, UUID resourceInstitutionId) {
		if (!institutionId.equals(resourceInstitutionId)) {
			throw new AccessDeniedException("Cross-institution webhook access is denied");
		}
	}
}
