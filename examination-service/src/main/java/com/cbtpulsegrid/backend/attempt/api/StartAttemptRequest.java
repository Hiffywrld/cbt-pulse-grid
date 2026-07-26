package com.cbtpulsegrid.backend.attempt.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StartAttemptRequest(
		@Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
		@NotBlank
		@Pattern(regexp = "\\d{6}", message = "accessPin must contain exactly six digits")
		String accessPin,
		@Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
		@NotBlank
		@Size(max = 512)
		String deviceId
) {
	@Override
	public String toString() {
		return "StartAttemptRequest[accessPin=[REDACTED], deviceId=[REDACTED]]";
	}
}
