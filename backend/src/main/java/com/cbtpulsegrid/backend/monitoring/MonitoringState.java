package com.cbtpulsegrid.backend.monitoring;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "monitoring_states",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_monitoring_states_attempt",
				columnNames = "attempt_id"
		)
)
public class MonitoringState {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "institution_id", nullable = false, updatable = false)
	private UUID institutionId;

	@Column(name = "exam_id", nullable = false, updatable = false)
	private UUID examId;

	@Column(name = "attempt_id", nullable = false, updatable = false)
	private UUID attemptId;

	@Column(name = "candidate_id", nullable = false, updatable = false)
	private UUID candidateId;

	@Column(name = "last_heartbeat_id")
	private UUID lastHeartbeatId;

	@Column(name = "last_client_sequence")
	private Long lastClientSequence;

	@Column(name = "last_client_timestamp")
	private Instant lastClientTimestamp;

	@Column(name = "last_heartbeat_received_at")
	private Instant lastHeartbeatReceivedAt;

	@Column(name = "last_connectivity_occurred_at")
	private Instant lastConnectivityOccurredAt;

	@Column(name = "focused")
	private Boolean focused;

	@Column(name = "fullscreen")
	private Boolean fullscreen;

	@Column(name = "online")
	private Boolean online;

	@Column(name = "event_count", nullable = false)
	private long eventCount;

	@Column(name = "risk_score", nullable = false)
	private int riskScore;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected MonitoringState() {
	}

	public MonitoringState(
			UUID institutionId,
			UUID examId,
			UUID attemptId,
			UUID candidateId
	) {
		this.institutionId = institutionId;
		this.examId = examId;
		this.attemptId = attemptId;
		this.candidateId = candidateId;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	boolean applyHeartbeat(
			UUID heartbeatId,
			long clientSequence,
			Instant clientTimestamp,
			Instant receivedAt,
			boolean focused,
			boolean fullscreen,
			boolean online
	) {
		if (lastClientSequence != null && clientSequence <= lastClientSequence) {
			return false;
		}
		lastHeartbeatId = heartbeatId;
		lastClientSequence = clientSequence;
		lastClientTimestamp = clientTimestamp;
		lastHeartbeatReceivedAt = receivedAt;
		this.focused = focused;
		this.fullscreen = fullscreen;
		applyConnectivity(online, clientTimestamp);
		return true;
	}

	void applyConnectivity(boolean online, Instant occurredAt) {
		if (lastConnectivityOccurredAt == null || occurredAt.isAfter(lastConnectivityOccurredAt)) {
			this.online = online;
			lastConnectivityOccurredAt = occurredAt;
		}
	}

	int recordEvent(int riskWeight) {
		eventCount++;
		int available = MonitoringRiskPolicy.MAX_RISK_SCORE - riskScore;
		int applied = Math.min(Math.max(available, 0), riskWeight);
		riskScore += applied;
		return applied;
	}

	public UUID getId() {
		return id;
	}

	public UUID getInstitutionId() {
		return institutionId;
	}

	public UUID getExamId() {
		return examId;
	}

	public UUID getAttemptId() {
		return attemptId;
	}

	public UUID getCandidateId() {
		return candidateId;
	}

	public UUID getLastHeartbeatId() {
		return lastHeartbeatId;
	}

	public Long getLastClientSequence() {
		return lastClientSequence;
	}

	public Instant getLastClientTimestamp() {
		return lastClientTimestamp;
	}

	public Instant getLastHeartbeatReceivedAt() {
		return lastHeartbeatReceivedAt;
	}

	public Instant getLastConnectivityOccurredAt() {
		return lastConnectivityOccurredAt;
	}

	public Boolean getFocused() {
		return focused;
	}

	public Boolean getFullscreen() {
		return fullscreen;
	}

	public Boolean getOnline() {
		return online;
	}

	public long getEventCount() {
		return eventCount;
	}

	public int getRiskScore() {
		return riskScore;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public long getVersion() {
		return version;
	}
}
