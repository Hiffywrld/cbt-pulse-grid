package com.cbtpulsegrid.backend.examination;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, UUID>, JpaSpecificationExecutor<Exam> {

	boolean existsByInstitutionIdAndCodeIgnoreCase(UUID institutionId, String code);

	boolean existsByInstitutionIdAndCodeIgnoreCaseAndIdNot(UUID institutionId, String code, UUID id);

	@Query("""
			select distinct exam
			from Exam exam
			left join fetch exam.poolRules
			where exam.id = :id
			""")
	Optional<Exam> findWithPoolRulesById(@Param("id") UUID id);
}
