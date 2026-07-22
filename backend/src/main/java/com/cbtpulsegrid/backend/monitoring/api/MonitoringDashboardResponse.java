package com.cbtpulsegrid.backend.monitoring.api;

import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.AttemptStatus;
import com.cbtpulsegrid.backend.identity.UserStatus;

public record MonitoringDashboardResponse(
		UUID attemptId,
		UUID candidateId,
		String firstName,
		String lastName,
		String registrationNumber,
		UserStatus candidateStatus,
		AttemptStatus attemptStatus,
		Instant lastHeartbeatAt,
		ConnectivityState connectivity,
		Boolean focused,
		Boolean fullscreen,
		long eventCount,
		int riskScore
) {
}
