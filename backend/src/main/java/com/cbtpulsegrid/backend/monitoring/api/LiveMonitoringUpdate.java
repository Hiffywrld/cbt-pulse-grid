package com.cbtpulsegrid.backend.monitoring.api;

import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.AttemptStatus;
import com.cbtpulsegrid.backend.identity.UserStatus;

public record LiveMonitoringUpdate(
		UUID attemptId,
		UUID candidateId,
		String candidateFirstName,
		String candidateLastName,
		String registrationNumber,
		UserStatus candidateStatus,
		AttemptStatus attemptStatus,
		Instant lastHeartbeat,
		ConnectivityState connectivity,
		Boolean focused,
		Boolean fullscreen,
		long eventCount,
		int riskScore,
		MonitoringUpdateType updateType,
		Instant serverTimestamp
) {
}
