package com.cbtpulsegrid.backend.monitoring.api;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record HeartbeatRequest(
		@NotNull UUID heartbeatId,
		@PositiveOrZero long clientSequence,
		@NotNull Instant clientTimestamp,
		@Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
		@NotBlank @Size(max = 512) String deviceId,
		boolean focused,
		boolean fullscreen,
		boolean online
) {
	@Override
	public String toString() {
		return "HeartbeatRequest[heartbeatId=" + heartbeatId
				+ ", clientSequence=" + clientSequence
				+ ", clientTimestamp=" + clientTimestamp
				+ ", deviceId=[REDACTED]"
				+ ", focused=" + focused
				+ ", fullscreen=" + fullscreen
				+ ", online=" + online + "]";
	}
}
