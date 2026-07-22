package com.cbtpulsegrid.backend.monitoring;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MonitoringStateRepository extends JpaRepository<MonitoringState, UUID> {

	Optional<MonitoringState> findByAttemptId(UUID attemptId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select state from MonitoringState state where state.attemptId = :attemptId")
	Optional<MonitoringState> findByAttemptIdForUpdate(@Param("attemptId") UUID attemptId);

	List<MonitoringState> findAllByInstitutionIdAndAttemptIdIn(
			UUID institutionId,
			Collection<UUID> attemptIds
	);

	@Query(value = """
			select state.*
			from monitoring_states state
			join exam_attempts attempt on attempt.id = state.attempt_id
			where attempt.status = 'IN_PROGRESS'
			and state.last_heartbeat_received_at is not null
			and state.last_heartbeat_received_at < :cutoff
			and state.heartbeat_outage_active = false
			order by state.last_heartbeat_received_at, state.id
			limit :batchSize
			for update of state skip locked
			""", nativeQuery = true)
	List<MonitoringState> findTimedOutForUpdate(
			@Param("cutoff") java.time.Instant cutoff,
			@Param("batchSize") int batchSize
	);
}
