package com.cbtpulsegrid.backend.audit;

import java.util.UUID;

import org.springframework.data.repository.Repository;

interface AuditEventRepository extends Repository<AuditEvent, UUID> {

	AuditEvent save(AuditEvent event);
}
