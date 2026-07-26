package com.cbtpulsegrid.backend.examination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentExamQueryServiceTests {

	private static final UUID INSTITUTION_ID = UUID.randomUUID();
	private static final UUID CANDIDATE_ID = UUID.randomUUID();
	private static final UUID EXAM_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");

	@Mock
	private ExamRepository examRepository;
	@Mock
	private ExamCandidateRepository candidateRepository;
	@Mock
	private PasswordEncoder passwordEncoder;

	private StudentExamQueryService queryService;

	@BeforeEach
	void createService() {
		queryService = new StudentExamQueryService(
				examRepository,
				candidateRepository,
				passwordEncoder
		);
	}

	@Test
	void rejectsWrongAccessPin() {
		Exam exam = publishedExam(NOW.minusSeconds(60), NOW.plusSeconds(3600));
		when(examRepository.findWithPoolRulesById(EXAM_ID)).thenReturn(Optional.of(exam));
		when(candidateRepository.findByExamIdAndUserIdForUpdate(EXAM_ID, CANDIDATE_ID))
				.thenReturn(Optional.of(assignment()));
		when(passwordEncoder.matches("000000", "pin-hash")).thenReturn(false);

		assertThrows(
				IllegalArgumentException.class,
				() -> queryService.requireStartableExam(
						INSTITUTION_ID,
						CANDIDATE_ID,
						EXAM_ID,
						"000000",
						NOW
				)
		);
	}

	@Test
	void rejectsUnassignedStudent() {
		when(examRepository.findWithPoolRulesById(EXAM_ID))
				.thenReturn(Optional.of(publishedExam(NOW.minusSeconds(60), NOW.plusSeconds(3600))));
		when(candidateRepository.findByExamIdAndUserIdForUpdate(EXAM_ID, CANDIDATE_ID))
				.thenReturn(Optional.empty());

		assertThrows(
				AccessDeniedException.class,
				() -> queryService.requireStartableExam(
						INSTITUTION_ID,
						CANDIDATE_ID,
						EXAM_ID,
						"123456",
						NOW
				)
		);
	}

	@Test
	void rejectsInactiveExamWindows() {
		Exam upcoming = publishedExam(NOW.plusSeconds(60), NOW.plusSeconds(3600));
		when(examRepository.findWithPoolRulesById(EXAM_ID)).thenReturn(Optional.of(upcoming));
		when(candidateRepository.findByExamIdAndUserIdForUpdate(EXAM_ID, CANDIDATE_ID))
				.thenReturn(Optional.of(assignment()));

		assertThrows(
				IllegalArgumentException.class,
				() -> queryService.requireStartableExam(
						INSTITUTION_ID,
						CANDIDATE_ID,
						EXAM_ID,
						"123456",
						NOW
				)
		);

		Exam ended = publishedExam(NOW.minusSeconds(3600), NOW);
		when(examRepository.findWithPoolRulesById(EXAM_ID)).thenReturn(Optional.of(ended));
		assertThrows(
				IllegalArgumentException.class,
				() -> queryService.requireStartableExam(
						INSTITUTION_ID,
						CANDIDATE_ID,
						EXAM_ID,
						"123456",
						NOW
				)
		);
	}

	private static Exam publishedExam(Instant startsAt, Instant endsAt) {
		Exam exam = new Exam(
				INSTITUTION_ID,
				UUID.randomUUID(),
				UUID.randomUUID(),
				"MAT-101",
				"Mathematics",
				null,
				60,
				startsAt,
				endsAt,
				"pin-hash",
				true,
				true,
				new BigDecimal("50.00"),
				ExamStatus.PUBLISHED
		);
		exam.replacePoolRules(List.of(
				new ExamPoolRule(QuestionDifficulty.EASY, 1, BigDecimal.ONE)
		));
		ReflectionTestUtils.setField(exam, "id", EXAM_ID);
		return exam;
	}

	private static ExamCandidate assignment() {
		return new ExamCandidate(EXAM_ID, CANDIDATE_ID, UUID.randomUUID());
	}
}
