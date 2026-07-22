package com.cbtpulsegrid.backend.attempt;

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
		name = "attempt_sync_batches",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_attempt_sync_batches_attempt_sync",
				columnNames = {"attempt_id", "sync_id"}
		)
)
public class AttemptSyncBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "attempt_id", nullable = false, updatable = false)
	private UUID attemptId;

	@Column(name = "sync_id", nullable = false, updatable = false)
	private UUID syncId;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	protected AttemptSyncBatch() {
	}

	public AttemptSyncBatch(UUID attemptId, UUID syncId, Instant receivedAt) {
		this.attemptId = attemptId;
		this.syncId = syncId;
		this.receivedAt = receivedAt;
	}

	public UUID getId() {
		return id;
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
