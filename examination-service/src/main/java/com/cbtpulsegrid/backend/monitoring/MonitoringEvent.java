package com.cbtpulsegrid.backend.monitoring;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
		name = "monitoring_events",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_monitoring_events_attempt_client_event",
				columnNames = {"attempt_id", "client_event_id"}
		)
)
public class MonitoringEvent {

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

	@Column(name = "client_event_id", nullable = false, updatable = false)
	private UUID clientEventId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40, updatable = false)
	private MonitoringEventType eventType;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata", nullable = false, columnDefinition = "jsonb", updatable = false)
	private Map<String, String> metadata = new LinkedHashMap<>();

	@Column(name = "risk_weight", nullable = false, updatable = false)
	private int riskWeight;

	@Column(name = "risk_points_applied", nullable = false, updatable = false)
	private int riskPointsApplied;

	protected MonitoringEvent() {
	}

	public MonitoringEvent(
			UUID institutionId,
			UUID examId,
			UUID attemptId,
			UUID candidateId,
			UUID clientEventId,
			MonitoringEventType eventType,
			Instant occurredAt,
			Instant receivedAt,
			Map<String, String> metadata,
			int riskWeight,
			int riskPointsApplied
	) {
		this.institutionId = institutionId;
		this.examId = examId;
		this.attemptId = attemptId;
		this.candidateId = candidateId;
		this.clientEventId = clientEventId;
		this.eventType = eventType;
		this.occurredAt = occurredAt;
		this.receivedAt = receivedAt;
		this.metadata = new LinkedHashMap<>(metadata);
		this.riskWeight = riskWeight;
		this.riskPointsApplied = riskPointsApplied;
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

	public UUID getClientEventId() {
		return clientEventId;
	}

	public MonitoringEventType getEventType() {
		return eventType;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}

	public Map<String, String> getMetadata() {
		return Map.copyOf(metadata);
	}

	public int getRiskWeight() {
		return riskWeight;
	}

	public int getRiskPointsApplied() {
		return riskPointsApplied;
	}
}
