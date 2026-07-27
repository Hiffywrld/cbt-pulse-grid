package com.cbtpulsegrid.backend.examination;

import java.math.BigDecimal;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
		name = "exam_pool_rules",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_exam_pool_rules_exam_difficulty",
				columnNames = {"exam_id", "difficulty"}
		)
)
public class ExamPoolRule {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exam_id", nullable = false)
	private Exam exam;

	@Enumerated(EnumType.STRING)
	@Column(name = "difficulty", nullable = false, length = 30)
	private QuestionDifficulty difficulty;

	@Column(name = "question_count", nullable = false)
	private int questionCount;

	@Column(name = "marks_per_question", nullable = false, precision = 10, scale = 2)
	private BigDecimal marksPerQuestion;

	protected ExamPoolRule() {
	}

	public ExamPoolRule(QuestionDifficulty difficulty, int questionCount, BigDecimal marksPerQuestion) {
		this.difficulty = difficulty;
		this.questionCount = questionCount;
		this.marksPerQuestion = marksPerQuestion;
	}

	void attachTo(Exam exam) {
		this.exam = exam;
	}

	void update(int questionCount, BigDecimal marksPerQuestion) {
		this.questionCount = questionCount;
		this.marksPerQuestion = marksPerQuestion;
	}

	public UUID getId() {
		return id;
	}

	public QuestionDifficulty getDifficulty() {
		return difficulty;
	}

	public int getQuestionCount() {
		return questionCount;
	}

	public BigDecimal getMarksPerQuestion() {
		return marksPerQuestion;
	}
}
