package com.cbtpulsegrid.backend.monitoring.api;

import java.time.Instant;
import java.util.UUID;

public record HeartbeatResponse(
		UUID attemptId,
		UUID heartbeatId,
		long clientSequence,
		Instant clientTimestamp,
		Instant receivedAt,
		boolean focused,
		boolean fullscreen,
		boolean online,
		boolean applied,
		long eventCount,
		int riskScore
) {
}
