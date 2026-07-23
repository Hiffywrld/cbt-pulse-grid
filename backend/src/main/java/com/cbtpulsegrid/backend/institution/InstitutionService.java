package com.cbtpulsegrid.backend.institution;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditAction;
import com.cbtpulsegrid.backend.audit.AuditResourceType;
import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.institution.api.CreateInstitutionRequest;
import com.cbtpulsegrid.backend.institution.api.InstitutionResponse;
import com.cbtpulsegrid.backend.institution.api.PageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionService {

	private static final int MAX_PAGE_SIZE = 100;

	private final InstitutionRepository institutionRepository;
	private final AuditTrail auditTrail;

	public InstitutionService(InstitutionRepository institutionRepository, AuditTrail auditTrail) {
		this.institutionRepository = institutionRepository;
		this.auditTrail = auditTrail;
	}

	@Transactional
	public InstitutionResponse create(CreateInstitutionRequest request) {
		String normalizedCode = normalizeCode(request.code());
		if (institutionRepository.existsByCodeIgnoreCase(normalizedCode)) {
			throw new DuplicateKeyException("Institution code already exists");
		}

		Institution institution = new Institution(
				request.name().trim(),
				normalizedCode,
				InstitutionStatus.ACTIVE
		);
		try {
			Institution saved = institutionRepository.saveAndFlush(institution);
			auditTrail.record(
					saved.getId(),
					AuditAction.INSTITUTION_CREATED,
					AuditResourceType.INSTITUTION,
					saved.getId(),
					java.util.Map.of("code", saved.getCode(), "status", saved.getStatus())
			);
			return toResponse(saved);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateKeyException("Institution code already exists", exception);
		}
	}

	@Transactional(readOnly = true)
	public PageResponse<InstitutionResponse> list(
			String search,
			InstitutionStatus status,
			int page,
			int size
	) {
		validatePage(page, size);
		String normalizedSearch = normalizeSearch(search);
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
		Page<InstitutionResponse> result = institutionRepository
				.search(normalizedSearch, status, pageRequest)
				.map(InstitutionService::toResponse);
		return PageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public InstitutionResponse get(UUID id) {
		return toResponse(findInstitution(id));
	}

	@Transactional(readOnly = true)
	public void requireActive(UUID id) {
		Institution institution = findInstitution(id);
		if (institution.getStatus() != InstitutionStatus.ACTIVE) {
			throw new IllegalArgumentException("Institution must be ACTIVE");
		}
	}

	@Transactional
	public InstitutionResponse updateName(UUID id, String name) {
		Institution institution = findInstitution(id);
		institution.setName(name.trim());
		Institution saved = institutionRepository.saveAndFlush(institution);
		auditTrail.record(id, AuditAction.INSTITUTION_UPDATED, AuditResourceType.INSTITUTION, id, java.util.Map.of());
		return toResponse(saved);
	}

	@Transactional
	public InstitutionResponse changeStatus(UUID id, InstitutionStatus status) {
		Institution institution = findInstitution(id);
		institution.setStatus(status);
		Institution saved = institutionRepository.saveAndFlush(institution);
		auditTrail.record(
				id,
				AuditAction.INSTITUTION_STATUS_CHANGED,
				AuditResourceType.INSTITUTION,
				id,
				java.util.Map.of("status", status)
		);
		return toResponse(saved);
	}

	private Institution findInstitution(UUID id) {
		return institutionRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Institution not found"));
	}

	private static String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
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

	private static InstitutionResponse toResponse(Institution institution) {
		return new InstitutionResponse(
				institution.getId(),
				institution.getName(),
				institution.getCode(),
				institution.getStatus(),
				institution.getCreatedAt(),
				institution.getUpdatedAt(),
				institution.getVersion()
		);
	}
}
