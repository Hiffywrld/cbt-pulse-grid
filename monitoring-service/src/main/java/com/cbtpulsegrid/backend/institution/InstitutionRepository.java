package com.cbtpulsegrid.backend.institution;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstitutionRepository extends JpaRepository<Institution, UUID>,
		JpaSpecificationExecutor<Institution> {

	boolean existsByCodeIgnoreCase(String code);
}
