package com.cbtpulsegrid.backend.questionbank;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

	boolean existsByInstitutionIdAndCodeIgnoreCase(UUID institutionId, String code);

	boolean existsByInstitutionIdAndCodeIgnoreCaseAndIdNot(UUID institutionId, String code, UUID id);

	@Query("""
			select subject
			from Subject subject
			where subject.institutionId = :institutionId
			and (
				:search is null
				or lower(subject.code) like lower(concat('%', :search, '%'))
				or lower(subject.name) like lower(concat('%', :search, '%'))
			)
			and (:status is null or subject.status = :status)
			""")
	Page<Subject> search(
			@Param("institutionId") UUID institutionId,
			@Param("search") String search,
			@Param("status") SubjectStatus status,
			Pageable pageable
	);
}
