package com.cbtpulsegrid.backend.attempt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.attempt.api.AttemptPackageResponse;
import com.cbtpulsegrid.backend.attempt.api.AttemptResultResponse;
import com.cbtpulsegrid.backend.attempt.api.CandidateOptionResponse;
import com.cbtpulsegrid.backend.attempt.api.CandidateQuestionResponse;
import com.cbtpulsegrid.backend.attempt.api.ExamAvailability;
import com.cbtpulsegrid.backend.attempt.api.SavedAnswerResponse;
import com.cbtpulsegrid.backend.attempt.api.StartAttemptRequest;
import com.cbtpulsegrid.backend.attempt.api.StudentActor;
import com.cbtpulsegrid.backend.attempt.api.StudentExamDetailResponse;
import com.cbtpulsegrid.backend.attempt.api.StudentExamSummaryResponse;
import com.cbtpulsegrid.backend.attempt.api.SyncAnswerRequest;
import com.cbtpulsegrid.backend.attempt.api.SyncAnswersRequest;
import com.cbtpulsegrid.backend.attempt.api.SyncAnswersResponse;
import com.cbtpulsegrid.backend.examination.StudentExamQuery;
import com.cbtpulsegrid.backend.examination.StudentExamQuery.AttemptExamDefinition;
import com.cbtpulsegrid.backend.examination.StudentExamQuery.PoolRule;
import com.cbtpulsegrid.backend.examination.StudentExamQuery.StudentExamView;
import com.cbtpulsegrid.backend.identity.AttemptStudentQuery;
import com.cbtpulsegrid.backend.questionbank.AttemptQuestionBankQuery;
import com.cbtpulsegrid.backend.questionbank.AttemptQuestionBankQuery.OptionSnapshot;
import com.cbtpulsegrid.backend.questionbank.AttemptQuestionBankQuery.QuestionSnapshot;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptService {

	private final ExamAttemptRepository attemptRepository;
	private final AttemptQuestionRepository questionRepository;
	private final AttemptAnswerRepository answerRepository;
	private final AttemptSyncBatchRepository syncBatchRepository;
	private final AttemptStudentQuery studentQuery;
	private final StudentExamQuery examQuery;
	private final AttemptQuestionBankQuery questionBankQuery;
	private final SecureRandom secureRandom;
	private final Clock clock;

	public AttemptService(
			ExamAttemptRepository attemptRepository,
			AttemptQuestionRepository questionRepository,
			AttemptAnswerRepository answerRepository,
			AttemptSyncBatchRepository syncBatchRepository,
			AttemptStudentQuery studentQuery,
			StudentExamQuery examQuery,
			AttemptQuestionBankQuery questionBankQuery,
			SecureRandom secureRandom,
			Clock clock
	) {
		this.attemptRepository = attemptRepository;
		this.questionRepository = questionRepository;
		this.answerRepository = answerRepository;
		this.syncBatchRepository = syncBatchRepository;
		this.studentQuery = studentQuery;
		this.examQuery = examQuery;
		this.questionBankQuery = questionBankQuery;
		this.secureRandom = secureRandom;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<StudentExamSummaryResponse> listAssignedExams(StudentActor actor) {
		requireActiveStudent(actor);
		Instant now = clock.instant();
		return examQuery.findAssignedPublishedExams(actor.institutionId(), actor.userId()).stream()
				.map(exam -> new StudentExamSummaryResponse(
						exam.id(),
						exam.code(),
						exam.title(),
						exam.durationMinutes(),
						exam.startsAt(),
						exam.endsAt(),
						availability(exam, now)
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public StudentExamDetailResponse getAssignedExam(StudentActor actor, UUID examId) {
		requireActiveStudent(actor);
		StudentExamView exam = examQuery.requireAssignedPublishedExam(
				actor.institutionId(),
				actor.userId(),
				examId
		);
		return new StudentExamDetailResponse(
				exam.id(),
				exam.code(),
				exam.title(),
				exam.instructions(),
				exam.durationMinutes(),
				exam.startsAt(),
				exam.endsAt(),
				availability(exam, clock.instant())
		);
	}

	@Transactional(noRollbackFor = AttemptConflictException.class)
	public AttemptPackageResponse startOrResume(
			StudentActor actor,
			UUID examId,
			StartAttemptRequest request
	) {
		requireActiveStudent(actor);
		Instant now = clock.instant();
		String deviceHash = sha256(request.deviceId());
		ExamAttempt existing = attemptRepository.findByExamIdAndCandidateId(examId, actor.userId())
				.orElse(null);
		if (existing != null) {
			existing = attemptRepository.findByIdForUpdate(existing.getId())
					.orElseThrow(() -> new NoSuchElementException("Attempt not found"));
			requireOwnership(existing, actor);
			if (existing.getStatus() != AttemptStatus.IN_PROGRESS) {
				throw new AttemptConflictException("A submitted attempt already exists for this exam");
			}
			if (isExpired(existing, now)) {
				score(existing, now, AttemptStatus.AUTO_SUBMITTED);
				throw new AttemptConflictException("Attempt has expired and was submitted automatically");
			}
			AttemptExamDefinition definition = examQuery.requireStartableExam(
					actor.institutionId(),
					actor.userId(),
					examId,
					request.accessPin(),
					now
			);
			if (!sameHash(existing.getDeviceIdHash(), deviceHash)) {
				throw new AttemptConflictException("Attempt is locked to another device");
			}
			return toPackage(existing, definition, now);
		}

		AttemptExamDefinition definition = examQuery.requireStartableExam(
				actor.institutionId(),
				actor.userId(),
				examId,
				request.accessPin(),
				now
		);
		existing = attemptRepository.findByExamIdAndCandidateId(examId, actor.userId()).orElse(null);
		if (existing != null) {
			existing = attemptRepository.findByIdForUpdate(existing.getId())
					.orElseThrow(() -> new NoSuchElementException("Attempt not found"));
			requireOwnership(existing, actor);
			if (existing.getStatus() != AttemptStatus.IN_PROGRESS) {
				throw new AttemptConflictException("A submitted attempt already exists for this exam");
			}
			if (isExpired(existing, now)) {
				score(existing, now, AttemptStatus.AUTO_SUBMITTED);
				throw new AttemptConflictException("Attempt has expired and was submitted automatically");
			}
			if (!sameHash(existing.getDeviceIdHash(), deviceHash)) {
				throw new AttemptConflictException("Attempt is locked to another device");
			}
			return toPackage(existing, definition, now);
		}
		Instant durationExpiry = now.plus(Duration.ofMinutes(definition.durationMinutes()));
		Instant expiresAt = durationExpiry.isBefore(definition.endsAt())
				? durationExpiry
				: definition.endsAt();
		ExamAttempt attempt = attemptRepository.saveAndFlush(new ExamAttempt(
				actor.institutionId(),
				examId,
				actor.userId(),
				deviceHash,
				now,
				expiresAt
		));
		List<SelectedSnapshot> selected = selectQuestions(definition);
		List<AttemptQuestion> snapshots = new ArrayList<>(selected.size());
		for (int index = 0; index < selected.size(); index++) {
			SelectedSnapshot selectedQuestion = selected.get(index);
			QuestionSnapshot source = selectedQuestion.question();
			AttemptQuestion question = new AttemptQuestion(
					attempt.getId(),
					source.sourceQuestionId(),
					index + 1,
					source.questionText(),
					source.type(),
					source.difficulty(),
					selectedQuestion.marks()
			);
			List<OptionSnapshot> options = new ArrayList<>(source.options());
			options.sort(Comparator.comparingInt(OptionSnapshot::displayOrder));
			if (definition.shuffleOptions()) {
				Collections.shuffle(options, secureRandom);
			}
			for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
				OptionSnapshot option = options.get(optionIndex);
				question.addOption(new AttemptOption(
						option.sourceOptionId(),
						option.optionText(),
						optionIndex + 1,
						option.correct()
				));
			}
			snapshots.add(question);
		}
		questionRepository.saveAllAndFlush(snapshots);
		return toPackage(attempt, definition, now);
	}

	@Transactional
	public AttemptPackageResponse getAttempt(StudentActor actor, UUID attemptId) {
		requireActiveStudent(actor);
		Instant now = clock.instant();
		ExamAttempt attempt = requireAttemptForUpdate(attemptId, actor);
		if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && isExpired(attempt, now)) {
			score(attempt, now, AttemptStatus.AUTO_SUBMITTED);
		}
		AttemptExamDefinition definition = examQuery.requireAssignedDefinition(
				actor.institutionId(),
				actor.userId(),
				attempt.getExamId()
		);
		return toPackage(attempt, definition, now);
	}

	@Transactional(noRollbackFor = AttemptExpiredException.class)
	public SyncAnswersResponse syncAnswers(
			StudentActor actor,
			UUID attemptId,
			SyncAnswersRequest request
	) {
		requireActiveStudent(actor);
		Instant now = clock.instant();
		ExamAttempt attempt = requireAttemptForUpdate(attemptId, actor);
		if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
			throw new AttemptConflictException("Submitted attempts cannot be changed");
		}
		if (isExpired(attempt, now)) {
			score(attempt, now, AttemptStatus.AUTO_SUBMITTED);
			throw new AttemptExpiredException();
		}

		if (syncBatchRepository.existsByAttemptIdAndSyncId(attemptId, request.syncId())) {
			return toSyncResponse(attempt, request.syncId());
		}
		Map<UUID, SyncAnswerRequest> incomingByQuestion = new LinkedHashMap<>();
		for (SyncAnswerRequest incoming : request.answers()) {
			if (incomingByQuestion.putIfAbsent(incoming.attemptQuestionId(), incoming) != null) {
				throw new IllegalArgumentException("A sync batch may contain each question only once");
			}
		}

		Map<UUID, AttemptQuestion> questions = questionRepository.findAllWithOptionsByAttemptId(attemptId)
				.stream()
				.collect(Collectors.toMap(AttemptQuestion::getId, Function.identity()));
		Map<UUID, AttemptAnswer> existingAnswers = answerRepository
				.findAllWithSelectionsByAttemptId(attemptId)
				.stream()
				.collect(Collectors.toMap(AttemptAnswer::getAttemptQuestionId, Function.identity()));
		List<AttemptAnswer> changedAnswers = new ArrayList<>();
		for (SyncAnswerRequest incoming : incomingByQuestion.values()) {
			AttemptQuestion question = questions.get(incoming.attemptQuestionId());
			if (question == null) {
				throw new IllegalArgumentException("Attempt question does not belong to this attempt");
			}
			Set<UUID> validOptionIds = question.getOptions().stream()
					.map(AttemptOption::getId)
					.collect(Collectors.toSet());
			if (!validOptionIds.containsAll(incoming.selectedOptionIds())) {
				throw new IllegalArgumentException("A selected option does not belong to the attempt question");
			}
			AttemptAnswer existingAnswer = existingAnswers.get(incoming.attemptQuestionId());
			if (existingAnswer == null) {
				AttemptAnswer created = new AttemptAnswer(
						attemptId,
						incoming.attemptQuestionId(),
						incoming.clientSequence(),
						now,
						incoming.selectedOptionIds()
				);
				existingAnswers.put(incoming.attemptQuestionId(), created);
				changedAnswers.add(created);
			}
			else if (incoming.clientSequence() > existingAnswer.getClientSequence()) {
				existingAnswer.replace(
						incoming.clientSequence(),
						now,
						incoming.selectedOptionIds()
				);
				changedAnswers.add(existingAnswer);
			}
		}
		if (!changedAnswers.isEmpty()) {
			answerRepository.saveAll(changedAnswers);
		}
		syncBatchRepository.save(new AttemptSyncBatch(attemptId, request.syncId(), now));
		attempt.markSaved(now);
		attemptRepository.saveAndFlush(attempt);
		return toSyncResponse(attempt, request.syncId(), existingAnswers.values());
	}

	@Transactional
	public AttemptResultResponse submit(StudentActor actor, UUID attemptId) {
		requireActiveStudent(actor);
		Instant now = clock.instant();
		ExamAttempt attempt = requireAttemptForUpdate(attemptId, actor);
		if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
			score(
					attempt,
					now,
					isExpired(attempt, now) ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED
			);
		}
		return toResult(attempt);
	}

	@Transactional
	public AttemptResultResponse getResult(StudentActor actor, UUID attemptId) {
		requireActiveStudent(actor);
		Instant now = clock.instant();
		ExamAttempt attempt = requireAttemptForUpdate(attemptId, actor);
		if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && isExpired(attempt, now)) {
			score(attempt, now, AttemptStatus.AUTO_SUBMITTED);
		}
		return toResult(attempt);
	}

	@Transactional
	public void autoSubmitExpired(UUID attemptId) {
		ExamAttempt attempt = attemptRepository.findByIdForUpdate(attemptId).orElse(null);
		if (attempt == null || attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
			return;
		}
		Instant now = clock.instant();
		if (isExpired(attempt, now)) {
			score(attempt, now, AttemptStatus.AUTO_SUBMITTED);
		}
	}

	@Transactional(readOnly = true)
	public List<UUID> findExpiredAttemptIds(int batchSize) {
		return attemptRepository.findExpiredIds(
				AttemptStatus.IN_PROGRESS,
				clock.instant(),
				org.springframework.data.domain.PageRequest.of(0, batchSize)
		);
	}

	private void score(ExamAttempt attempt, Instant now, AttemptStatus completedStatus) {
		if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
			return;
		}
		List<AttemptQuestion> questions = questionRepository.findAllWithOptionsByAttemptId(attempt.getId());
		Map<UUID, AttemptAnswer> answers = answerRepository
				.findAllWithSelectionsByAttemptId(attempt.getId())
				.stream()
				.collect(Collectors.toMap(AttemptAnswer::getAttemptQuestionId, Function.identity()));
		BigDecimal score = BigDecimal.ZERO;
		BigDecimal maximumScore = BigDecimal.ZERO;
		for (AttemptQuestion question : questions) {
			maximumScore = maximumScore.add(question.getMarks());
			Set<UUID> correctOptions = question.getOptions().stream()
					.filter(AttemptOption::isCorrect)
					.map(AttemptOption::getId)
					.collect(Collectors.toSet());
			AttemptAnswer answer = answers.get(question.getId());
			Set<UUID> selectedOptions = answer == null ? Set.of() : answer.getSelectedOptionIds();
			boolean correct = switch (question.getQuestionType()) {
				case SINGLE_CHOICE, TRUE_FALSE -> selectedOptions.size() == 1
						&& selectedOptions.equals(correctOptions);
				case MULTIPLE_CHOICE -> selectedOptions.equals(correctOptions);
			};
			if (correct) {
				score = score.add(question.getMarks());
			}
		}
		BigDecimal percentage = maximumScore.signum() == 0
				? BigDecimal.ZERO.setScale(2)
				: score.multiply(new BigDecimal("100"))
						.divide(maximumScore, 2, RoundingMode.HALF_UP);
		AttemptExamDefinition definition = examQuery.requireAssignedDefinition(
				attempt.getInstitutionId(),
				attempt.getCandidateId(),
				attempt.getExamId()
		);
		boolean passed = percentage.compareTo(definition.passMarkPercentage()) >= 0;
		attempt.complete(completedStatus, now, score, maximumScore, percentage, passed);
		attemptRepository.saveAndFlush(attempt);
	}

	private List<SelectedSnapshot> selectQuestions(AttemptExamDefinition definition) {
		List<SelectedSnapshot> selected = new ArrayList<>();
		Set<UUID> sourceQuestionIds = new HashSet<>();
		for (PoolRule rule : definition.poolRules()) {
			List<QuestionSnapshot> eligible = new ArrayList<>(
					questionBankQuery.findPublishedQuestionSnapshots(
							definition.institutionId(),
							definition.subjectId(),
							rule.difficulty()
					)
			);
			if (eligible.size() < rule.questionCount()) {
				throw new IllegalArgumentException(
						"The published question pool is no longer sufficient for this exam"
				);
			}
			eligible.sort(Comparator.comparing(snapshot -> snapshot.sourceQuestionId().toString()));
			Collections.shuffle(eligible, secureRandom);
			List<QuestionSnapshot> chosen = new ArrayList<>(eligible.subList(0, rule.questionCount()));
			if (!definition.shuffleQuestions()) {
				chosen.sort(Comparator.comparing(snapshot -> snapshot.sourceQuestionId().toString()));
			}
			for (QuestionSnapshot snapshot : chosen) {
				if (!sourceQuestionIds.add(snapshot.sourceQuestionId())) {
					throw new IllegalStateException("A source question was selected more than once");
				}
				selected.add(new SelectedSnapshot(snapshot, rule.marksPerQuestion()));
			}
		}
		if (definition.shuffleQuestions()) {
			Collections.shuffle(selected, secureRandom);
		}
		return selected;
	}

	private AttemptPackageResponse toPackage(
			ExamAttempt attempt,
			AttemptExamDefinition definition,
			Instant now
	) {
		Map<UUID, AttemptAnswer> answers = answerRepository
				.findAllWithSelectionsByAttemptId(attempt.getId())
				.stream()
				.collect(Collectors.toMap(AttemptAnswer::getAttemptQuestionId, Function.identity()));
		List<CandidateQuestionResponse> questions = questionRepository
				.findAllWithOptionsByAttemptId(attempt.getId())
				.stream()
				.sorted(Comparator.comparingInt(AttemptQuestion::getPosition))
				.map(question -> new CandidateQuestionResponse(
						question.getId(),
						question.getPosition(),
						question.getQuestionText(),
						question.getQuestionType(),
						question.getOptions().stream()
								.sorted(Comparator.comparingInt(AttemptOption::getDisplayOrder))
								.map(option -> new CandidateOptionResponse(
										option.getId(),
										option.getOptionText(),
										option.getDisplayOrder()
								))
								.toList()
				))
				.toList();
		List<SavedAnswerResponse> answerResponses = answers.values().stream()
				.sorted(Comparator.comparing(AttemptAnswer::getAttemptQuestionId))
				.map(AttemptService::toSavedAnswer)
				.toList();
		long remainingSeconds = attempt.getStatus() == AttemptStatus.IN_PROGRESS
				? Math.max(0, Duration.between(now, attempt.getExpiresAt()).getSeconds())
				: 0;
		return new AttemptPackageResponse(
				attempt.getId(),
				attempt.getExamId(),
				definition.code(),
				definition.title(),
				definition.instructions(),
				attempt.getStatus(),
				now,
				attempt.getExpiresAt(),
				remainingSeconds,
				questions,
				answerResponses
		);
	}

	private SyncAnswersResponse toSyncResponse(ExamAttempt attempt, UUID syncId) {
		return toSyncResponse(
				attempt,
				syncId,
				answerRepository.findAllWithSelectionsByAttemptId(attempt.getId())
		);
	}

	private static SyncAnswersResponse toSyncResponse(
			ExamAttempt attempt,
			UUID syncId,
			java.util.Collection<AttemptAnswer> answers
	) {
		return new SyncAnswersResponse(
				syncId,
				answers.stream()
						.sorted(Comparator.comparing(AttemptAnswer::getAttemptQuestionId))
						.map(AttemptService::toSavedAnswer)
						.toList(),
				attempt.getLastSavedAt(),
				attempt.getStatus()
		);
	}

	private static SavedAnswerResponse toSavedAnswer(AttemptAnswer answer) {
		return new SavedAnswerResponse(
				answer.getAttemptQuestionId(),
				answer.getSelectedOptionIds(),
				answer.getClientSequence(),
				answer.getAnsweredAt()
		);
	}

	private static AttemptResultResponse toResult(ExamAttempt attempt) {
		return new AttemptResultResponse(
				attempt.getId(),
				attempt.getStatus(),
				attempt.getSubmittedAt(),
				attempt.getScore(),
				attempt.getMaximumScore(),
				attempt.getPercentage(),
				attempt.getPassed()
		);
	}

	private ExamAttempt requireAttemptForUpdate(UUID attemptId, StudentActor actor) {
		ExamAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
				.orElseThrow(() -> new NoSuchElementException("Attempt not found"));
		requireOwnership(attempt, actor);
		return attempt;
	}

	private void requireActiveStudent(StudentActor actor) {
		if (actor == null || !actor.isStudent() || actor.institutionId() == null) {
			throw new AccessDeniedException("Student access is required");
		}
		studentQuery.requireActiveStudent(actor.institutionId(), actor.userId());
	}

	private static void requireOwnership(ExamAttempt attempt, StudentActor actor) {
		if (!attempt.getCandidateId().equals(actor.userId())
				|| !attempt.getInstitutionId().equals(actor.institutionId())) {
			throw new AccessDeniedException("Attempt access is denied");
		}
	}

	private static ExamAvailability availability(StudentExamView exam, Instant now) {
		if (now.isBefore(exam.startsAt())) {
			return ExamAvailability.UPCOMING;
		}
		if (now.isBefore(exam.endsAt())) {
			return ExamAvailability.ACTIVE;
		}
		return ExamAvailability.ENDED;
	}

	private static boolean isExpired(ExamAttempt attempt, Instant now) {
		return !now.isBefore(attempt.getExpiresAt());
	}

	private static boolean sameHash(String first, String second) {
		return MessageDigest.isEqual(
				first.getBytes(StandardCharsets.US_ASCII),
				second.getBytes(StandardCharsets.US_ASCII)
		);
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private record SelectedSnapshot(QuestionSnapshot question, BigDecimal marks) {
	}
}
