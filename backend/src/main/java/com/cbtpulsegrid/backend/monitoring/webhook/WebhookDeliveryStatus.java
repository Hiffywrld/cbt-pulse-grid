package com.cbtpulsegrid.backend.monitoring.webhook;

public enum WebhookDeliveryStatus {
	PENDING,
	IN_FLIGHT,
	SUCCEEDED,
	FAILED,
	DEAD_LETTER
}
