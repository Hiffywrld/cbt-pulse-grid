package com.cbtpulsegrid.backend.audit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events")
class AuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "institution_id", updatable = false)
	private UUID institutionId;

	@Column(name = "actor_id", updatable = false)
	private UUID actorId;

	@Column(name = "actor_roles", nullable = false, length = 255, updatable = false)
	private String actorRoles;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false, length = 80, updatable = false)
	private AuditAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "resource_type", nullable = false, length = 60, updatable = false)
	private AuditResourceType resourceType;

	@Column(name = "resource_id", updatable = false)
	private UUID resourceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "outcome", nullable = false, length = 20, updatable = false)
	private AuditOutcome outcome;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	@Column(name = "request_id", updatable = false)
	private UUID requestId;

	@Column(name = "metadata", nullable = false, length = 2000, updatable = false)
	private String metadata;

	protected AuditEvent() {
	}

	AuditEvent(
			UUID institutionId,
			UUID actorId,
			String actorRoles,
			AuditAction action,
			AuditResourceType resourceType,
			UUID resourceId,
			AuditOutcome outcome,
			Instant occurredAt,
			UUID requestId,
			String metadata
	) {
		this.institutionId = institutionId;
		this.actorId = actorId;
		this.actorRoles = actorRoles;
		this.action = action;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.outcome = outcome;
		this.occurredAt = occurredAt;
		this.requestId = requestId;
		this.metadata = metadata;
	}

	UUID getId() {
		return id;
	}

	UUID getInstitutionId() {
		return institutionId;
	}

	UUID getActorId() {
		return actorId;
	}

	String getActorRoles() {
		return actorRoles;
	}

	AuditAction getAction() {
		return action;
	}

	AuditResourceType getResourceType() {
		return resourceType;
	}

	UUID getResourceId() {
		return resourceId;
	}

	AuditOutcome getOutcome() {
		return outcome;
	}

	Instant getOccurredAt() {
		return occurredAt;
	}

	UUID getRequestId() {
		return requestId;
	}

	String getMetadata() {
		return metadata;
	}
}
