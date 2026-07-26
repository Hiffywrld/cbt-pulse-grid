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
		name = "monitoring_heartbeat_receipts",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_monitoring_heartbeat_receipts_attempt_heartbeat",
				columnNames = {"attempt_id", "heartbeat_id"}
		)
)
public class MonitoringHeartbeatReceipt {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "institution_id", nullable = false, updatable = false)
	private UUID institutionId;

	@Column(name = "attempt_id", nullable = false, updatable = false)
	private UUID attemptId;

	@Column(name = "heartbeat_id", nullable = false, updatable = false)
	private UUID heartbeatId;

	@Column(name = "client_sequence", nullable = false, updatable = false)
	private long clientSequence;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	protected MonitoringHeartbeatReceipt() {
	}

	public MonitoringHeartbeatReceipt(
			UUID institutionId,
			UUID attemptId,
			UUID heartbeatId,
			long clientSequence,
			Instant receivedAt
	) {
		this.institutionId = institutionId;
		this.attemptId = attemptId;
		this.heartbeatId = heartbeatId;
		this.clientSequence = clientSequence;
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

	public UUID getHeartbeatId() {
		return heartbeatId;
	}

	public long getClientSequence() {
		return clientSequence;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}
}
