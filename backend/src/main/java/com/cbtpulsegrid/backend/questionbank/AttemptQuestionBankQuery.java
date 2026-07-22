package com.cbtpulsegrid.backend.questionbank;

import java.util.List;
import java.util.UUID;

/**
 * Server-side snapshot source for the attempt module. Correct-answer data is
 * intentionally confined to this internal delivery boundary and is never used
 * in candidate response DTOs.
 */
public interface AttemptQuestionBankQuery {

	List<QuestionSnapshot> findPublishedQuestionSnapshots(
			UUID institutionId,
			UUID subjectId,
			QuestionDifficulty difficulty
	);

	record QuestionSnapshot(
			UUID sourceQuestionId,
			String questionText,
			QuestionType type,
			QuestionDifficulty difficulty,
			List<OptionSnapshot> options
	) {
		public QuestionSnapshot {
			options = List.copyOf(options);
		}
	}

	record OptionSnapshot(
			UUID sourceOptionId,
			String optionText,
			boolean correct,
			int displayOrder
	) {
	}
}
