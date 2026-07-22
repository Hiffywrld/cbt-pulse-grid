package com.cbtpulsegrid.backend.monitoring;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.monitoring.missed-heartbeat")
record MissedHeartbeatProperties(
		@NotNull Duration scanInterval,
		@NotNull Duration timeout,
		@Min(1) @Max(1000) int batchSize
) {
	MissedHeartbeatProperties {
		if (scanInterval != null && (scanInterval.isZero() || scanInterval.isNegative())) {
			throw new IllegalArgumentException("Monitoring scan interval must be positive");
		}
		if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
			throw new IllegalArgumentException("Monitoring heartbeat timeout must be positive");
		}
	}
}
