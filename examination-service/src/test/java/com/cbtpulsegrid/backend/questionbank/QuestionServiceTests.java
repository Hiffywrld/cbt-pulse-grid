package com.cbtpulsegrid.backend.questionbank;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.institution.InstitutionService;
import com.cbtpulsegrid.backend.questionbank.api.QuestionBankActor;
import com.cbtpulsegrid.backend.questionbank.api.QuestionOptionRequest;
import com.cbtpulsegrid.backend.questionbank.api.UpdateQuestionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTests {

	private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER_INSTITUTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID SUBJECT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	@Mock
	private QuestionRepository questionRepository;

	@Mock
	private SubjectService subjectService;

	@Mock
	private InstitutionService institutionService;
	@Mock
	private AuditTrail auditTrail;

	private QuestionService questionService;

	@BeforeEach
	void createService() {
		questionService = new QuestionService(
				questionRepository,
				subjectService,
				institutionService,
				new QuestionStructureValidator(),
				new QuestionBankAuthorization(),
				auditTrail
		);
	}

	@Test
	void publishesAValidDraftQuestion() {
		UUID questionId = UUID.randomUUID();
		Question question = validSingleChoiceQuestion(INSTITUTION_ID);
		when(questionRepository.findById(questionId)).thenReturn(java.util.Optional.of(question));
		when(subjectService.requireActiveSubject(INSTITUTION_ID, SUBJECT_ID))
				.thenReturn(activeSubject(INSTITUTION_ID));
		when(questionRepository.saveAndFlush(question)).thenReturn(question);

		var response = questionService.changeStatus(examiner(INSTITUTION_ID), questionId, QuestionStatus.PUBLISHED);

		assertEquals(QuestionStatus.PUBLISHED, question.getStatus());
		assertEquals(QuestionStatus.PUBLISHED, response.status());
	}

	@Test
	void atomicallyReplacesQuestionOptionsDuringUpdate() {
		UUID questionId = UUID.randomUUID();
		Question question = validSingleChoiceQuestion(INSTITUTION_ID);
		when(questionRepository.findById(questionId)).thenReturn(java.util.Optional.of(question));
		when(subjectService.requireActiveSubject(INSTITUTION_ID, SUBJECT_ID))
				.thenReturn(activeSubject(INSTITUTION_ID));
		when(questionRepository.saveAndFlush(question)).thenReturn(question);
		UpdateQuestionRequest request = new UpdateQuestionRequest(
				SUBJECT_ID,
				"Which values are prime?",
				QuestionType.MULTIPLE_CHOICE,
				QuestionDifficulty.MEDIUM,
				new BigDecimal("2.00"),
				List.of(
						new QuestionOptionRequest("Two", true, 1),
						new QuestionOptionRequest("Three", true, 2),
						new QuestionOptionRequest("Four", false, 3)
				)
		);

		var response = questionService.update(examiner(INSTITUTION_ID), questionId, request);

		assertEquals(3, question.getOptions().size());
		assertEquals("Two", question.getOptions().get(0).getOptionText());
		assertEquals("Three", question.getOptions().get(1).getOptionText());
		assertEquals("Four", question.getOptions().get(2).getOptionText());
		assertEquals(3, response.options().size());
	}

	@Test
	void rejectsCrossInstitutionQuestionAccess() {
		UUID questionId = UUID.randomUUID();
		Question question = validSingleChoiceQuestion(OTHER_INSTITUTION_ID);
		when(questionRepository.findById(questionId)).thenReturn(java.util.Optional.of(question));

		assertThrows(
				AccessDeniedException.class,
				() -> questionService.get(examiner(INSTITUTION_ID), questionId)
		);
	}

	@Test
	void passesPaginationAndAllFiltersToTheSpecificationQuery() {
		when(subjectService.requireOwnedSubject(INSTITUTION_ID, SUBJECT_ID))
				.thenReturn(activeSubject(INSTITUTION_ID));
		when(questionRepository.findAll(
				any(Specification.class),
				any(Pageable.class)
		)).thenReturn(Page.empty(PageRequest.of(1, 25)));

		questionService.list(
				examiner(INSTITUTION_ID),
				SUBJECT_ID,
				QuestionType.SINGLE_CHOICE,
				QuestionDifficulty.HARD,
				QuestionStatus.DRAFT,
				"  algebra  ",
				1,
				25
		);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(questionRepository).findAll(
				any(Specification.class),
				pageable.capture()
		);
		assertEquals(1, pageable.getValue().getPageNumber());
		assertEquals(25, pageable.getValue().getPageSize());
	}

	private static Question validSingleChoiceQuestion(UUID institutionId) {
		Question question = new Question(
				institutionId,
				SUBJECT_ID,
				UUID.randomUUID(),
				"Which value is prime?",
				QuestionType.SINGLE_CHOICE,
				QuestionDifficulty.EASY,
				BigDecimal.ONE,
				QuestionStatus.DRAFT
		);
		question.replaceOptions(List.of(
				new QuestionOption("Two", true, 1),
				new QuestionOption("Four", false, 2)
		));
		return question;
	}

	private static Subject activeSubject(UUID institutionId) {
		return new Subject(institutionId, "MAT-101", "Mathematics", null, SubjectStatus.ACTIVE);
	}

	private static QuestionBankActor examiner(UUID institutionId) {
		return new QuestionBankActor(UUID.randomUUID(), institutionId, Set.of("EXAMINER"));
	}
}
