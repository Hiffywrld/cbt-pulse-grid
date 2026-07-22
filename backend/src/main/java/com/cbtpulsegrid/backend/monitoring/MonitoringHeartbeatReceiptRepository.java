package com.cbtpulsegrid.backend.monitoring;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MonitoringHeartbeatReceiptRepository
		extends JpaRepository<MonitoringHeartbeatReceipt, UUID> {

	boolean existsByAttemptIdAndHeartbeatId(UUID attemptId, UUID heartbeatId);
}
