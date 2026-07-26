package com.cbtpulsegrid.backend.questionbank;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ExaminationQuestionBankQueryService implements ExaminationQuestionBankQuery {

	private final SubjectService subjectService;
	private final QuestionRepository questionRepository;

	ExaminationQuestionBankQueryService(
			SubjectService subjectService,
			QuestionRepository questionRepository
	) {
		this.subjectService = subjectService;
		this.questionRepository = questionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public void requireActiveSubject(UUID institutionId, UUID subjectId) {
		subjectService.requireActiveSubject(institutionId, subjectId);
	}

	@Override
	@Transactional(readOnly = true)
	public long countPublishedQuestions(
			UUID institutionId,
			UUID subjectId,
			QuestionDifficulty difficulty
	) {
		return questionRepository.countByInstitutionIdAndSubjectIdAndDifficultyAndStatus(
				institutionId,
				subjectId,
				difficulty,
				QuestionStatus.PUBLISHED
		);
	}
}
