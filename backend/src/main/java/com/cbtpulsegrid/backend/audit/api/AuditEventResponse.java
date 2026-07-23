package com.cbtpulsegrid.backend.audit.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditAction;
import com.cbtpulsegrid.backend.audit.AuditOutcome;
import com.cbtpulsegrid.backend.audit.AuditResourceType;

public record AuditEventResponse(
		UUID id,
		UUID institutionId,
		UUID actorId,
		String actorRoles,
		AuditAction action,
		AuditResourceType resourceType,
		UUID resourceId,
		AuditOutcome outcome,
		Instant occurredAt,
		UUID requestId,
		Map<String, Object> metadata
) {
}
