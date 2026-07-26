package com.cbtpulsegrid.backend.monitoring.webhook.api;

public record WebhookSubscriptionSecretResponse(
		WebhookSubscriptionResponse subscription,
		String secret
) {
	@Override
	public String toString() {
		return "WebhookSubscriptionSecretResponse[subscription=" + subscription + ", secret=[REDACTED]]";
	}
}
