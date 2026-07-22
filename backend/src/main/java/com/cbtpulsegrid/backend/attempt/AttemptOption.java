package com.cbtpulsegrid.backend.attempt;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "attempt_options",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_attempt_options_display_order",
				columnNames = {"attempt_question_id", "display_order"}
		)
)
public class AttemptOption {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_question_id", nullable = false, updatable = false)
	private AttemptQuestion attemptQuestion;

	@Column(name = "source_option_id", updatable = false)
	private UUID sourceOptionId;

	@Column(name = "option_text", nullable = false, columnDefinition = "text", updatable = false)
	private String optionText;

	@Column(name = "display_order", nullable = false, updatable = false)
	private int displayOrder;

	@Column(name = "correct", nullable = false, updatable = false)
	private boolean correct;

	protected AttemptOption() {
	}

	public AttemptOption(UUID sourceOptionId, String optionText, int displayOrder, boolean correct) {
		this.sourceOptionId = sourceOptionId;
		this.optionText = optionText;
		this.displayOrder = displayOrder;
		this.correct = correct;
	}

	void attachTo(AttemptQuestion attemptQuestion) {
		this.attemptQuestion = attemptQuestion;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAttemptQuestionId() {
		return attemptQuestion.getId();
	}

	UUID getSourceOptionId() {
		return sourceOptionId;
	}

	public String getOptionText() {
		return optionText;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	boolean isCorrect() {
		return correct;
	}
}
