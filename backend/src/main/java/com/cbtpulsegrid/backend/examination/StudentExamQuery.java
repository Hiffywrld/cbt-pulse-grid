package com.cbtpulsegrid.backend.examination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;

/**
 * Narrow examination-module contract used by student attempt delivery.
 */
public interface StudentExamQuery {

	List<StudentExamView> findAssignedPublishedExams(UUID institutionId, UUID candidateId);

	StudentExamView requireAssignedPublishedExam(UUID institutionId, UUID candidateId, UUID examId);

	AttemptExamDefinition requireStartableExam(
			UUID institutionId,
			UUID candidateId,
			UUID examId,
			String accessPin,
			Instant now
	);

	AttemptExamDefinition requireAssignedDefinition(UUID institutionId, UUID candidateId, UUID examId);

	record StudentExamView(
			UUID id,
			String code,
			String title,
			String instructions,
			int durationMinutes,
			Instant startsAt,
			Instant endsAt,
			BigDecimal maximumScore
	) {
		public StudentExamView(
				UUID id, String code, String title, String instructions,
				int durationMinutes, Instant startsAt, Instant endsAt
		) {
			this(id, code, title, instructions, durationMinutes, startsAt, endsAt, null);
		}
	}

	record AttemptExamDefinition(
			UUID id,
			UUID institutionId,
			UUID subjectId,
			String code,
			String title,
			String instructions,
			int durationMinutes,
			Instant startsAt,
			Instant endsAt,
			boolean shuffleQuestions,
			boolean shuffleOptions,
			BigDecimal passMarkPercentage,
			List<PoolRule> poolRules
	) {
		public AttemptExamDefinition {
			poolRules = List.copyOf(poolRules);
		}
	}

	record PoolRule(
			QuestionDifficulty difficulty,
			int questionCount,
			BigDecimal marksPerQuestion
	) {
	}
}
