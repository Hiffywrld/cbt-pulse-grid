package com.cbtpulsegrid.backend.questionbank;

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
		name = "question_options",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_question_options_display_order",
				columnNames = {"question_id", "display_order"}
		)
)
public class QuestionOption {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private Question question;

	@Column(name = "option_text", nullable = false, columnDefinition = "text")
	private String optionText;

	@Column(name = "correct", nullable = false)
	private boolean correct;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	protected QuestionOption() {
	}

	public QuestionOption(String optionText, boolean correct, int displayOrder) {
		this.optionText = optionText;
		this.correct = correct;
		this.displayOrder = displayOrder;
	}

	void attachTo(Question question) {
		this.question = question;
	}

	public UUID getId() {
		return id;
	}

	public String getOptionText() {
		return optionText;
	}

	public boolean isCorrect() {
		return correct;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}
}
