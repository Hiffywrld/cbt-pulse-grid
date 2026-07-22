package com.cbtpulsegrid.backend.questionbank;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AttemptQuestionBankQueryService implements AttemptQuestionBankQuery {

	private final QuestionRepository questionRepository;

	AttemptQuestionBankQueryService(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<QuestionSnapshot> findPublishedQuestionSnapshots(
			UUID institutionId,
			UUID subjectId,
			QuestionDifficulty difficulty
	) {
		return questionRepository.findAllWithOptionsByPool(
				institutionId,
				subjectId,
				difficulty,
				QuestionStatus.PUBLISHED
		).stream().map(question -> new QuestionSnapshot(
				question.getId(),
				question.getQuestionText(),
				question.getType(),
				question.getDifficulty(),
				question.getOptions().stream()
						.sorted(Comparator.comparingInt(QuestionOption::getDisplayOrder))
						.map(option -> new OptionSnapshot(
								option.getId(),
								option.getOptionText(),
								option.isCorrect(),
								option.getDisplayOrder()
						))
						.toList()
		)).toList();
	}
}
