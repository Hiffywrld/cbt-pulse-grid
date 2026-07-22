package com.cbtpulsegrid.backend.monitoring;

import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.api.MonitoringActor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
class MonitoringAuthorization {

	UUID requireStudent(MonitoringActor actor) {
		if (actor == null || !actor.isStudent()) {
			throw new AccessDeniedException("Student monitoring access is denied");
		}
		return requireInstitution(actor);
	}

	UUID requireStaff(MonitoringActor actor) {
		if (actor == null || actor.isSuperAdmin() || !actor.isStaff()) {
			throw new AccessDeniedException("Monitoring staff access is denied");
		}
		return requireInstitution(actor);
	}

	private static UUID requireInstitution(MonitoringActor actor) {
		if (actor.institutionId() == null) {
			throw new AccessDeniedException("Institution context is required");
		}
		return actor.institutionId();
	}
}
