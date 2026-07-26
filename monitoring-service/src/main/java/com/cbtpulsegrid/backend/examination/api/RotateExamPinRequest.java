package com.cbtpulsegrid.backend.examination.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RotateExamPinRequest(
		@NotBlank @Pattern(regexp = "\\d{6}", message = "accessPin must contain exactly six digits") String accessPin
) {
}
