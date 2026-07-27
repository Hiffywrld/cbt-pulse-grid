package com.cbtpulsegrid.backend.identity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

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
			select distinct user
			from User user
			left join fetch user.roles
			where user.id = :id
			""")
	Optional<User> findWithRolesById(@Param("id") UUID id);

	@Query("""
			select distinct user
			from User user
			left join fetch user.roles
			where user.id in :ids
			""")
	List<User> findAllWithRolesByIdIn(@Param("ids") Collection<UUID> ids);

}
