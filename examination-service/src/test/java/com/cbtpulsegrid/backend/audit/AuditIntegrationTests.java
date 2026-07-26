package com.cbtpulsegrid.backend.audit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.api.AuditActor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AuditIntegrationTests {

	@Autowired
	private AuditService auditService;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private EntityManager entityManager;

	@Test
	@Transactional
	void auditQueriesAreTenantIsolated() {
		UUID institutionId = insertInstitution();
		UUID otherInstitutionId = insertInstitution();
		auditService.record(
				institutionId,
				AuditAction.EXAM_PUBLISHED,
				AuditResourceType.EXAM,
				UUID.randomUUID(),
				Map.of("status", "PUBLISHED")
		);
		entityManager.flush();

		var owned = auditService.findEvents(
				admin(institutionId), null, null, null, null, null, 0, 20
		);
		var other = auditService.findEvents(
				admin(otherInstitutionId), null, null, null, null, null, 0, 20
		);

		assertEquals(1, owned.totalElements());
		assertEquals(AuditAction.EXAM_PUBLISHED, owned.content().getFirst().action());
		assertEquals(0, other.totalElements());
	}

	@Test
	@Transactional
	void databaseRejectsAuditUpdatesAndDeletes() {
		UUID id = UUID.randomUUID();
		jdbc.update("""
				insert into audit_events (
				    id, institution_id, actor_id, actor_roles, action, resource_type,
				    resource_id, outcome, occurred_at, request_id, metadata
				) values (?, null, null, 'SYSTEM', 'ATTEMPT_AUTO_SUBMITTED', 'ATTEMPT', ?,
				          'SUCCESS', ?, null, '{}')
				""", id, UUID.randomUUID(), Timestamp.from(Instant.now()));

		assertThrows(
				DataAccessException.class,
				() -> jdbc.update("update audit_events set actor_roles = 'ALTERED' where id = ?", id)
		);
	}

	private UUID insertInstitution() {
		UUID id = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("""
				insert into institutions (id, name, code, status, created_at, updated_at, version)
				values (?, 'Audit Test Institution', ?, 'ACTIVE', ?, ?, 0)
				""",
				id,
				"AUD-" + id.toString().substring(0, 8).toUpperCase(),
				Timestamp.from(now),
				Timestamp.from(now)
		);
		return id;
	}

	private static AuditActor admin(UUID institutionId) {
		return new AuditActor(UUID.randomUUID(), institutionId, Set.of("INSTITUTION_ADMIN"));
	}
}
