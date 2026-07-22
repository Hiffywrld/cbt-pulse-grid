package com.cbtpulsegrid.backend.institution;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

	boolean existsByCodeIgnoreCase(String code);

	@Query("""
			select institution
			from Institution institution
			where (
				:search is null
				or lower(institution.name) like lower(concat('%', :search, '%'))
				or lower(institution.code) like lower(concat('%', :search, '%'))
			)
			and (:status is null or institution.status = :status)
			""")
	Page<Institution> search(
			@Param("search") String search,
			@Param("status") InstitutionStatus status,
			Pageable pageable
	);
}
