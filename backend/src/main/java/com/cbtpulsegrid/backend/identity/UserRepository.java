package com.cbtpulsegrid.backend.identity;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmailIgnoreCase(String email);

	long countByRolesContaining(Role role);
}
