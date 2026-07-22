package com.cbtpulsegrid.backend.identity;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByInstitutionIdAndRegistrationNumberIgnoreCase(
			UUID institutionId,
			String registrationNumber
	);

	boolean existsByInstitutionIdAndRegistrationNumberIgnoreCaseAndIdNot(
			UUID institutionId,
			String registrationNumber,
			UUID id
	);

	long countByRolesContaining(Role role);

	@Query("""
			select user
			from User user
			where (
				:search is null
				or lower(user.firstName) like lower(concat('%', :search, '%'))
				or lower(user.lastName) like lower(concat('%', :search, '%'))
				or lower(concat(user.firstName, ' ', user.lastName)) like lower(concat('%', :search, '%'))
				or lower(user.email) like lower(concat('%', :search, '%'))
				or lower(user.registrationNumber) like lower(concat('%', :search, '%'))
			)
			and (:institutionId is null or user.institutionId = :institutionId)
			and (:role is null or :role member of user.roles)
			and (:status is null or user.status = :status)
			""")
	Page<User> search(
			@Param("search") String search,
			@Param("institutionId") UUID institutionId,
			@Param("role") Role role,
			@Param("status") UserStatus status,
			Pageable pageable
	);
}
