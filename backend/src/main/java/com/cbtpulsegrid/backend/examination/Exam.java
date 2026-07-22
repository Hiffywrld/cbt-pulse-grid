package com.cbtpulsegrid.backend.examination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "exams",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_exams_institution_code",
				columnNames = {"institution_id", "code"}
		)
)
public class Exam {

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

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "instructions", columnDefinition = "text")
	private String instructions;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Column(name = "pass_mark_percentage", nullable = false, precision = 5, scale = 2)
	private BigDecimal passMarkPercentage;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	@Column(name = "access_pin_hash", nullable = false, length = 100)
	private String accessPinHash;

	@Column(name = "shuffle_questions", nullable = false)
	private boolean shuffleQuestions;

	@Column(name = "shuffle_options", nullable = false)
	private boolean shuffleOptions;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private ExamStatus status;

	@OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("difficulty ASC")
	private List<ExamPoolRule> poolRules = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected Exam() {
	}

	public Exam(
			UUID institutionId,
			UUID subjectId,
			UUID createdBy,
			String code,
			String title,
			String instructions,
			int durationMinutes,
			Instant startsAt,
			Instant endsAt,
			String accessPinHash,
			boolean shuffleQuestions,
			boolean shuffleOptions,
			ExamStatus status
	) {
		this(
				institutionId,
				subjectId,
				createdBy,
				code,
				title,
				instructions,
				durationMinutes,
				startsAt,
				endsAt,
				accessPinHash,
				shuffleQuestions,
				shuffleOptions,
				new BigDecimal("50.00"),
				status
		);
	}

	public Exam(
			UUID institutionId,
			UUID subjectId,
			UUID createdBy,
			String code,
			String title,
			String instructions,
			int durationMinutes,
			Instant startsAt,
			Instant endsAt,
			String accessPinHash,
			boolean shuffleQuestions,
			boolean shuffleOptions,
			BigDecimal passMarkPercentage,
			ExamStatus status
	) {
		this.institutionId = institutionId;
		this.subjectId = subjectId;
		this.createdBy = createdBy;
		this.code = code;
		this.title = title;
		this.instructions = instructions;
		this.durationMinutes = durationMinutes;
		this.passMarkPercentage = passMarkPercentage;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.accessPinHash = accessPinHash;
		this.shuffleQuestions = shuffleQuestions;
		this.shuffleOptions = shuffleOptions;
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

	void updateDefinition(
			UUID subjectId,
			String code,
			String title,
			String instructions,
			int durationMinutes,
			Instant startsAt,
			Instant endsAt,
			boolean shuffleQuestions,
			boolean shuffleOptions,
			BigDecimal passMarkPercentage
	) {
		this.subjectId = subjectId;
		this.code = code;
		this.title = title;
		this.instructions = instructions;
		this.durationMinutes = durationMinutes;
		this.passMarkPercentage = passMarkPercentage;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.shuffleQuestions = shuffleQuestions;
		this.shuffleOptions = shuffleOptions;
	}

	void replacePoolRules(List<ExamPoolRule> replacementRules) {
		Map<QuestionDifficulty, ExamPoolRule> replacements =
				replacementRules.stream().collect(Collectors.toMap(
						ExamPoolRule::getDifficulty,
						Function.identity()
				));
		poolRules.removeIf(existing -> !replacements.containsKey(existing.getDifficulty()));
		for (ExamPoolRule replacement : replacementRules) {
			ExamPoolRule existing = poolRules.stream()
					.filter(rule -> rule.getDifficulty() == replacement.getDifficulty())
					.findFirst()
					.orElse(null);
			if (existing == null) {
				addPoolRule(replacement);
			}
			else {
				existing.update(replacement.getQuestionCount(), replacement.getMarksPerQuestion());
			}
		}
	}

	private void addPoolRule(ExamPoolRule poolRule) {
		poolRule.attachTo(this);
		poolRules.add(poolRule);
	}

	void rotateAccessPin(String accessPinHash) {
		this.accessPinHash = accessPinHash;
	}

	boolean isAccessPinConfigured() {
		return accessPinHash != null && !accessPinHash.isBlank();
	}

	String accessPinHash() {
		return accessPinHash;
	}

	void setStatus(ExamStatus status) {
		this.status = status;
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

	public UUID getCreatedBy() {
		return createdBy;
	}

	public String getCode() {
		return code;
	}

	public String getTitle() {
		return title;
	}

	public String getInstructions() {
		return instructions;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public BigDecimal getPassMarkPercentage() {
		return passMarkPercentage;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public boolean isShuffleQuestions() {
		return shuffleQuestions;
	}

	public boolean isShuffleOptions() {
		return shuffleOptions;
	}

	public ExamStatus getStatus() {
		return status;
	}

	public List<ExamPoolRule> getPoolRules() {
		return Collections.unmodifiableList(poolRules);
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
