package com.cbtpulsegrid.backend.attempt;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "attempt_answers",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_attempt_answers_attempt_question",
				columnNames = {"attempt_id", "attempt_question_id"}
		)
)
public class AttemptAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "attempt_id", nullable = false, updatable = false)
	private UUID attemptId;

	@Column(name = "attempt_question_id", nullable = false, updatable = false)
	private UUID attemptQuestionId;

	@Column(name = "client_sequence", nullable = false)
	private long clientSequence;

	@Column(name = "answered_at", nullable = false)
	private Instant answeredAt;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
			name = "attempt_answer_selections",
			joinColumns = @JoinColumn(name = "attempt_answer_id")
	)
	@Column(name = "attempt_option_id", nullable = false)
	private Set<UUID> selectedOptionIds = new HashSet<>();

	protected AttemptAnswer() {
	}

	public AttemptAnswer(
			UUID attemptId,
			UUID attemptQuestionId,
			long clientSequence,
			Instant answeredAt,
			Set<UUID> selectedOptionIds
	) {
		this.attemptId = attemptId;
		this.attemptQuestionId = attemptQuestionId;
		this.clientSequence = clientSequence;
		this.answeredAt = answeredAt;
		this.selectedOptionIds = new HashSet<>(selectedOptionIds);
	}

	void replace(long clientSequence, Instant answeredAt, Set<UUID> selectedOptionIds) {
		this.clientSequence = clientSequence;
		this.answeredAt = answeredAt;
		this.selectedOptionIds.clear();
		this.selectedOptionIds.addAll(selectedOptionIds);
	}

	public UUID getId() {
		return id;
	}

	public UUID getAttemptId() {
		return attemptId;
	}

	public UUID getAttemptQuestionId() {
		return attemptQuestionId;
	}

	public long getClientSequence() {
		return clientSequence;
	}

	public Instant getAnsweredAt() {
		return answeredAt;
	}

	public Set<UUID> getSelectedOptionIds() {
		return Set.copyOf(selectedOptionIds);
	}
}
