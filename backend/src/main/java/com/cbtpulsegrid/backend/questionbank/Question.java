package com.cbtpulsegrid.backend.questionbank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "questions")
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "institution_id", nullable = false, updatable = false)
	private UUID institutionId;

	@Column(name = "subject_id", nullable = false)
	private UUID subjectId;

	@Column(name = "created_by", nullable = false, updatable = false)
	private UUID createdBy;

	@Column(name = "question_text", nullable = false, columnDefinition = "text")
	private String questionText;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private QuestionType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "difficulty", nullable = false, length = 30)
	private QuestionDifficulty difficulty;

	@Column(name = "marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal marks;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private QuestionStatus status;

	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder ASC")
	private List<QuestionOption> options = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected Question() {
	}

	public Question(
			UUID institutionId,
			UUID subjectId,
			UUID createdBy,
			String questionText,
			QuestionType type,
			QuestionDifficulty difficulty,
			BigDecimal marks,
			QuestionStatus status
	) {
		this.institutionId = institutionId;
		this.subjectId = subjectId;
		this.createdBy = createdBy;
		this.questionText = questionText;
		this.type = type;
		this.difficulty = difficulty;
		this.marks = marks;
		this.status = status;
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

	public void replaceOptions(List<QuestionOption> replacementOptions) {
		options.clear();
		replacementOptions.forEach(this::addOption);
	}

	private void addOption(QuestionOption option) {
		option.attachTo(this);
		options.add(option);
	}

	public UUID getId() {
		return id;
	}

	public UUID getInstitutionId() {
		return institutionId;
	}

	public UUID getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(UUID subjectId) {
		this.subjectId = subjectId;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public QuestionType getType() {
		return type;
	}

	public void setType(QuestionType type) {
		this.type = type;
	}

	public QuestionDifficulty getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(QuestionDifficulty difficulty) {
		this.difficulty = difficulty;
	}

	public BigDecimal getMarks() {
		return marks;
	}

	public void setMarks(BigDecimal marks) {
		this.marks = marks;
	}

	public QuestionStatus getStatus() {
		return status;
	}

	public void setStatus(QuestionStatus status) {
		this.status = status;
	}

	public List<QuestionOption> getOptions() {
		return Collections.unmodifiableList(options);
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
