package com.cbtpulsegrid.backend.questionbank;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditAction;
import com.cbtpulsegrid.backend.audit.AuditResourceType;
import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.institution.InstitutionService;
import com.cbtpulsegrid.backend.questionbank.api.CreateSubjectRequest;
import com.cbtpulsegrid.backend.questionbank.api.QuestionBankActor;
import com.cbtpulsegrid.backend.questionbank.api.QuestionBankPageResponse;
import com.cbtpulsegrid.backend.questionbank.api.SubjectResponse;
import com.cbtpulsegrid.backend.questionbank.api.UpdateSubjectRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

	private static final int MAX_PAGE_SIZE = 100;

	private final SubjectRepository subjectRepository;
	private final InstitutionService institutionService;
	private final QuestionBankAuthorization authorization;
	private final AuditTrail auditTrail;

	public SubjectService(
			SubjectRepository subjectRepository,
			InstitutionService institutionService,
			QuestionBankAuthorization authorization,
			AuditTrail auditTrail
	) {
		this.subjectRepository = subjectRepository;
		this.institutionService = institutionService;
		this.authorization = authorization;
		this.auditTrail = auditTrail;
	}

	@Transactional
	public SubjectResponse create(QuestionBankActor actor, CreateSubjectRequest request) {
		UUID institutionId = authorization.requireSubjectManagementAccess(actor);
		institutionService.requireActive(institutionId);
		String code = normalizeCode(request.code());
		if (subjectRepository.existsByInstitutionIdAndCodeIgnoreCase(institutionId, code)) {
			throw new DuplicateKeyException("Subject code already exists");
		}

		Subject subject = new Subject(
				institutionId,
				code,
				request.name().trim(),
				normalizeDescription(request.description()),
				SubjectStatus.ACTIVE
		);
		try {
			Subject saved = subjectRepository.saveAndFlush(subject);
			auditTrail.record(
					institutionId,
					AuditAction.SUBJECT_CREATED,
					AuditResourceType.SUBJECT,
					saved.getId(),
					java.util.Map.of("code", saved.getCode(), "status", saved.getStatus())
			);
			return toResponse(saved);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateKeyException("Subject code already exists", exception);
		}
	}

	@Transactional(readOnly = true)
	public QuestionBankPageResponse<SubjectResponse> list(
			QuestionBankActor actor,
			String search,
			SubjectStatus status,
			int page,
			int size
	) {
		UUID institutionId = authorization.requireSubjectReadAccess(actor);
		institutionService.requireActive(institutionId);
		validatePage(page, size);
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "code"));
		Page<SubjectResponse> result = subjectRepository
				.search(institutionId, normalizeSearch(search), status, pageRequest)
				.map(SubjectService::toResponse);
		return QuestionBankPageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public SubjectResponse get(QuestionBankActor actor, UUID id) {
		UUID institutionId = authorization.requireSubjectReadAccess(actor);
		institutionService.requireActive(institutionId);
		return toResponse(requireOwnedSubject(institutionId, id));
	}

	@Transactional
	public SubjectResponse update(QuestionBankActor actor, UUID id, UpdateSubjectRequest request) {
		UUID institutionId = authorization.requireSubjectManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Subject subject = requireOwnedSubject(institutionId, id);
		String code = normalizeCode(request.code());
		if (subjectRepository.existsByInstitutionIdAndCodeIgnoreCaseAndIdNot(institutionId, code, id)) {
			throw new DuplicateKeyException("Subject code already exists");
		}

		subject.setCode(code);
		subject.setName(request.name().trim());
		subject.setDescription(normalizeDescription(request.description()));
		try {
			Subject saved = subjectRepository.saveAndFlush(subject);
			auditTrail.record(institutionId, AuditAction.SUBJECT_UPDATED, AuditResourceType.SUBJECT, id, java.util.Map.of());
			return toResponse(saved);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateKeyException("Subject code already exists", exception);
		}
	}

	@Transactional
	public SubjectResponse changeStatus(QuestionBankActor actor, UUID id, SubjectStatus status) {
		UUID institutionId = authorization.requireSubjectManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Subject subject = requireOwnedSubject(institutionId, id);
		subject.setStatus(status);
		Subject saved = subjectRepository.saveAndFlush(subject);
		auditTrail.record(
				institutionId,
				AuditAction.SUBJECT_STATUS_CHANGED,
				AuditResourceType.SUBJECT,
				id,
				java.util.Map.of("status", status)
		);
		return toResponse(saved);
	}

	Subject requireOwnedSubject(UUID institutionId, UUID id) {
		Subject subject = findSubject(id);
		authorization.requireTenant(institutionId, subject.getInstitutionId());
		return subject;
	}

	Subject requireActiveSubject(UUID institutionId, UUID id) {
		Subject subject = requireOwnedSubject(institutionId, id);
		if (subject.getStatus() != SubjectStatus.ACTIVE) {
			throw new IllegalArgumentException("Subject must be ACTIVE");
		}
		return subject;
	}

	private Subject findSubject(UUID id) {
		return subjectRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Subject not found"));
	}

	private static String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
	}

	private static String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.trim();
	}

	private static String normalizeSearch(String search) {
		if (search == null || search.isBlank()) {
			return null;
		}
		return search.trim();
	}

	private static void validatePage(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must not be negative");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Page size must be between 1 and 100");
		}
	}

	private static SubjectResponse toResponse(Subject subject) {
		return new SubjectResponse(
				subject.getId(),
				subject.getInstitutionId(),
				subject.getCode(),
				subject.getName(),
				subject.getDescription(),
				subject.getStatus(),
				subject.getCreatedAt(),
				subject.getUpdatedAt(),
				subject.getVersion()
		);
	}
}
