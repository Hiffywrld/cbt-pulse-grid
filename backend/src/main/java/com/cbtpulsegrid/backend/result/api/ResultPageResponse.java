package com.cbtpulsegrid.backend.result.api;

import java.util.List;

public record ResultPageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {
}
