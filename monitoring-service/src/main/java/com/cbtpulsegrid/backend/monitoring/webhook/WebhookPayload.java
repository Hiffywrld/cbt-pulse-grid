package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;

record WebhookPayload(
		String payloadVersion,
		UUID eventId,
		MonitoringEventType eventType,
		UUID institutionId,
		UUID examId,
		UUID attemptId,
		UUID candidateId,
		Instant occurredAt,
		Instant receivedAt,
		int riskPointsApplied,
		int totalRiskScore,
		Instant serverTimestamp
) {
}
