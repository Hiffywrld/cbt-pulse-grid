package com.cbtpulsegrid.backend.monitoring;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.monitoring.websocket")
record MonitoringWebSocketProperties(@NotEmpty List<@NotBlank String> allowedOrigins) {

	MonitoringWebSocketProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
	}
}
