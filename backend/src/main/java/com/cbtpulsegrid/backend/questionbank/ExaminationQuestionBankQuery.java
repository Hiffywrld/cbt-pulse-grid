package com.cbtpulsegrid.backend.questionbank;

import java.util.UUID;

public interface ExaminationQuestionBankQuery {

	void requireActiveSubject(UUID institutionId, UUID subjectId);

	long countPublishedQuestions(
			UUID institutionId,
			UUID subjectId,
			QuestionDifficulty difficulty
	);
}
