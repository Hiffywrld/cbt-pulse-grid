package com.cbtpulsegrid.backend.attempt;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.attempts.expiry")
record ExpiredAttemptProperties(
		@NotNull Duration scanInterval,
		@Min(1) @Max(500) int batchSize
) {
	ExpiredAttemptProperties {
		if (scanInterval == null || scanInterval.isZero() || scanInterval.isNegative()) {
			throw new IllegalArgumentException("Attempt expiry scan interval must be positive");
		}
	}
}
