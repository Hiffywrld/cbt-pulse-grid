package com.cbtpulsegrid.backend.monitoring;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "monitoring_sync_batches",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_monitoring_sync_batches_attempt_sync",
				columnNames = {"attempt_id", "sync_id"}
		)
)
public class MonitoringSyncBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "institution_id", nullable = false, updatable = false)
	private UUID institutionId;

	@Column(name = "attempt_id", nullable = false, updatable = false)
	private UUID attemptId;

	@Column(name = "sync_id", nullable = false, updatable = false)
	private UUID syncId;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	protected MonitoringSyncBatch() {
	}

	public MonitoringSyncBatch(
			UUID institutionId,
			UUID attemptId,
			UUID syncId,
			Instant receivedAt
	) {
		this.institutionId = institutionId;
		this.attemptId = attemptId;
		this.syncId = syncId;
		this.receivedAt = receivedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getInstitutionId() {
		return institutionId;
	}

	public UUID getAttemptId() {
		return attemptId;
	}

	public UUID getSyncId() {
		return syncId;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}
}
