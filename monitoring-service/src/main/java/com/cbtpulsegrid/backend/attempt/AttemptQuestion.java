package com.cbtpulsegrid.backend.attempt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import com.cbtpulsegrid.backend.questionbank.QuestionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "attempt_questions",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uq_attempt_questions_position",
						columnNames = {"attempt_id", "position"}
				),
				@UniqueConstraint(
						name = "uq_attempt_questions_source",
						columnNames = {"attempt_id", "source_question_id"}
				)
		}
)
public class AttemptQuestion {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "attempt_id", nullable = false, updatable = false)
	private UUID attemptId;

	@Column(name = "source_question_id", nullable = false, updatable = false)
	private UUID sourceQuestionId;

	@Column(name = "position", nullable = false, updatable = false)
	private int position;

	@Column(name = "question_text", nullable = false, columnDefinition = "text", updatable = false)
	private String questionText;

	@Enumerated(EnumType.STRING)
	@Column(name = "question_type", nullable = false, length = 30, updatable = false)
	private QuestionType questionType;

	@Enumerated(EnumType.STRING)
	@Column(name = "difficulty", nullable = false, length = 30, updatable = false)
	private QuestionDifficulty difficulty;

	@Column(name = "marks", nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal marks;

	@OneToMany(mappedBy = "attemptQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder ASC")
	private List<AttemptOption> options = new ArrayList<>();

	protected AttemptQuestion() {
	}

	public AttemptQuestion(
			UUID attemptId,
			UUID sourceQuestionId,
			int position,
			String questionText,
			QuestionType questionType,
			QuestionDifficulty difficulty,
			BigDecimal marks
	) {
		this.attemptId = attemptId;
		this.sourceQuestionId = sourceQuestionId;
		this.position = position;
		this.questionText = questionText;
		this.questionType = questionType;
		this.difficulty = difficulty;
		this.marks = marks;
	}

	public void addOption(AttemptOption option) {
		option.attachTo(this);
		options.add(option);
	}

	public UUID getId() {
		return id;
	}

	public UUID getAttemptId() {
		return attemptId;
	}

	UUID getSourceQuestionId() {
		return sourceQuestionId;
	}

	public int getPosition() {
		return position;
	}

	public String getQuestionText() {
		return questionText;
	}

	public QuestionType getQuestionType() {
		return questionType;
	}

	public QuestionDifficulty getDifficulty() {
		return difficulty;
	}

	public BigDecimal getMarks() {
		return marks;
	}

	public List<AttemptOption> getOptions() {
		return Collections.unmodifiableList(options);
	}
}
