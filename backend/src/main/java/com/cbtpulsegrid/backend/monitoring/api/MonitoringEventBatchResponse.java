package com.cbtpulsegrid.backend.monitoring.api;

import java.time.Instant;
import java.util.UUID;

public record MonitoringEventBatchResponse(
		UUID acknowledgedSyncId,
		int acceptedEvents,
		int duplicateEvents,
		long eventCount,
		int riskScore,
		Instant receivedAt
) {
}
