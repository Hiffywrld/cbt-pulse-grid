package com.cbtpulsegrid.backend.examination.api;

import java.util.List;

import org.springframework.data.domain.Page;

public record ExamPageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
) {

	public static <T> ExamPageResponse<T> from(Page<T> result) {
		return new ExamPageResponse<>(
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
