package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
class WebhookRetryPolicy {

	private final WebhookProperties properties;

	WebhookRetryPolicy(WebhookProperties properties) {
		this.properties = properties;
	}

	Duration delayAfterAttempt(int attemptNumber) {
		long initialMillis = Math.max(1, properties.initialBackoff().toMillis());
		long maximumMillis = Math.max(initialMillis, properties.maximumBackoff().toMillis());
		int exponent = Math.max(0, Math.min(attemptNumber - 1, 62));
		long delay;
		try {
			delay = Math.multiplyExact(initialMillis, 1L << exponent);
		}
		catch (ArithmeticException exception) {
			delay = maximumMillis;
		}
		return Duration.ofMillis(Math.min(delay, maximumMillis));
	}
}
