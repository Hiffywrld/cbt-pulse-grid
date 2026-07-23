package com.cbtpulsegrid.backend.attempt;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.api.AttemptPackageResponse;
import com.cbtpulsegrid.backend.attempt.api.CandidateOptionResponse;
import com.cbtpulsegrid.backend.attempt.api.CandidateQuestionResponse;
import com.cbtpulsegrid.backend.attempt.api.StartAttemptRequest;
import com.cbtpulsegrid.backend.attempt.api.StudentActor;
import com.cbtpulsegrid.backend.attempt.api.SyncAnswerRequest;
import com.cbtpulsegrid.backend.attempt.api.SyncAnswersRequest;
import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.examination.StudentExamQuery;
import com.cbtpulsegrid.backend.examination.StudentExamQuery.AttemptExamDefinition;
import com.cbtpulsegrid.backend.examination.StudentExamQuery.PoolRule;
import com.cbtpulsegrid.backend.identity.AttemptStudentQuery;
import com.cbtpulsegrid.backend.questionbank.AttemptQuestionBankQuery;
import com.cbtpulsegrid.backend.questionbank.AttemptQuestionBankQuery.OptionSnapshot;
import com.cbtpulsegrid.backend.questionbank.AttemptQuestionBankQuery.QuestionSnapshot;
import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import com.cbtpulsegrid.backend.questionbank.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.jpa.repository.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTests {

	private static final UUID INSTITUTION_ID = UUID.randomUUID();
	private static final UUID CANDIDATE_ID = UUID.randomUUID();
	private static final UUID EXAM_ID = UUID.randomUUID();
	private static final UUID SUBJECT_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
	private static final StudentActor ACTOR = new StudentActor(
			CANDIDATE_ID,
			INSTITUTION_ID,
			Set.of("STUDENT")
	);

	@Mock
	private ExamAttemptRepository attemptRepository;
	@Mock
	private AttemptQuestionRepository questionRepository;
	@Mock
	private AttemptAnswerRepository answerRepository;
	@Mock
	private AttemptSyncBatchRepository syncBatchRepository;
	@Mock
	private AttemptStudentQuery studentQuery;
	@Mock
	private StudentExamQuery examQuery;
	@Mock
	private AttemptQuestionBankQuery questionBankQuery;
	@Mock
	private AuditTrail auditTrail;

	private AttemptService attemptService;

	@BeforeEach
	void createService() {
		attemptService = new AttemptService(
				attemptRepository,
				questionRepository,
				answerRepository,
				syncBatchRepository,
				studentQuery,
				examQuery,
				questionBankQuery,
				new SecureRandom(new byte[] {1, 2, 3, 4}),
				Clock.fixed(NOW, ZoneOffset.UTC),
				auditTrail
		);
	}

	@Test
	void selectsConfiguredPoolCountsWithoutDuplicateQuestions() {
		AttemptExamDefinition definition = definition(List.of(
				new PoolRule(QuestionDifficulty.EASY, 2, BigDecimal.ONE),
				new PoolRule(QuestionDifficulty.MEDIUM, 1, new BigDecimal("2.00"))
		));
		when(examQuery.requireStartableExam(
				INSTITUTION_ID,
				CANDIDATE_ID,
				EXAM_ID,
				"123456",
				NOW
		)).thenReturn(definition);
		when(attemptRepository.findByExamIdAndCandidateId(EXAM_ID, CANDIDATE_ID))
				.thenReturn(Optional.empty());
		when(attemptRepository.saveAndFlush(any(ExamAttempt.class))).thenAnswer(invocation -> {
			ExamAttempt attempt = invocation.getArgument(0);
			ReflectionTestUtils.setField(attempt, "id", UUID.randomUUID());
			return attempt;
		});
		when(questionBankQuery.findPublishedQuestionSnapshots(
				INSTITUTION_ID,
				SUBJECT_ID,
				QuestionDifficulty.EASY
		)).thenReturn(List.of(
				question(QuestionType.SINGLE_CHOICE, QuestionDifficulty.EASY),
				question(QuestionType.SINGLE_CHOICE, QuestionDifficulty.EASY),
				question(QuestionType.SINGLE_CHOICE, QuestionDifficulty.EASY)
		));
		when(questionBankQuery.findPublishedQuestionSnapshots(
				INSTITUTION_ID,
				SUBJECT_ID,
				QuestionDifficulty.MEDIUM
		)).thenReturn(List.of(
				question(QuestionType.MULTIPLE_CHOICE, QuestionDifficulty.MEDIUM),
				question(QuestionType.MULTIPLE_CHOICE, QuestionDifficulty.MEDIUM)
		));
		List<AttemptQuestion> persisted = new ArrayList<>();
		when(questionRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
			List<AttemptQuestion> questions = invocation.getArgument(0);
			for (AttemptQuestion question : questions) {
				assignSnapshotIds(question);
			}
			persisted.addAll(questions);
			return questions;
		});
		when(questionRepository.findAllWithOptionsByAttemptId(any())).thenAnswer(invocation -> persisted);
		when(answerRepository.findAllWithSelectionsByAttemptId(any())).thenReturn(List.of());

		AttemptPackageResponse response = attemptService.startOrResume(
				ACTOR,
				EXAM_ID,
				new StartAttemptRequest("123456", "browser-device")
		);

		verify(questionRepository).saveAllAndFlush(anyList());
		assertEquals(3, response.questions().size());
		assertEquals(3, persisted.stream()
				.map(AttemptQuestion::getSourceQuestionId)
				.distinct()
				.count());
		assertEquals(2, persisted.stream()
				.filter(question -> question.getDifficulty() == QuestionDifficulty.EASY)
				.count());
	}

	@Test
	void candidatePackageContractContainsNoCorrectOrSourceIdentifiers() {
		Set<String> optionFields = java.util.Arrays.stream(CandidateOptionResponse.class.getRecordComponents())
				.map(java.lang.reflect.RecordComponent::getName)
				.collect(java.util.stream.Collectors.toSet());
		Set<String> questionFields = java.util.Arrays.stream(CandidateQuestionResponse.class.getRecordComponents())
				.map(java.lang.reflect.RecordComponent::getName)
				.collect(java.util.stream.Collectors.toSet());

		assertFalse(optionFields.contains("correct"));
		assertFalse(optionFields.contains("sourceOptionId"));
		assertFalse(questionFields.contains("sourceQuestionId"));
		assertFalse(questionFields.contains("marks"));
		String sensitiveRequestText = new StartAttemptRequest("123456", "raw-device-id").toString();
		assertFalse(sensitiveRequestText.contains("123456"));
		assertFalse(sensitiveRequestText.contains("raw-device-id"));
	}

	@Test
	void resumesIdempotentlyOnTheSameDeviceAndRejectsAnotherDevice() throws Exception {
		ExamAttempt attempt = inProgressAttempt(NOW.plusSeconds(3600), hash("device-a"));
		when(examQuery.requireStartableExam(
				INSTITUTION_ID,
				CANDIDATE_ID,
				EXAM_ID,
				"123456",
				NOW
		)).thenReturn(definition(List.of(new PoolRule(
				QuestionDifficulty.EASY,
				1,
				BigDecimal.ONE
		))));
		when(attemptRepository.findByExamIdAndCandidateId(EXAM_ID, CANDIDATE_ID))
				.thenReturn(Optional.of(attempt));
		when(attemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
		when(questionRepository.findAllWithOptionsByAttemptId(attempt.getId())).thenReturn(List.of());
		when(answerRepository.findAllWithSelectionsByAttemptId(attempt.getId())).thenReturn(List.of());

		var first = attemptService.startOrResume(
				ACTOR,
				EXAM_ID,
				new StartAttemptRequest("123456", "device-a")
		);
		var second = attemptService.startOrResume(
				ACTOR,
				EXAM_ID,
				new StartAttemptRequest("123456", "device-a")
		);

		assertEquals(first.attemptId(), second.attemptId());
		verify(attemptRepository, never()).saveAndFlush(any(ExamAttempt.class));
		assertThrows(
				AttemptConflictException.class,
				() -> attemptService.startOrResume(
						ACTOR,
						EXAM_ID,
						new StartAttemptRequest("123456", "device-b")
				)
		);
	}

	@Test
	void duplicateSyncIdIsAcknowledgedWithoutApplyingAnswersTwice() {
		ExamAttempt attempt = inProgressAttempt(NOW.plusSeconds(3600), hashUnchecked("device"));
		UUID syncId = UUID.randomUUID();
		when(attemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
		when(syncBatchRepository.existsByAttemptIdAndSyncId(attempt.getId(), syncId)).thenReturn(true);
		when(answerRepository.findAllWithSelectionsByAttemptId(attempt.getId())).thenReturn(List.of());

		var response = attemptService.syncAnswers(
				ACTOR,
				attempt.getId(),
				new SyncAnswersRequest(syncId, List.of(new SyncAnswerRequest(
						UUID.randomUUID(),
						Set.of(),
						1
				)))
		);

		assertEquals(syncId, response.acknowledgedSyncId());
		verify(answerRepository, never()).saveAll(anyList());
		verify(syncBatchRepository, never()).save(any(AttemptSyncBatch.class));
	}

	@Test
	void lowerClientSequenceCannotOverwriteNewerAnswer() {
		ExamAttempt attempt = inProgressAttempt(NOW.plusSeconds(3600), hashUnchecked("device"));
		AttemptQuestion question = attemptQuestion(attempt.getId(), QuestionType.SINGLE_CHOICE, BigDecimal.ONE);
		UUID savedOption = question.getOptions().getFirst().getId();
		AttemptAnswer existing = new AttemptAnswer(
				attempt.getId(),
				question.getId(),
				10,
				NOW.minusSeconds(10),
				Set.of(savedOption)
		);
		UUID syncId = UUID.randomUUID();
		when(attemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
		when(questionRepository.findAllWithOptionsByAttemptId(attempt.getId())).thenReturn(List.of(question));
		when(answerRepository.findAllWithSelectionsByAttemptId(attempt.getId()))
				.thenReturn(List.of(existing));
		when(attemptRepository.saveAndFlush(attempt)).thenReturn(attempt);

		var response = attemptService.syncAnswers(
				ACTOR,
				attempt.getId(),
				new SyncAnswersRequest(syncId, List.of(new SyncAnswerRequest(
						question.getId(),
						Set.of(question.getOptions().get(1).getId()),
						9
				)))
		);

		assertEquals(10, response.savedAnswers().getFirst().clientSequence());
		assertEquals(Set.of(savedOption), response.savedAnswers().getFirst().selectedOptionIds());
		verify(answerRepository, never()).saveAll(anyList());
		verify(syncBatchRepository).save(any(AttemptSyncBatch.class));

		UUID newerOption = question.getOptions().get(1).getId();
		var newerResponse = attemptService.syncAnswers(
				ACTOR,
				attempt.getId(),
				new SyncAnswersRequest(UUID.randomUUID(), List.of(new SyncAnswerRequest(
						question.getId(),
						Set.of(newerOption),
						11
				)))
		);

		assertEquals(11, newerResponse.savedAnswers().getFirst().clientSequence());
		assertEquals(Set.of(newerOption), newerResponse.savedAnswers().getFirst().selectedOptionIds());
		verify(answerRepository, times(1)).saveAll(anyList());
	}

	@Test
	void rejectsAnOptionThatDoesNotBelongToTheAttemptQuestion() {
		ExamAttempt attempt = inProgressAttempt(NOW.plusSeconds(3600), hashUnchecked("device"));
		AttemptQuestion question = attemptQuestion(attempt.getId(), QuestionType.SINGLE_CHOICE, BigDecimal.ONE);
		when(attemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
		when(questionRepository.findAllWithOptionsByAttemptId(attempt.getId())).thenReturn(List.of(question));
		when(answerRepository.findAllWithSelectionsByAttemptId(attempt.getId())).thenReturn(List.of());

		assertThrows(
				IllegalArgumentException.class,
				() -> attemptService.syncAnswers(
						ACTOR,
						attempt.getId(),
						new SyncAnswersRequest(UUID.randomUUID(), List.of(new SyncAnswerRequest(
								question.getId(),
								Set.of(UUID.randomUUID()),
								1
						)))
				)
		);
		verify(syncBatchRepository, never()).save(any());
	}

	@Test
	void scoresEveryQuestionTypeByExactSelectionAndSubmissionIsIdempotent() {
		ExamAttempt attempt = inProgressAttempt(NOW.plusSeconds(3600), hashUnchecked("device"));
		AttemptQuestion single = attemptQuestion(attempt.getId(), QuestionType.SINGLE_CHOICE, BigDecimal.ONE);
		AttemptQuestion multiple = attemptQuestion(attempt.getId(), QuestionType.MULTIPLE_CHOICE, new BigDecimal("3.00"));
		AttemptQuestion trueFalse = attemptQuestion(attempt.getId(), QuestionType.TRUE_FALSE, new BigDecimal("2.00"));
		List<AttemptAnswer> answers = List.of(
				answer(attempt, single, Set.of(single.getOptions().getFirst().getId())),
				answer(attempt, multiple, Set.of(
						multiple.getOptions().getFirst().getId(),
						multiple.getOptions().get(1).getId()
				)),
				answer(attempt, trueFalse, Set.of(trueFalse.getOptions().getFirst().getId()))
		);
		when(attemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
		when(questionRepository.findAllWithOptionsByAttemptId(attempt.getId()))
				.thenReturn(List.of(single, multiple, trueFalse));
		when(answerRepository.findAllWithSelectionsByAttemptId(attempt.getId())).thenReturn(answers);
		when(examQuery.requireAssignedDefinition(INSTITUTION_ID, CANDIDATE_ID, EXAM_ID))
				.thenReturn(definition(List.of(new PoolRule(
						QuestionDifficulty.EASY,
						3,
						BigDecimal.ONE
				))));
		when(attemptRepository.saveAndFlush(attempt)).thenReturn(attempt);

		var first = attemptService.submit(ACTOR, attempt.getId());
		var second = attemptService.submit(ACTOR, attempt.getId());

		assertEquals(new BigDecimal("6.00"), first.score().setScale(2));
		assertEquals(new BigDecimal("6.00"), first.maximumScore().setScale(2));
		assertEquals(new BigDecimal("100.00"), first.percentage());
		assertTrue(first.passed());
		assertEquals(first, second);
		verify(questionRepository, times(1)).findAllWithOptionsByAttemptId(attempt.getId());
	}

	@Test
	void automaticallySubmitsExpiredAttempt() {
		ExamAttempt attempt = inProgressAttempt(NOW, hashUnchecked("device"));
		AttemptQuestion question = attemptQuestion(attempt.getId(), QuestionType.TRUE_FALSE, BigDecimal.ONE);
		when(attemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
		when(questionRepository.findAllWithOptionsByAttemptId(attempt.getId())).thenReturn(List.of(question));
		when(answerRepository.findAllWithSelectionsByAttemptId(attempt.getId())).thenReturn(List.of());
		when(examQuery.requireAssignedDefinition(INSTITUTION_ID, CANDIDATE_ID, EXAM_ID))
				.thenReturn(definition(List.of(new PoolRule(
						QuestionDifficulty.EASY,
						1,
						BigDecimal.ONE
				))));
		when(attemptRepository.saveAndFlush(attempt)).thenReturn(attempt);

		attemptService.autoSubmitExpired(attempt.getId());

		assertEquals(AttemptStatus.AUTO_SUBMITTED, attempt.getStatus());
		assertEquals(BigDecimal.ZERO.setScale(2), attempt.getPercentage());
		assertFalse(attempt.getPassed());
	}

	@Test
	void expiryBatchAndManualSubmitProduceOneIdenticalResult() {
		ExamAttempt attempt = inProgressAttempt(NOW, hashUnchecked("device"));
		AttemptQuestion question = attemptQuestion(attempt.getId(), QuestionType.TRUE_FALSE, BigDecimal.ONE);
		when(attemptRepository.findExpiredForUpdate(NOW, 25)).thenReturn(List.of(attempt));
		when(attemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
		when(questionRepository.findAllWithOptionsByAttemptId(attempt.getId())).thenReturn(List.of(question));
		when(answerRepository.findAllWithSelectionsByAttemptId(attempt.getId())).thenReturn(List.of());
		when(examQuery.requireAssignedDefinition(INSTITUTION_ID, CANDIDATE_ID, EXAM_ID))
				.thenReturn(definition(List.of(new PoolRule(
						QuestionDifficulty.EASY,
						1,
						BigDecimal.ONE
				))));
		when(attemptRepository.saveAndFlush(attempt)).thenReturn(attempt);

		assertEquals(1, attemptService.autoSubmitExpiredBatch(25));
		var scheduledResult = attemptService.submit(ACTOR, attempt.getId());
		var repeatedResult = attemptService.submit(ACTOR, attempt.getId());

		assertEquals(AttemptStatus.AUTO_SUBMITTED, scheduledResult.status());
		assertEquals(scheduledResult, repeatedResult);
		verify(questionRepository, times(1)).findAllWithOptionsByAttemptId(attempt.getId());
	}

	@Test
	void expiryBatchUsesPostgresqlSkipLockedForReplicaSafety() throws Exception {
		Method method = ExamAttemptRepository.class.getDeclaredMethod(
				"findExpiredForUpdate",
				Instant.class,
				int.class
		);
		String sql = method.getAnnotation(Query.class).value().toLowerCase();

		assertTrue(sql.contains("status = 'in_progress'"));
		assertTrue(sql.contains("expires_at <= :now"));
		assertTrue(sql.contains("limit :batchsize"));
		assertTrue(sql.contains("for update of attempt skip locked"));
	}

	@Test
	void awardsZeroForAPartialMultipleChoiceSelection() {
		ExamAttempt attempt = inProgressAttempt(NOW.plusSeconds(3600), hashUnchecked("device"));
		AttemptQuestion multiple = attemptQuestion(
				attempt.getId(),
				QuestionType.MULTIPLE_CHOICE,
				new BigDecimal("3.00")
		);
		AttemptAnswer partial = answer(
				attempt,
				multiple,
				Set.of(multiple.getOptions().getFirst().getId())
		);
		when(attemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
		when(questionRepository.findAllWithOptionsByAttemptId(attempt.getId()))
				.thenReturn(List.of(multiple));
		when(answerRepository.findAllWithSelectionsByAttemptId(attempt.getId()))
				.thenReturn(List.of(partial));
		when(examQuery.requireAssignedDefinition(INSTITUTION_ID, CANDIDATE_ID, EXAM_ID))
				.thenReturn(definition(List.of(new PoolRule(
						QuestionDifficulty.EASY,
						1,
						new BigDecimal("3.00")
				))));
		when(attemptRepository.saveAndFlush(attempt)).thenReturn(attempt);

		var result = attemptService.submit(ACTOR, attempt.getId());

		assertEquals(BigDecimal.ZERO, result.score());
		assertEquals(new BigDecimal("3.00"), result.maximumScore());
		assertEquals(new BigDecimal("0.00"), result.percentage());
		assertFalse(result.passed());
	}

	@Test
	void rejectsAccessToAnotherStudentsAttempt() {
		ExamAttempt attempt = inProgressAttempt(NOW.plusSeconds(3600), hashUnchecked("device"));
		ReflectionTestUtils.setField(attempt, "candidateId", UUID.randomUUID());
		when(attemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));

		assertThrows(
				AccessDeniedException.class,
				() -> attemptService.submit(ACTOR, attempt.getId())
		);
	}

	private static AttemptExamDefinition definition(List<PoolRule> poolRules) {
		return new AttemptExamDefinition(
				EXAM_ID,
				INSTITUTION_ID,
				SUBJECT_ID,
				"MAT-101",
				"Mathematics",
				"Answer every question",
				60,
				NOW.minusSeconds(3600),
				NOW.plusSeconds(7200),
				true,
				true,
				new BigDecimal("50.00"),
				poolRules
		);
	}

	private static QuestionSnapshot question(QuestionType type, QuestionDifficulty difficulty) {
		return new QuestionSnapshot(
				UUID.randomUUID(),
				"Question text",
				type,
				difficulty,
				List.of(
						new OptionSnapshot(UUID.randomUUID(), "A", true, 1),
						new OptionSnapshot(UUID.randomUUID(), "B", type == QuestionType.MULTIPLE_CHOICE, 2),
						new OptionSnapshot(UUID.randomUUID(), "C", false, 3)
				)
		);
	}

	private static AttemptQuestion attemptQuestion(
			UUID attemptId,
			QuestionType type,
			BigDecimal marks
	) {
		AttemptQuestion question = new AttemptQuestion(
				attemptId,
				UUID.randomUUID(),
				1,
				"Question text",
				type,
				QuestionDifficulty.EASY,
				marks
		);
		question.addOption(new AttemptOption(UUID.randomUUID(), "A", 1, true));
		question.addOption(new AttemptOption(
				UUID.randomUUID(),
				"B",
				2,
				type == QuestionType.MULTIPLE_CHOICE
		));
		if (type == QuestionType.MULTIPLE_CHOICE) {
			question.addOption(new AttemptOption(UUID.randomUUID(), "C", 3, false));
		}
		assignSnapshotIds(question);
		return question;
	}

	private static AttemptAnswer answer(
			ExamAttempt attempt,
			AttemptQuestion question,
			Set<UUID> selectedOptions
	) {
		return new AttemptAnswer(
				attempt.getId(),
				question.getId(),
				1,
				NOW,
				selectedOptions
		);
	}

	private static ExamAttempt inProgressAttempt(Instant expiresAt, String deviceHash) {
		ExamAttempt attempt = new ExamAttempt(
				INSTITUTION_ID,
				EXAM_ID,
				CANDIDATE_ID,
				deviceHash,
				NOW.minusSeconds(60),
				expiresAt
		);
		ReflectionTestUtils.setField(attempt, "id", UUID.randomUUID());
		return attempt;
	}

	private static void assignSnapshotIds(AttemptQuestion question) {
		if (question.getId() == null) {
			ReflectionTestUtils.setField(question, "id", UUID.randomUUID());
		}
		for (AttemptOption option : question.getOptions()) {
			if (option.getId() == null) {
				ReflectionTestUtils.setField(option, "id", UUID.randomUUID());
			}
		}
	}

	private static String hash(String value) throws Exception {
		return HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
		);
	}

	private static String hashUnchecked(String value) {
		try {
			return hash(value);
		}
		catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}
}
