package com.cbtpulsegrid.backend.questionbank.api;

import java.util.List;

import org.springframework.data.domain.Page;

public record QuestionBankPageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
) {

	public static <T> QuestionBankPageResponse<T> from(Page<T> result) {
		return new QuestionBankPageResponse<>(
				List.copyOf(result.getContent()),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.isFirst(),
				result.isLast()
		);
	}
}
