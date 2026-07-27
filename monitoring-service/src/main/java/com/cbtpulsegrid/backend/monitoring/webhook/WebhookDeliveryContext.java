package com.cbtpulsegrid.backend.monitoring.webhook;

import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;

record WebhookDeliveryContext(
		UUID deliveryId,
		UUID subscriptionId,
		UUID eventId,
		MonitoringEventType eventType,
		String destinationUrl,
		int secretVersion,
		byte[] payloadBody,
		int attemptNumber
) {
	WebhookDeliveryContext {
		payloadBody = payloadBody.clone();
	}

	@Override
	public byte[] payloadBody() {
		return payloadBody.clone();
	}
}
