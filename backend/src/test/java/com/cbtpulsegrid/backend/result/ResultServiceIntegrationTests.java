package com.cbtpulsegrid.backend.result;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.result.api.CandidateResultStatus;
import com.cbtpulsegrid.backend.result.api.ResultActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ResultServiceIntegrationTests {

	private static final Instant START = Instant.parse("2030-01-01T10:00:00Z");

	@Autowired
	private ResultService resultService;
	@Autowired
	private JdbcTemplate jdbc;

	private UUID institutionId;
	private UUID examId;
	private UUID submittedAttemptId;
	private UUID subjectId;
	private UUID creatorId;

	@BeforeEach
	void insertReportingScenario() {
		institutionId = UUID.randomUUID();
		examId = UUID.randomUUID();
		subjectId = UUID.randomUUID();
		creatorId = UUID.randomUUID();
		insertInstitution(institutionId, "RPT-" + shortId(institutionId));
		insertUser(creatorId, institutionId, "Exam", "Creator", "creator-" + creatorId + "@test.local", null);
		jdbc.update("""
				insert into subjects (
				    id, institution_id, code, name, description, status, created_at, updated_at, version
				) values (?, ?, ?, ?, null, 'ACTIVE', ?, ?, 0)
				""", subjectId, institutionId, "SUB-" + shortId(subjectId), "Reporting Subject", ts(START), ts(START));
		jdbc.update("""
				insert into exams (
				    id, institution_id, subject_id, created_by, code, title, instructions,
				    duration_minutes, starts_at, ends_at, access_pin_hash,
				    shuffle_questions, shuffle_options, status, created_at, updated_at, version,
				    pass_mark_percentage
				) values (?, ?, ?, ?, ?, ?, null, 60, ?, ?, ?, false, false, 'PUBLISHED', ?, ?, 0, 50.00)
				""",
				examId,
				institutionId,
				subjectId,
				creatorId,
				"EX-" + shortId(examId),
				"Reporting Exam",
				ts(START),
				ts(START.plusSeconds(7200)),
				"bcrypt-placeholder",
				ts(START),
				ts(START)
		);
		jdbc.update("""
				insert into exam_pool_rules (
				    id, exam_id, difficulty, question_count, marks_per_question
				) values (?, ?, 'EASY', 10, 2.00)
				""", UUID.randomUUID(), examId);

		UUID submittedCandidate = insertCandidate("REG-1", "Ada", "Submitted");
		UUID autoCandidate = insertCandidate("REG-2", "Ben", "Automatic");
		insertCandidate("REG-3", "Cara", "Not Started");
		UUID activeCandidate = insertCandidate("REG-4", "Dan", "In Progress");

		submittedAttemptId = insertAttempt(
				submittedCandidate,
				"SUBMITTED",
				new BigDecimal("16.00"),
				new BigDecimal("80.00"),
				true,
				START.plusSeconds(1800)
		);
		insertAttempt(
				autoCandidate,
				"AUTO_SUBMITTED",
				new BigDecimal("8.00"),
				new BigDecimal("40.00"),
				false,
				START.plusSeconds(3600)
		);
		insertAttempt(activeCandidate, "IN_PROGRESS", null, null, null, null);
		insertSubmittedQuestionReview(submittedAttemptId);
	}

	@Test
	void calculatesSummaryIncludingAssignedCandidatesWhoHaveNotStarted() {
		var summary = resultService.summary(staff(institutionId), examId);

		assertEquals(4, summary.assignedCandidates());
		assertEquals(1, summary.notStarted());
		assertEquals(1, summary.inProgress());
		assertEquals(1, summary.submitted());
		assertEquals(1, summary.autoSubmitted());
		assertEquals(1, summary.passed());
		assertEquals(1, summary.failed());
		assertEquals(new BigDecimal("60.00"), summary.averagePercentage());
		assertEquals(new BigDecimal("40.00"), summary.minimumPercentage());
		assertEquals(new BigDecimal("80.00"), summary.maximumPercentage());
		assertEquals(new BigDecimal("50.00"), summary.passRate());
		assertEquals(new BigDecimal("20.00"), summary.totalObtainableMarks().setScale(2));
	}

	@Test
	void candidateListingSupportsPaginationSearchAndFilters() {
		var firstPage = resultService.candidates(
				staff(institutionId), examId, null, null, null, 0, 2
		);
		assertEquals(4, firstPage.totalElements());
		assertEquals(2, firstPage.totalPages());
		assertEquals(2, firstPage.content().size());

		var notStarted = resultService.candidates(
				staff(institutionId), examId, "reg-3", CandidateResultStatus.NOT_STARTED, null, 0, 20
		);
		assertEquals(1, notStarted.totalElements());
		assertEquals(CandidateResultStatus.NOT_STARTED, notStarted.content().getFirst().attemptStatus());

		var autoSubmitted = resultService.candidates(
				staff(institutionId), examId, "automatic", CandidateResultStatus.AUTO_SUBMITTED, false, 0, 20
		);
		assertEquals(1, autoSubmitted.totalElements());
		assertFalse(autoSubmitted.content().getFirst().passed());
	}

	@Test
	void detailedReviewIsTenantSecuredAndAvailableOnlyAfterSubmission() {
		var detail = resultService.attempt(staff(institutionId), submittedAttemptId);

		assertTrue(detail.reviewAvailable());
		assertEquals(1, detail.questions().size());
		assertEquals(new BigDecimal("2.00"), detail.questions().getFirst().awardedMarks().setScale(2));
		assertTrue(detail.questions().getFirst().options().getFirst().correct());
		assertTrue(detail.questions().getFirst().options().getFirst().selected());

		assertThrows(
				java.util.NoSuchElementException.class,
				() -> resultService.attempt(staff(UUID.randomUUID()), submittedAttemptId)
		);
		assertThrows(
				AccessDeniedException.class,
				() -> resultService.summary(
						new ResultActor(UUID.randomUUID(), institutionId, Set.of("STUDENT")),
						examId
				)
		);
	}

	@Test
	void examinerResultsAreLimitedToExamsTheyCreated() {
		ResultActor otherExaminer = new ResultActor(UUID.randomUUID(), institutionId, Set.of("EXAMINER"));

		assertThrows(AccessDeniedException.class, () -> resultService.summary(otherExaminer, examId));
		assertThrows(
				AccessDeniedException.class,
				() -> resultService.candidates(otherExaminer, examId, null, null, null, 0, 20)
		);
		assertThrows(AccessDeniedException.class, () -> resultService.attempt(otherExaminer, submittedAttemptId));
		assertThrows(
				AccessDeniedException.class,
				() -> resultService.exportCsv(otherExaminer, examId, null, null, null)
		);
	}

	private UUID insertCandidate(String registrationNumber, String firstName, String lastName) {
		UUID userId = UUID.randomUUID();
		insertUser(
				userId,
				institutionId,
				firstName,
				lastName,
				registrationNumber.toLowerCase() + "-" + userId + "@test.local",
				registrationNumber
		);
		jdbc.update("""
				insert into exam_candidates (id, exam_id, user_id, assigned_by, assigned_at)
				values (?, ?, ?, ?, ?)
				""", UUID.randomUUID(), examId, userId, userId, ts(START.minusSeconds(60)));
		return userId;
	}

	private UUID insertAttempt(
			UUID candidateId,
			String status,
			BigDecimal score,
			BigDecimal percentage,
			Boolean passed,
			Instant submittedAt
	) {
		UUID attemptId = UUID.randomUUID();
		jdbc.update("""
				insert into exam_attempts (
				    id, institution_id, exam_id, candidate_id, status, device_id_hash,
				    started_at, expires_at, submitted_at, last_saved_at,
				    score, maximum_score, percentage, passed, created_at, updated_at, version
				) values (?, ?, ?, ?, ?, ?, ?, ?, ?, null, ?, ?, ?, ?, ?, ?, 0)
				""",
				attemptId,
				institutionId,
				examId,
				candidateId,
				status,
				"0".repeat(64),
				ts(START),
				ts(START.plusSeconds(3600)),
				ts(submittedAt),
				score,
				score == null ? null : new BigDecimal("20.00"),
				percentage,
				passed,
				ts(START),
				ts(START)
		);
		return attemptId;
	}

	private void insertSubmittedQuestionReview(UUID attemptId) {
		UUID sourceQuestionId = UUID.randomUUID();
		UUID sourceCorrectOption = UUID.randomUUID();
		UUID sourceOtherOption = UUID.randomUUID();
		jdbc.update("""
				insert into questions (
				    id, institution_id, subject_id, created_by, question_text, type,
				    difficulty, marks, status, created_at, updated_at, version
				) values (?, ?, ?, ?, 'Two plus two?', 'SINGLE_CHOICE',
				          'EASY', 2.00, 'PUBLISHED', ?, ?, 0)
				""", sourceQuestionId, institutionId, subjectId, creatorId, ts(START), ts(START));
		jdbc.update("""
				insert into question_options (
				    id, question_id, option_text, correct, display_order
				) values (?, ?, 'Four', true, 1), (?, ?, 'Five', false, 2)
				""", sourceCorrectOption, sourceQuestionId, sourceOtherOption, sourceQuestionId);

		UUID questionId = UUID.randomUUID();
		UUID correctOption = UUID.randomUUID();
		UUID otherOption = UUID.randomUUID();
		UUID answerId = UUID.randomUUID();
		jdbc.update("""
				insert into attempt_questions (
				    id, attempt_id, source_question_id, position, question_text,
				    question_type, difficulty, marks
				) values (?, ?, ?, 1, 'Two plus two?', 'SINGLE_CHOICE', 'EASY', 2.00)
				""", questionId, attemptId, sourceQuestionId);
		jdbc.update("""
				insert into attempt_options (
				    id, attempt_question_id, source_option_id, option_text, display_order, correct
				) values (?, ?, ?, 'Four', 1, true), (?, ?, ?, 'Five', 2, false)
				""",
				correctOption, questionId, sourceCorrectOption,
				otherOption, questionId, sourceOtherOption);
		jdbc.update("""
				insert into attempt_answers (
				    id, attempt_id, attempt_question_id, client_sequence, answered_at
				) values (?, ?, ?, 1, ?)
				""", answerId, attemptId, questionId, ts(START.plusSeconds(60)));
		jdbc.update("""
				insert into attempt_answer_selections (attempt_answer_id, attempt_option_id)
				values (?, ?)
				""", answerId, correctOption);
	}

	private void insertInstitution(UUID id, String code) {
		jdbc.update("""
				insert into institutions (id, name, code, status, created_at, updated_at, version)
				values (?, 'Reporting Institution', ?, 'ACTIVE', ?, ?, 0)
				""", id, code, ts(START), ts(START));
	}

	private void insertUser(
			UUID id,
			UUID userInstitutionId,
			String firstName,
			String lastName,
			String email,
			String registrationNumber
	) {
		jdbc.update("""
				insert into users (
				    id, institution_id, first_name, last_name, email, password_hash,
				    registration_number, status, created_at, updated_at, version
				) values (?, ?, ?, ?, ?, 'bcrypt-placeholder', ?, 'ACTIVE', ?, ?, 0)
				""",
				id,
				userInstitutionId,
				firstName,
				lastName,
				email,
				registrationNumber,
				ts(START),
				ts(START)
		);
	}

	private ResultActor staff(UUID institutionId) {
		return new ResultActor(creatorId, institutionId, Set.of("EXAMINER"));
	}

	private static Timestamp ts(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private static String shortId(UUID id) {
		return id.toString().substring(0, 8).toUpperCase();
	}
}
