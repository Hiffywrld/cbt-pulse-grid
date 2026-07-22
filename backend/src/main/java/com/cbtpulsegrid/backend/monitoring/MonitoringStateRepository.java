package com.cbtpulsegrid.backend.monitoring;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MonitoringStateRepository extends JpaRepository<MonitoringState, UUID> {

	Optional<MonitoringState> findByAttemptId(UUID attemptId);

	List<MonitoringState> findAllByInstitutionIdAndAttemptIdIn(
			UUID institutionId,
			Collection<UUID> attemptIds
	);
}
