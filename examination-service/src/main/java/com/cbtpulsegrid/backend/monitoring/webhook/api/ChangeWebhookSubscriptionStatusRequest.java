package com.cbtpulsegrid.backend.monitoring.webhook.api;

import com.cbtpulsegrid.backend.monitoring.webhook.WebhookSubscriptionStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeWebhookSubscriptionStatusRequest(
		@NotNull WebhookSubscriptionStatus status
) {
}
