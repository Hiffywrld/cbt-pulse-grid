package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Duration;
import java.util.Base64;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.webhooks")
public record WebhookProperties(
		boolean enabled,
		String masterKey,
		boolean allowPrivateHttp,
		@NotNull Duration connectTimeout,
		@NotNull Duration responseTimeout,
		@NotNull Duration scanInterval,
		@NotNull Duration leaseDuration,
		@NotNull Duration initialBackoff,
		@NotNull Duration maximumBackoff,
		@Min(1) @Max(100) int batchSize,
		@Min(1) @Max(20) int maximumAttempts
) {
	public WebhookProperties {
		validatePositive(connectTimeout, "Webhook connection timeout");
		validatePositive(responseTimeout, "Webhook response timeout");
		validatePositive(scanInterval, "Webhook scan interval");
		validatePositive(leaseDuration, "Webhook lease duration");
		validatePositive(initialBackoff, "Webhook initial backoff");
		validatePositive(maximumBackoff, "Webhook maximum backoff");
		if (initialBackoff != null && maximumBackoff != null
				&& initialBackoff.compareTo(maximumBackoff) > 0) {
			throw new IllegalArgumentException(
					"Webhook initial backoff must not exceed maximum backoff"
			);
		}
		if (enabled) {
			byte[] decoded;
			try {
				decoded = Base64.getDecoder().decode(masterKey == null ? "" : masterKey);
			}
			catch (IllegalArgumentException exception) {
				throw new IllegalArgumentException(
						"WEBHOOK_MASTER_KEY must be valid Base64",
						exception
				);
			}
			if (decoded.length != 32) {
				throw new IllegalArgumentException(
						"WEBHOOK_MASTER_KEY must decode to exactly 32 bytes"
				);
			}
		}
	}

	byte[] decodedMasterKey() {
		if (!enabled) {
			throw new IllegalStateException("Webhook delivery is disabled");
		}
		return Base64.getDecoder().decode(masterKey);
	}

	private static void validatePositive(Duration value, String name) {
		if (value != null && (value.isZero() || value.isNegative())) {
			throw new IllegalArgumentException(name + " must be positive");
		}
	}
}
