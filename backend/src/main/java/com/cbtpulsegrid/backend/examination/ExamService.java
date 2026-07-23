package com.cbtpulsegrid.backend.examination;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditAction;
import com.cbtpulsegrid.backend.audit.AuditResourceType;
import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.examination.api.AssignExamCandidatesRequest;
import com.cbtpulsegrid.backend.examination.api.CreateExamRequest;
import com.cbtpulsegrid.backend.examination.api.ExamActor;
import com.cbtpulsegrid.backend.examination.api.ExamCandidateResponse;
import com.cbtpulsegrid.backend.examination.api.ExamDetailResponse;
import com.cbtpulsegrid.backend.examination.api.ExamPageResponse;
import com.cbtpulsegrid.backend.examination.api.ExamPoolRuleRequest;
import com.cbtpulsegrid.backend.examination.api.ExamPoolRuleResponse;
import com.cbtpulsegrid.backend.examination.api.ExamSummaryResponse;
import com.cbtpulsegrid.backend.examination.api.UpdateExamRequest;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery.CandidateProfile;
import com.cbtpulsegrid.backend.institution.InstitutionService;
import com.cbtpulsegrid.backend.questionbank.ExaminationQuestionBankQuery;
import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final int MAX_DURATION_MINUTES = 480;

	private final ExamRepository examRepository;
	private final ExamCandidateRepository examCandidateRepository;
	private final InstitutionService institutionService;
	private final ExaminationQuestionBankQuery questionBankQuery;
	private final ExaminationCandidateQuery candidateQuery;
	private final PasswordEncoder passwordEncoder;
	private final ExamAuthorization authorization;
	private final AuditTrail auditTrail;

	public ExamService(
			ExamRepository examRepository,
			ExamCandidateRepository examCandidateRepository,
			InstitutionService institutionService,
			ExaminationQuestionBankQuery questionBankQuery,
			ExaminationCandidateQuery candidateQuery,
			PasswordEncoder passwordEncoder,
			ExamAuthorization authorization,
			AuditTrail auditTrail
	) {
		this.examRepository = examRepository;
		this.examCandidateRepository = examCandidateRepository;
		this.institutionService = institutionService;
		this.questionBankQuery = questionBankQuery;
		this.candidateQuery = candidateQuery;
		this.passwordEncoder = passwordEncoder;
		this.authorization = authorization;
		this.auditTrail = auditTrail;
	}

	@Transactional
	public ExamDetailResponse create(ExamActor actor, CreateExamRequest request) {
		UUID institutionId = authorization.requireManagementAccess(actor);
		institutionService.requireActive(institutionId);
		questionBankQuery.requireActiveSubject(institutionId, request.subjectId());
		validateDefinition(
				request.subjectId(),
				request.title(),
				request.durationMinutes(),
				request.startsAt(),
				request.endsAt(),
				request.poolRules()
		);
		validatePin(request.accessPin());

		String code = normalizeCode(request.code());
		assertUniqueCode(institutionId, code, null);
		Exam exam = new Exam(
				institutionId,
				request.subjectId(),
				actor.userId(),
				code,
				request.title().trim(),
				normalizeInstructions(request.instructions()),
				request.durationMinutes(),
				request.startsAt(),
				request.endsAt(),
				passwordEncoder.encode(request.accessPin()),
				request.shuffleQuestions(),
				request.shuffleOptions(),
				normalizePassMark(request.passMarkPercentage()),
				ExamStatus.DRAFT
		);
		exam.replacePoolRules(toPoolRules(request.poolRules()));
		try {
			Exam saved = examRepository.saveAndFlush(exam);
			auditTrail.record(
					institutionId,
					AuditAction.EXAM_CREATED,
					AuditResourceType.EXAM,
					saved.getId(),
					Map.of("code", saved.getCode(), "status", saved.getStatus())
			);
			return toDetail(saved);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateKeyException("Exam code already exists", exception);
		}
	}

	@Transactional(readOnly = true)
	public ExamPageResponse<ExamSummaryResponse> list(
			ExamActor actor,
			String search,
			UUID subjectId,
			ExamStatus status,
			int page,
			int size
	) {
		UUID institutionId = authorization.requireReadAccess(actor);
		institutionService.requireActive(institutionId);
		validatePage(page, size);
		if (subjectId != null) {
			questionBankQuery.requireActiveSubject(institutionId, subjectId);
		}
		ExamStatus visibleStatus = authorization.resolveListStatus(actor, status);
		PageRequest pageRequest = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Order.desc("startsAt"), Sort.Order.asc("code"))
		);
		Page<ExamSummaryResponse> result = examRepository.findAll(
				ExamSpecifications.filteredBy(
						institutionId,
						subjectId,
						visibleStatus,
						normalizeSearch(search)
				),
				pageRequest
		).map(ExamService::toSummary);
		return ExamPageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public ExamDetailResponse get(ExamActor actor, UUID id) {
		UUID institutionId = authorization.requireReadAccess(actor);
		institutionService.requireActive(institutionId);
		Exam exam = requireOwnedExamWithRules(institutionId, id);
		authorization.requireReadableStatus(actor, exam);
		return toDetail(exam);
	}

	@Transactional
	public ExamDetailResponse update(ExamActor actor, UUID id, UpdateExamRequest request) {
		UUID institutionId = authorization.requireManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Exam exam = requireOwnedExamWithRules(institutionId, id);
		requireDraft(exam);
		questionBankQuery.requireActiveSubject(institutionId, request.subjectId());
		validateDefinition(
				request.subjectId(),
				request.title(),
				request.durationMinutes(),
				request.startsAt(),
				request.endsAt(),
				request.poolRules()
		);

		String code = normalizeCode(request.code());
		assertUniqueCode(institutionId, code, id);
		exam.updateDefinition(
				request.subjectId(),
				code,
				request.title().trim(),
				normalizeInstructions(request.instructions()),
				request.durationMinutes(),
				request.startsAt(),
				request.endsAt(),
				request.shuffleQuestions(),
				request.shuffleOptions(),
				normalizePassMark(request.passMarkPercentage())
		);
		exam.replacePoolRules(toPoolRules(request.poolRules()));
		try {
			Exam saved = examRepository.saveAndFlush(exam);
			auditTrail.record(institutionId, AuditAction.EXAM_UPDATED, AuditResourceType.EXAM, id, Map.of("status", saved.getStatus()));
			return toDetail(saved);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateKeyException("Exam code already exists", exception);
		}
	}

	@Transactional
	public ExamDetailResponse publish(ExamActor actor, UUID id) {
		UUID institutionId = authorization.requireManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Exam exam = requireOwnedExamWithRules(institutionId, id);
		requireDraft(exam);
		questionBankQuery.requireActiveSubject(institutionId, exam.getSubjectId());
		validateDefinition(
				exam.getSubjectId(),
				exam.getTitle(),
				exam.getDurationMinutes(),
				exam.getStartsAt(),
				exam.getEndsAt(),
				toPoolRuleRequests(exam.getPoolRules())
		);
		normalizePassMark(exam.getPassMarkPercentage());
		if (!exam.isAccessPinConfigured()) {
			throw new IllegalArgumentException("Exam access PIN must be configured before publishing");
		}
		if (examCandidateRepository.countByExamId(exam.getId()) < 1) {
			throw new IllegalArgumentException("At least one candidate must be assigned before publishing");
		}
		for (ExamPoolRule rule : exam.getPoolRules()) {
			long available = questionBankQuery.countPublishedQuestions(
					institutionId,
					exam.getSubjectId(),
					rule.getDifficulty()
			);
			if (available < rule.getQuestionCount()) {
				throw new IllegalArgumentException(
						"Not enough PUBLISHED questions for difficulty " + rule.getDifficulty()
				);
			}
		}
		exam.setStatus(ExamStatus.PUBLISHED);
		Exam saved = examRepository.saveAndFlush(exam);
		auditTrail.record(institutionId, AuditAction.EXAM_PUBLISHED, AuditResourceType.EXAM, id, Map.of("status", saved.getStatus()));
		return toDetail(saved);
	}

	@Transactional
	public ExamDetailResponse cancel(ExamActor actor, UUID id) {
		UUID institutionId = authorization.requireManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Exam exam = requireOwnedExamWithRules(institutionId, id);
		if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.PUBLISHED) {
			throw new IllegalArgumentException("Only DRAFT or PUBLISHED exams may be cancelled");
		}
		exam.setStatus(ExamStatus.CANCELLED);
		Exam saved = examRepository.saveAndFlush(exam);
		auditTrail.record(institutionId, AuditAction.EXAM_CANCELLED, AuditResourceType.EXAM, id, Map.of("status", saved.getStatus()));
		return toDetail(saved);
	}

	@Transactional
	public ExamDetailResponse close(ExamActor actor, UUID id) {
		UUID institutionId = authorization.requireManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Exam exam = requireOwnedExamWithRules(institutionId, id);
		if (exam.getStatus() != ExamStatus.PUBLISHED) {
			throw new IllegalArgumentException("Only PUBLISHED exams may be closed");
		}
		exam.setStatus(ExamStatus.CLOSED);
		Exam saved = examRepository.saveAndFlush(exam);
		auditTrail.record(institutionId, AuditAction.EXAM_CLOSED, AuditResourceType.EXAM, id, Map.of("status", saved.getStatus()));
		return toDetail(saved);
	}

	@Transactional
	public ExamDetailResponse rotateAccessPin(ExamActor actor, UUID id, String accessPin) {
		UUID institutionId = authorization.requireManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Exam exam = requireOwnedExamWithRules(institutionId, id);
		requireDraft(exam);
		validatePin(accessPin);
		exam.rotateAccessPin(passwordEncoder.encode(accessPin));
		Exam saved = examRepository.saveAndFlush(exam);
		auditTrail.record(institutionId, AuditAction.EXAM_ACCESS_PIN_ROTATED, AuditResourceType.EXAM, id, Map.of());
		return toDetail(saved);
	}

	@Transactional
	public List<ExamCandidateResponse> assignCandidates(
			ExamActor actor,
			UUID id,
			AssignExamCandidatesRequest request
	) {
		UUID institutionId = authorization.requireManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Exam exam = requireOwnedExam(institutionId, id);
		requireDraft(exam);
		Set<UUID> userIds = validateCandidateIds(request.userIds());
		Map<UUID, CandidateProfile> profiles = candidateQuery.requireActiveStudents(institutionId, userIds);
		if (!examCandidateRepository.findAllByExamIdAndUserIdIn(id, userIds).isEmpty()) {
			throw new DuplicateKeyException("One or more candidates are already assigned to this exam");
		}

		List<ExamCandidate> assignments = request.userIds().stream()
				.map(userId -> new ExamCandidate(id, userId, actor.userId()))
				.toList();
		try {
			List<ExamCandidate> saved = examCandidateRepository.saveAllAndFlush(assignments);
			auditTrail.record(
					institutionId,
					AuditAction.EXAM_CANDIDATES_ASSIGNED,
					AuditResourceType.EXAM,
					id,
					Map.of("candidateCount", saved.size())
			);
			return saved.stream()
					.map(assignment -> toCandidateResponse(assignment, requireProfile(profiles, assignment.getUserId())))
					.toList();
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateKeyException("One or more candidates are already assigned to this exam", exception);
		}
	}

	@Transactional(readOnly = true)
	public ExamPageResponse<ExamCandidateResponse> listCandidates(
			ExamActor actor,
			UUID id,
			int page,
			int size
	) {
		UUID institutionId = authorization.requireReadAccess(actor);
		institutionService.requireActive(institutionId);
		Exam exam = requireOwnedExam(institutionId, id);
		authorization.requireReadableStatus(actor, exam);
		validatePage(page, size);
		PageRequest pageRequest = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Order.desc("assignedAt"), Sort.Order.asc("id"))
		);
		Page<ExamCandidate> assignments = examCandidateRepository.findByExamId(id, pageRequest);
		Set<UUID> userIds = assignments.getContent().stream()
				.map(ExamCandidate::getUserId)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		Map<UUID, CandidateProfile> profiles = candidateQuery.findByIds(userIds);
		return ExamPageResponse.from(assignments.map(
				assignment -> toCandidateResponse(
						assignment,
						requireProfile(profiles, assignment.getUserId())
				)
		));
	}

	@Transactional
	public void removeCandidate(ExamActor actor, UUID id, UUID userId) {
		UUID institutionId = authorization.requireManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Exam exam = requireOwnedExam(institutionId, id);
		requireDraft(exam);
		ExamCandidate assignment = examCandidateRepository.findByExamIdAndUserId(id, userId)
				.orElseThrow(() -> new NoSuchElementException("Exam candidate assignment not found"));
		examCandidateRepository.delete(assignment);
		examCandidateRepository.flush();
		auditTrail.record(
				institutionId,
				AuditAction.EXAM_CANDIDATE_REMOVED,
				AuditResourceType.EXAM_CANDIDATE,
				assignment.getId(),
				Map.of("examId", id, "candidateId", userId)
		);
	}

	private Exam requireOwnedExam(UUID institutionId, UUID id) {
		Exam exam = examRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Exam not found"));
		authorization.requireTenant(institutionId, exam.getInstitutionId());
		return exam;
	}

	private Exam requireOwnedExamWithRules(UUID institutionId, UUID id) {
		Exam exam = examRepository.findWithPoolRulesById(id)
				.orElseThrow(() -> new NoSuchElementException("Exam not found"));
		authorization.requireTenant(institutionId, exam.getInstitutionId());
		return exam;
	}

	private void assertUniqueCode(UUID institutionId, String code, UUID existingId) {
		boolean duplicate = existingId == null
				? examRepository.existsByInstitutionIdAndCodeIgnoreCase(institutionId, code)
				: examRepository.existsByInstitutionIdAndCodeIgnoreCaseAndIdNot(institutionId, code, existingId);
		if (duplicate) {
			throw new DuplicateKeyException("Exam code already exists");
		}
	}

	private static void requireDraft(Exam exam) {
		if (exam.getStatus() != ExamStatus.DRAFT) {
			throw new IllegalArgumentException("Only DRAFT exams may be changed");
		}
	}

	private static void validateDefinition(
			UUID subjectId,
			String title,
			int durationMinutes,
			Instant startsAt,
			Instant endsAt,
			List<ExamPoolRuleRequest> poolRules
	) {
		if (subjectId == null) {
			throw new IllegalArgumentException("subjectId is required");
		}
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("title is required");
		}
		if (durationMinutes < 1 || durationMinutes > MAX_DURATION_MINUTES) {
			throw new IllegalArgumentException("durationMinutes must be between 1 and 480");
		}
		if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
			throw new IllegalArgumentException("startsAt must be before endsAt");
		}
		if (Duration.between(startsAt, endsAt).compareTo(Duration.ofMinutes(durationMinutes)) < 0) {
			throw new IllegalArgumentException("Exam window must be at least as long as durationMinutes");
		}
		validatePoolRules(poolRules);
	}

	private static void validatePoolRules(List<ExamPoolRuleRequest> poolRules) {
		if (poolRules == null || poolRules.isEmpty()) {
			throw new IllegalArgumentException("At least one pool rule is required");
		}
		Set<QuestionDifficulty> difficulties = EnumSet.noneOf(QuestionDifficulty.class);
		for (ExamPoolRuleRequest rule : poolRules) {
			if (rule == null || rule.difficulty() == null) {
				throw new IllegalArgumentException("Pool-rule difficulty is required");
			}
			if (!difficulties.add(rule.difficulty())) {
				throw new IllegalArgumentException("Each pool-rule difficulty may appear only once");
			}
			if (rule.questionCount() < 1) {
				throw new IllegalArgumentException("Pool-rule questionCount must be greater than zero");
			}
			BigDecimal marks = rule.marksPerQuestion();
			if (marks == null || marks.signum() <= 0) {
				throw new IllegalArgumentException("Pool-rule marksPerQuestion must be greater than zero");
			}
		}
	}

	private static Set<UUID> validateCandidateIds(List<UUID> candidateIds) {
		if (candidateIds == null || candidateIds.isEmpty()) {
			throw new IllegalArgumentException("At least one candidate userId is required");
		}
		if (candidateIds.stream().anyMatch(java.util.Objects::isNull)) {
			throw new IllegalArgumentException("Candidate userIds must not contain null values");
		}
		Set<UUID> uniqueIds = new LinkedHashSet<>(candidateIds);
		if (uniqueIds.size() != candidateIds.size()) {
			throw new IllegalArgumentException("Candidate userIds must not contain duplicates");
		}
		return Set.copyOf(uniqueIds);
	}

	private static void validatePin(String accessPin) {
		if (accessPin == null || !accessPin.matches("\\d{6}")) {
			throw new IllegalArgumentException("accessPin must contain exactly six digits");
		}
	}

	private static String normalizeCode(String code) {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("code is required");
		}
		return code.trim().toUpperCase(Locale.ROOT);
	}

	private static String normalizeInstructions(String instructions) {
		return instructions == null || instructions.isBlank() ? null : instructions.trim();
	}

	private static String normalizeSearch(String search) {
		return search == null || search.isBlank() ? null : search.trim();
	}

	private static BigDecimal normalizePassMark(BigDecimal passMarkPercentage) {
		BigDecimal value = passMarkPercentage == null
				? new BigDecimal("50.00")
				: passMarkPercentage;
		if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100.00")) > 0) {
			throw new IllegalArgumentException("passMarkPercentage must be between 0 and 100");
		}
		return value;
	}

	private static void validatePage(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must not be negative");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Page size must be between 1 and 100");
		}
	}

	private static List<ExamPoolRule> toPoolRules(List<ExamPoolRuleRequest> poolRules) {
		return poolRules.stream()
				.map(rule -> new ExamPoolRule(
						rule.difficulty(),
						rule.questionCount(),
						rule.marksPerQuestion()
				))
				.toList();
	}

	private static List<ExamPoolRuleRequest> toPoolRuleRequests(List<ExamPoolRule> poolRules) {
		return poolRules.stream()
				.map(rule -> new ExamPoolRuleRequest(
						rule.getDifficulty(),
						rule.getQuestionCount(),
						rule.getMarksPerQuestion()
				))
				.toList();
	}

	private static CandidateProfile requireProfile(Map<UUID, CandidateProfile> profiles, UUID userId) {
		CandidateProfile profile = profiles.get(userId);
		if (profile == null) {
			throw new IllegalStateException("Candidate profile could not be loaded");
		}
		return profile;
	}

	private static ExamSummaryResponse toSummary(Exam exam) {
		return new ExamSummaryResponse(
				exam.getId(),
				exam.getInstitutionId(),
				exam.getSubjectId(),
				exam.getCode(),
				exam.getTitle(),
				exam.getDurationMinutes(),
				exam.getStartsAt(),
				exam.getEndsAt(),
				exam.isShuffleQuestions(),
				exam.isShuffleOptions(),
				exam.getStatus(),
				exam.getCreatedAt(),
				exam.getUpdatedAt(),
				exam.getVersion(),
				exam.getPassMarkPercentage()
		);
	}

	private static ExamDetailResponse toDetail(Exam exam) {
		List<ExamPoolRuleResponse> poolRules = exam.getPoolRules().stream()
				.sorted(Comparator.comparing(ExamPoolRule::getDifficulty))
				.map(rule -> new ExamPoolRuleResponse(
						rule.getId(),
						rule.getDifficulty(),
						rule.getQuestionCount(),
						rule.getMarksPerQuestion()
				))
				.toList();
		return new ExamDetailResponse(
				exam.getId(),
				exam.getInstitutionId(),
				exam.getSubjectId(),
				exam.getCreatedBy(),
				exam.getCode(),
				exam.getTitle(),
				exam.getInstructions(),
				exam.getDurationMinutes(),
				exam.getStartsAt(),
				exam.getEndsAt(),
				exam.isAccessPinConfigured(),
				exam.isShuffleQuestions(),
				exam.isShuffleOptions(),
				exam.getStatus(),
				poolRules,
				exam.getCreatedAt(),
				exam.getUpdatedAt(),
				exam.getVersion(),
				exam.getPassMarkPercentage()
		);
	}

	private static ExamCandidateResponse toCandidateResponse(
			ExamCandidate assignment,
			CandidateProfile profile
	) {
		return new ExamCandidateResponse(
				assignment.getId(),
				assignment.getUserId(),
				profile.firstName(),
				profile.lastName(),
				profile.email(),
				profile.registrationNumber(),
				profile.status().name(),
				assignment.getAssignedBy(),
				assignment.getAssignedAt()
		);
	}
}
