package com.cbtpulsegrid.backend.monitoring;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, UUID> {

	@Query("""
			select event.clientEventId
			from MonitoringEvent event
			where event.attemptId = :attemptId
			and event.clientEventId in :clientEventIds
			""")
	List<UUID> findExistingClientEventIds(
			@Param("attemptId") UUID attemptId,
			@Param("clientEventIds") Collection<UUID> clientEventIds
	);

	Page<MonitoringEvent> findByInstitutionIdAndAttemptId(
			UUID institutionId,
			UUID attemptId,
			Pageable pageable
	);
}
