package com.cbtpulsegrid.backend.monitoring.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MonitoringEventBatchRequest(
		@NotNull UUID syncId,
		@NotEmpty @Size(max = 200) List<@Valid MonitoringEventRequest> events
) {
	public MonitoringEventBatchRequest {
		events = events == null ? null : List.copyOf(events);
	}
}
