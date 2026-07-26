package com.cbtpulsegrid.backend.questionbank;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubjectRepository extends JpaRepository<Subject, UUID>, JpaSpecificationExecutor<Subject> {

	boolean existsByInstitutionIdAndCodeIgnoreCase(UUID institutionId, String code);

	boolean existsByInstitutionIdAndCodeIgnoreCaseAndIdNot(UUID institutionId, String code, UUID id);

}
