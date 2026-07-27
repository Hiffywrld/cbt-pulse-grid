package com.cbtpulsegrid.backend.monitoring.webhook.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import com.cbtpulsegrid.backend.monitoring.webhook.WebhookSubscriptionStatus;

public record WebhookSubscriptionResponse(
		UUID id,
		UUID institutionId,
		String name,
		String destinationUrl,
		WebhookSubscriptionStatus status,
		boolean allEventTypes,
		Set<MonitoringEventType> eventTypes,
		int secretVersion,
		UUID createdBy,
		UUID updatedBy,
		Instant createdAt,
		Instant updatedAt,
		long version
) {
	public WebhookSubscriptionResponse {
		eventTypes = Set.copyOf(eventTypes);
	}
}
