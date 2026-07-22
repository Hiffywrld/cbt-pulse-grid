package com.cbtpulsegrid.backend.questionbank.api;

import java.util.UUID;

public record StaffQuestionOptionResponse(
		UUID id,
		String optionText,
		boolean correct,
		int displayOrder
) {
}
