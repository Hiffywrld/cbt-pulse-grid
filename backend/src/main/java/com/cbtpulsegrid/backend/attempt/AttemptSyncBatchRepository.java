package com.cbtpulsegrid.backend.attempt;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AttemptSyncBatchRepository extends JpaRepository<AttemptSyncBatch, UUID> {

	boolean existsByAttemptIdAndSyncId(UUID attemptId, UUID syncId);
}
