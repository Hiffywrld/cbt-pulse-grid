package com.cbtpulsegrid.backend.examination;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class StudentExamQueryService implements StudentExamQuery {

	private final ExamRepository examRepository;
	private final ExamCandidateRepository candidateRepository;
	private final PasswordEncoder passwordEncoder;

	StudentExamQueryService(
			ExamRepository examRepository,
			ExamCandidateRepository candidateRepository,
			PasswordEncoder passwordEncoder
	) {
		this.examRepository = examRepository;
		this.candidateRepository = candidateRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional(readOnly = true)
	public List<StudentExamView> findAssignedPublishedExams(UUID institutionId, UUID candidateId) {
		Set<UUID> examIds = candidateRepository.findAllByUserId(candidateId).stream()
				.map(ExamCandidate::getExamId)
				.collect(Collectors.toSet());
		if (examIds.isEmpty()) {
			return List.of();
		}
		return examRepository.findAllByInstitutionIdAndStatusAndIdIn(
				institutionId,
				ExamStatus.PUBLISHED,
				examIds
		).stream()
				.sorted(Comparator.comparing(Exam::getStartsAt).thenComparing(Exam::getCode))
				.map(StudentExamQueryService::toView)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public StudentExamView requireAssignedPublishedExam(
			UUID institutionId,
			UUID candidateId,
			UUID examId
	) {
		Exam exam = requireAssignedExam(institutionId, candidateId, examId, false, false);
		if (exam.getStatus() != ExamStatus.PUBLISHED) {
			throw new NoSuchElementException("Exam not found");
		}
		return toView(exam);
	}

	@Override
	@Transactional
	public AttemptExamDefinition requireStartableExam(
			UUID institutionId,
			UUID candidateId,
			UUID examId,
			String accessPin,
			Instant now
	) {
		Exam exam = requireAssignedExam(institutionId, candidateId, examId, true, true);
		if (exam.getStatus() != ExamStatus.PUBLISHED) {
			throw new IllegalArgumentException("Exam is not available");
		}
		if (now.isBefore(exam.getStartsAt()) || !now.isBefore(exam.getEndsAt())) {
			throw new IllegalArgumentException("Exam is outside its active window");
		}
		if (accessPin == null || !passwordEncoder.matches(accessPin, exam.accessPinHash())) {
			throw new IllegalArgumentException("Exam access credentials are invalid");
		}
		return toDefinition(exam);
	}

	@Override
	@Transactional(readOnly = true)
	public AttemptExamDefinition requireAssignedDefinition(
			UUID institutionId,
			UUID candidateId,
			UUID examId
	) {
		return toDefinition(requireAssignedExam(institutionId, candidateId, examId, true, false));
	}

	private Exam requireAssignedExam(
			UUID institutionId,
			UUID candidateId,
			UUID examId,
			boolean withPoolRules,
			boolean lockAssignment
	) {
		Exam exam = (withPoolRules
				? examRepository.findWithPoolRulesById(examId)
				: examRepository.findById(examId))
				.orElseThrow(() -> new NoSuchElementException("Exam not found"));
		if (!institutionId.equals(exam.getInstitutionId())) {
			throw new AccessDeniedException("Cross-institution exam access is denied");
		}
		boolean assigned = lockAssignment
				? candidateRepository.findByExamIdAndUserIdForUpdate(examId, candidateId).isPresent()
				: candidateRepository.existsByExamIdAndUserId(examId, candidateId);
		if (!assigned) {
			throw new AccessDeniedException("Student is not assigned to this exam");
		}
		return exam;
	}

	private static StudentExamView toView(Exam exam) {
		return new StudentExamView(
				exam.getId(),
				exam.getCode(),
				exam.getTitle(),
				exam.getInstructions(),
				exam.getDurationMinutes(),
				exam.getStartsAt(),
				exam.getEndsAt()
		);
	}

	private static AttemptExamDefinition toDefinition(Exam exam) {
		return new AttemptExamDefinition(
				exam.getId(),
				exam.getInstitutionId(),
				exam.getSubjectId(),
				exam.getCode(),
				exam.getTitle(),
				exam.getInstructions(),
				exam.getDurationMinutes(),
				exam.getStartsAt(),
				exam.getEndsAt(),
				exam.isShuffleQuestions(),
				exam.isShuffleOptions(),
				exam.getPassMarkPercentage(),
				exam.getPoolRules().stream()
						.map(rule -> new PoolRule(
								rule.getDifficulty(),
								rule.getQuestionCount(),
								rule.getMarksPerQuestion()
						))
						.toList()
		);
	}
}
