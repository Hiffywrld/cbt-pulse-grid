package com.cbtpulsegrid.backend.result.api;

import java.util.UUID;

public record StaffOptionReviewResponse(
		UUID optionId,
		String optionText,
		int displayOrder,
		boolean selected,
		boolean correct
) {
}
