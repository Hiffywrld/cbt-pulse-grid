package com.cbtpulsegrid.backend.monitoring.webhook.api;

import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import com.cbtpulsegrid.backend.monitoring.webhook.WebhookDeliveryStatus;

public record WebhookDeliveryResponse(
		UUID id,
		UUID subscriptionId,
		UUID eventId,
		MonitoringEventType eventType,
		WebhookDeliveryStatus status,
		int attemptCount,
		Instant nextAttemptAt,
		Integer responseStatus,
		String failureReason,
		Instant deliveredAt,
		Instant createdAt,
		Instant updatedAt,
		long version
) {
}
