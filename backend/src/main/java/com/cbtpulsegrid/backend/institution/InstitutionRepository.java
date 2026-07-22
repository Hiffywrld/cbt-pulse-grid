package com.cbtpulsegrid.backend.institution;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
}
