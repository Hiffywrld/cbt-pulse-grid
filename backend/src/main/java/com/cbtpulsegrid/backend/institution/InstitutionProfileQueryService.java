package com.cbtpulsegrid.backend.institution;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InstitutionProfileQueryService implements InstitutionProfileQuery {

	private final InstitutionRepository institutionRepository;

	InstitutionProfileQueryService(InstitutionRepository institutionRepository) {
		this.institutionRepository = institutionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public InstitutionProfile requireProfile(UUID institutionId) {
		Institution institution = institutionRepository.findById(institutionId)
				.orElseThrow(() -> new NoSuchElementException("Institution not found"));
		return new InstitutionProfile(institution.getId(), institution.getName(), institution.getCode());
	}
}
