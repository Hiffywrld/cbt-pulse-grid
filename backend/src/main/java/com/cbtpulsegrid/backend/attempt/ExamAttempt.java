package com.cbtpulsegrid.backend.attempt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
		name = "exam_attempts",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_exam_attempts_exam_candidate",
				columnNames = {"exam_id", "candidate_id"}
		)
)
public class ExamAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "institution_id", nullable = false, updatable = false)
	private UUID institutionId;

	@Column(name = "exam_id", nullable = false, updatable = false)
	private UUID examId;

	@Column(name = "candidate_id", nullable = false, updatable = false)
	private UUID candidateId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private AttemptStatus status;

	@Column(name = "device_id_hash", nullable = false, length = 64, updatable = false)
	private String deviceIdHash;

	@Column(name = "started_at", nullable = false, updatable = false)
	private Instant startedAt;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "submitted_at")
	private Instant submittedAt;

	@Column(name = "last_saved_at")
	private Instant lastSavedAt;

	@Column(name = "score", precision = 12, scale = 2)
	private BigDecimal score;

	@Column(name = "maximum_score", precision = 12, scale = 2)
	private BigDecimal maximumScore;

	@Column(name = "percentage", precision = 5, scale = 2)
	private BigDecimal percentage;

	@Column(name = "passed")
	private Boolean passed;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected ExamAttempt() {
	}

	public ExamAttempt(
			UUID institutionId,
			UUID examId,
			UUID candidateId,
			String deviceIdHash,
			Instant startedAt,
			Instant expiresAt
	) {
		this.institutionId = institutionId;
		this.examId = examId;
		this.candidateId = candidateId;
		this.status = AttemptStatus.IN_PROGRESS;
		this.deviceIdHash = deviceIdHash;
		this.startedAt = startedAt;
		this.expiresAt = expiresAt;
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

	void markSaved(Instant savedAt) {
		lastSavedAt = savedAt;
	}

	void complete(
			AttemptStatus completedStatus,
			Instant completedAt,
			BigDecimal score,
			BigDecimal maximumScore,
			BigDecimal percentage,
			boolean passed
	) {
		if (status != AttemptStatus.IN_PROGRESS) {
			return;
		}
		status = completedStatus;
		submittedAt = completedAt;
		this.score = score;
		this.maximumScore = maximumScore;
		this.percentage = percentage;
		this.passed = passed;
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

	public UUID getCandidateId() {
		return candidateId;
	}

	public AttemptStatus getStatus() {
		return status;
	}

	String getDeviceIdHash() {
		return deviceIdHash;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public Instant getLastSavedAt() {
		return lastSavedAt;
	}

	public BigDecimal getScore() {
		return score;
	}

	public BigDecimal getMaximumScore() {
		return maximumScore;
	}

	public BigDecimal getPercentage() {
		return percentage;
	}

	public Boolean getPassed() {
		return passed;
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
