package com.cbtpulsegrid.backend.monitoring.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;

public record MonitoringEventResponse(
		UUID id,
		UUID clientEventId,
		MonitoringEventType eventType,
		Instant occurredAt,
		Instant receivedAt,
		Map<String, String> metadata,
		int riskWeight,
		int riskPointsApplied
) {
	public MonitoringEventResponse {
		metadata = Map.copyOf(metadata);
	}
}
