package com.cbtpulsegrid.backend.examination;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "exam_candidates",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_exam_candidates_exam_user",
				columnNames = {"exam_id", "user_id"}
		)
)
public class ExamCandidate {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "exam_id", nullable = false, updatable = false)
	private UUID examId;

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(name = "assigned_by", nullable = false, updatable = false)
	private UUID assignedBy;

	@Column(name = "assigned_at", nullable = false, updatable = false)
	private Instant assignedAt;

	protected ExamCandidate() {
	}

	public ExamCandidate(UUID examId, UUID userId, UUID assignedBy) {
		this.examId = examId;
		this.userId = userId;
		this.assignedBy = assignedBy;
	}

	@PrePersist
	void onCreate() {
		assignedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getExamId() {
		return examId;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getAssignedBy() {
		return assignedBy;
	}

	public Instant getAssignedAt() {
		return assignedAt;
	}
}
