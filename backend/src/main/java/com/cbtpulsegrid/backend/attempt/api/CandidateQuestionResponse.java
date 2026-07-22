package com.cbtpulsegrid.backend.attempt.api;

import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionType;

public record CandidateQuestionResponse(
		UUID id,
		int position,
		String questionText,
		QuestionType questionType,
		List<CandidateOptionResponse> options
) {
	public CandidateQuestionResponse {
		options = List.copyOf(options);
	}
}
