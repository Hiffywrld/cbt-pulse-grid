package com.cbtpulsegrid.backend.examination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.ApiValidationException;
import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.examination.api.AssignExamCandidatesRequest;
import com.cbtpulsegrid.backend.examination.api.CreateExamRequest;
import com.cbtpulsegrid.backend.examination.api.ExamActor;
import com.cbtpulsegrid.backend.examination.api.ExamPoolRuleRequest;
import com.cbtpulsegrid.backend.examination.api.UpdateExamRequest;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery.CandidateProfile;
import com.cbtpulsegrid.backend.identity.UserStatus;
import com.cbtpulsegrid.backend.institution.InstitutionService;
import com.cbtpulsegrid.backend.questionbank.ExaminationQuestionBankQuery;
import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTests {

	private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER_INSTITUTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID SUBJECT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID ACTOR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	private static final UUID CANDIDATE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

	@Mock
	private ExamRepository examRepository;
	@Mock
	private ExamCandidateRepository examCandidateRepository;
	@Mock
	private InstitutionService institutionService;
	@Mock
	private ExaminationQuestionBankQuery questionBankQuery;
	@Mock
	private ExaminationCandidateQuery candidateQuery;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AuditTrail auditTrail;

	private ExamService examService;

	@BeforeEach
	void createService() {
		examService = new ExamService(
				examRepository,
				examCandidateRepository,
				institutionService,
				questionBankQuery,
				candidateQuery,
				passwordEncoder,
				new ExamAuthorization(),
				auditTrail
		);
	}

	@Test
	void normalizesCodeAndHashesPinWithoutExposingIt() {
		when(passwordEncoder.encode("123456")).thenReturn("bcrypt-hash");
		when(examRepository.saveAndFlush(any(Exam.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = examService.create(manager(INSTITUTION_ID), createRequest("  mat-202  "));

		ArgumentCaptor<Exam> exam = ArgumentCaptor.forClass(Exam.class);
		verify(examRepository).saveAndFlush(exam.capture());
		verify(passwordEncoder).encode("123456");
		assertEquals("MAT-202", exam.getValue().getCode());
		assertTrue(response.accessPinConfigured());
		assertEquals(new BigDecimal("50.00"), response.passMarkPercentage());
		assertFalse(response.toString().contains("123456"));
		assertFalse(response.toString().contains("bcrypt-hash"));
	}

	@Test
	void rejectsDuplicateExamCode() {
		when(examRepository.existsByInstitutionIdAndCodeIgnoreCase(INSTITUTION_ID, "MAT-202"))
				.thenReturn(true);

		assertThrows(
				DuplicateKeyException.class,
				() -> examService.create(manager(INSTITUTION_ID), createRequest("mat-202"))
		);
		verify(examRepository, never()).saveAndFlush(any(Exam.class));
	}

	@Test
	void publishesDraftWhenCandidatesAndQuestionPoolsAreAvailable() {
		UUID examId = UUID.randomUUID();
		Exam exam = exam(examId, INSTITUTION_ID, ExamStatus.DRAFT, 2);
		when(examRepository.findWithPoolRulesById(examId)).thenReturn(Optional.of(exam));
		when(examCandidateRepository.countByExamId(examId)).thenReturn(1L);
		when(questionBankQuery.countPublishedQuestions(
				INSTITUTION_ID,
				SUBJECT_ID,
				QuestionDifficulty.EASY
		)).thenReturn(2L);
		when(examRepository.saveAndFlush(exam)).thenReturn(exam);

		var response = examService.publish(manager(INSTITUTION_ID), examId);

		assertEquals(ExamStatus.PUBLISHED, response.status());
		assertEquals(ExamStatus.PUBLISHED, exam.getStatus());
	}

	@Test
	void rejectsPublishingWhenPoolAvailabilityIsInsufficient() {
		UUID examId = UUID.randomUUID();
		Exam exam = exam(examId, INSTITUTION_ID, ExamStatus.DRAFT, 3);
		when(examRepository.findWithPoolRulesById(examId)).thenReturn(Optional.of(exam));
		when(examCandidateRepository.countByExamId(examId)).thenReturn(1L);
		when(questionBankQuery.countPublishedQuestions(
				INSTITUTION_ID,
				SUBJECT_ID,
				QuestionDifficulty.EASY
		)).thenReturn(2L);

		assertThrows(
				ApiValidationException.class,
				() -> examService.publish(manager(INSTITUTION_ID), examId)
		);
		verify(examRepository, never()).saveAndFlush(exam);
	}

	@Test
	void closesOnlyPublishedExams() {
		UUID examId = UUID.randomUUID();
		Exam published = exam(examId, INSTITUTION_ID, ExamStatus.PUBLISHED, 2);
		when(examRepository.findWithPoolRulesById(examId)).thenReturn(Optional.of(published));
		when(examRepository.saveAndFlush(published)).thenReturn(published);

		assertEquals(ExamStatus.CLOSED, examService.close(manager(INSTITUTION_ID), examId).status());

		Exam draft = exam(UUID.randomUUID(), INSTITUTION_ID, ExamStatus.DRAFT, 2);
		when(examRepository.findWithPoolRulesById(draft.getId())).thenReturn(Optional.of(draft));
		assertThrows(
				IllegalArgumentException.class,
				() -> examService.close(manager(INSTITUTION_ID), draft.getId())
		);
	}

	@Test
	void rejectsEditingANonDraftExam() {
		UUID examId = UUID.randomUUID();
		Exam exam = exam(examId, INSTITUTION_ID, ExamStatus.PUBLISHED, 2);
		when(examRepository.findWithPoolRulesById(examId)).thenReturn(Optional.of(exam));

		assertThrows(
				IllegalArgumentException.class,
				() -> examService.update(manager(INSTITUTION_ID), examId, updateRequest())
		);
	}

	@Test
	void rejectsCrossTenantExamAccess() {
		UUID examId = UUID.randomUUID();
		when(examRepository.findWithPoolRulesById(examId))
				.thenReturn(Optional.of(exam(examId, OTHER_INSTITUTION_ID, ExamStatus.DRAFT, 2)));

		assertThrows(
				AccessDeniedException.class,
				() -> examService.get(manager(INSTITUTION_ID), examId)
		);
	}

	@Test
	void limitsInvigilatorReadsToPublishedExams() {
		UUID draftId = UUID.randomUUID();
		when(examRepository.findWithPoolRulesById(draftId))
				.thenReturn(Optional.of(exam(draftId, INSTITUTION_ID, ExamStatus.DRAFT, 2)));

		assertThrows(
				AccessDeniedException.class,
				() -> examService.get(invigilator(INSTITUTION_ID), draftId)
		);

		UUID publishedId = UUID.randomUUID();
		when(examRepository.findWithPoolRulesById(publishedId))
				.thenReturn(Optional.of(exam(publishedId, INSTITUTION_ID, ExamStatus.PUBLISHED, 2)));
		assertEquals(
				ExamStatus.PUBLISHED,
				examService.get(invigilator(INSTITUTION_ID), publishedId).status()
		);
	}

	@Test
	void validatesCandidatesThroughIdentityBoundaryBeforeAssignment() {
		UUID examId = UUID.randomUUID();
		Exam exam = exam(examId, INSTITUTION_ID, ExamStatus.DRAFT, 2);
		CandidateProfile profile = candidateProfile();
		when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
		when(candidateQuery.requireActiveStudents(INSTITUTION_ID, Set.of(CANDIDATE_ID)))
				.thenReturn(Map.of(CANDIDATE_ID, profile));
		when(examCandidateRepository.findAllByExamIdAndUserIdIn(examId, Set.of(CANDIDATE_ID)))
				.thenReturn(List.of());
		when(examCandidateRepository.saveAllAndFlush(anyList()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var result = examService.assignCandidates(
				manager(INSTITUTION_ID),
				examId,
				new AssignExamCandidatesRequest(List.of(CANDIDATE_ID))
		);

		verify(candidateQuery).requireActiveStudents(INSTITUTION_ID, Set.of(CANDIDATE_ID));
		assertEquals(1, result.size());
		assertEquals(CANDIDATE_ID, result.getFirst().userId());
	}

	@Test
	void paginatesExamSummariesWithoutFetchingPoolCollections() {
		Exam exam = exam(UUID.randomUUID(), INSTITUTION_ID, ExamStatus.DRAFT, 2);
		when(examRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(exam), PageRequest.of(2, 25), 51));

		var result = examService.list(
				manager(INSTITUTION_ID),
				" algebra ",
				SUBJECT_ID,
				ExamStatus.DRAFT,
				2,
				25
		);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(examRepository).findAll(any(Specification.class), pageable.capture());
		verify(examRepository, never()).findWithPoolRulesById(any());
		assertEquals(2, pageable.getValue().getPageNumber());
		assertEquals(25, pageable.getValue().getPageSize());
		assertEquals(51, result.totalElements());
	}

	@Test
	void paginatesCandidateAssignments() {
		UUID examId = UUID.randomUUID();
		Exam exam = exam(examId, INSTITUTION_ID, ExamStatus.DRAFT, 2);
		ExamCandidate assignment = new ExamCandidate(examId, CANDIDATE_ID, ACTOR_ID);
		ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(assignment, "assignedAt", Instant.EPOCH);
		when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
		when(examCandidateRepository.findByExamId(any(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(assignment), PageRequest.of(1, 10), 11));
		when(candidateQuery.findByIds(Set.of(CANDIDATE_ID)))
				.thenReturn(Map.of(CANDIDATE_ID, candidateProfile()));

		var result = examService.listCandidates(manager(INSTITUTION_ID), examId, 1, 10);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(examCandidateRepository).findByExamId(any(), pageable.capture());
		assertEquals(1, pageable.getValue().getPageNumber());
		assertEquals(10, pageable.getValue().getPageSize());
		assertEquals(11, result.totalElements());
		assertEquals(CANDIDATE_ID, result.content().getFirst().userId());
	}

	private static CreateExamRequest createRequest(String code) {
		Instant startsAt = Instant.parse("2030-01-01T09:00:00Z");
		return new CreateExamRequest(
				code,
				SUBJECT_ID,
				"Algebra Examination",
				"Answer every question",
				60,
				startsAt,
				startsAt.plusSeconds(7200),
				"123456",
				true,
				true,
				List.of(new ExamPoolRuleRequest(QuestionDifficulty.EASY, 2, BigDecimal.ONE))
		);
	}

	private static UpdateExamRequest updateRequest() {
		Instant startsAt = Instant.parse("2030-01-01T09:00:00Z");
		return new UpdateExamRequest(
				"MAT-202",
				SUBJECT_ID,
				"Updated Algebra Examination",
				null,
				60,
				startsAt,
				startsAt.plusSeconds(7200),
				false,
				false,
				List.of(new ExamPoolRuleRequest(QuestionDifficulty.EASY, 2, BigDecimal.ONE))
		);
	}

	private static Exam exam(UUID id, UUID institutionId, ExamStatus status, int questionCount) {
		Instant startsAt = Instant.parse("2030-01-01T09:00:00Z");
		Exam exam = new Exam(
				institutionId,
				SUBJECT_ID,
				ACTOR_ID,
				"MAT-202",
				"Algebra Examination",
				null,
				60,
				startsAt,
				startsAt.plusSeconds(7200),
				"bcrypt-hash",
				true,
				true,
				status
		);
		exam.replacePoolRules(List.of(
				new ExamPoolRule(QuestionDifficulty.EASY, questionCount, BigDecimal.ONE)
		));
		ReflectionTestUtils.setField(exam, "id", id);
		ReflectionTestUtils.setField(exam, "createdAt", Instant.EPOCH);
		ReflectionTestUtils.setField(exam, "updatedAt", Instant.EPOCH);
		return exam;
	}

	private static CandidateProfile candidateProfile() {
		return new CandidateProfile(
				CANDIDATE_ID,
				INSTITUTION_ID,
				"Ada",
				"Student",
				"ada@example.edu",
				"STU-001",
				UserStatus.ACTIVE
		);
	}

	private static ExamActor manager(UUID institutionId) {
		return new ExamActor(ACTOR_ID, institutionId, Set.of("EXAMINER"));
	}

	private static ExamActor invigilator(UUID institutionId) {
		return new ExamActor(ACTOR_ID, institutionId, Set.of("INVIGILATOR"));
	}
}
