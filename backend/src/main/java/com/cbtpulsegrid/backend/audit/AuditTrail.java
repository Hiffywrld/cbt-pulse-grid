package com.cbtpulsegrid.backend.audit;

import java.util.Map;
import java.util.UUID;

public interface AuditTrail {

	void record(
			UUID institutionId,
			AuditAction action,
			AuditResourceType resourceType,
			UUID resourceId,
			Map<String, ?> metadata
	);
}
