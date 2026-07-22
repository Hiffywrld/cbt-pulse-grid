package com.cbtpulsegrid.backend.monitoring;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MonitoringSyncBatchRepository extends JpaRepository<MonitoringSyncBatch, UUID> {

	boolean existsByAttemptIdAndSyncId(UUID attemptId, UUID syncId);
}
