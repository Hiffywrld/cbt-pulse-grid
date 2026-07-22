package com.cbtpulsegrid.backend.monitoring.webhook;

class WebhookTransportException extends RuntimeException {

	private final boolean timeout;

	WebhookTransportException(boolean timeout, Throwable cause) {
		super(timeout ? "Webhook request timed out" : "Webhook network delivery failed", cause);
		this.timeout = timeout;
	}

	boolean isTimeout() {
		return timeout;
	}
}
