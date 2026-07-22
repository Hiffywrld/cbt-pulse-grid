package com.cbtpulsegrid.backend.questionbank;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExaminationQuestionBankQueryServiceTests {

	@Test
	void countsOnlyPublishedQuestionsForTheRequestedPool() {
		UUID institutionId = UUID.randomUUID();
		UUID subjectId = UUID.randomUUID();
		SubjectService subjectService = mock(SubjectService.class);
		QuestionRepository questionRepository = mock(QuestionRepository.class);
		when(questionRepository.countByInstitutionIdAndSubjectIdAndDifficultyAndStatus(
				institutionId,
				subjectId,
				QuestionDifficulty.HARD,
				QuestionStatus.PUBLISHED
		)).thenReturn(7L);
		ExaminationQuestionBankQueryService queryService = new ExaminationQuestionBankQueryService(
				subjectService,
				questionRepository
		);

		assertEquals(
				7L,
				queryService.countPublishedQuestions(institutionId, subjectId, QuestionDifficulty.HARD)
		);
		verify(questionRepository).countByInstitutionIdAndSubjectIdAndDifficultyAndStatus(
				institutionId,
				subjectId,
				QuestionDifficulty.HARD,
				QuestionStatus.PUBLISHED
		);
	}

	@Test
	void delegatesActiveSubjectValidationWithoutExposingTheSubjectEntity() {
		UUID institutionId = UUID.randomUUID();
		UUID subjectId = UUID.randomUUID();
		SubjectService subjectService = mock(SubjectService.class);
		QuestionRepository questionRepository = mock(QuestionRepository.class);
		ExaminationQuestionBankQueryService queryService = new ExaminationQuestionBankQueryService(
				subjectService,
				questionRepository
		);

		queryService.requireActiveSubject(institutionId, subjectId);

		verify(subjectService).requireActiveSubject(institutionId, subjectId);
	}
}
