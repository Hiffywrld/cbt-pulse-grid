package com.cbtpulsegrid.backend.monitoring.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MonitoringEventRequest(
		@NotNull UUID eventId,
		@NotNull MonitoringEventType eventType,
		@NotNull Instant occurredAt,
		@Size(max = 10)
		Map<@NotBlank @Size(max = 40) String, @NotNull @Size(max = 200) String> metadata
) {
	public MonitoringEventRequest {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
