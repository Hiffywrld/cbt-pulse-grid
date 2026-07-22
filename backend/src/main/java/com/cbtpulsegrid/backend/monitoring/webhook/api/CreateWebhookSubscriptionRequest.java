package com.cbtpulsegrid.backend.monitoring.webhook.api;

import java.util.Set;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWebhookSubscriptionRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(max = 2048) String destinationUrl,
		@Size(max = 9) Set<MonitoringEventType> eventTypes
) {
}
