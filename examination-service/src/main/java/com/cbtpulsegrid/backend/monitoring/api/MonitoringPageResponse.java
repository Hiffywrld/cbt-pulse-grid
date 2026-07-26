package com.cbtpulsegrid.backend.monitoring.api;

import java.util.List;

import org.springframework.data.domain.Page;

public record MonitoringPageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
) {
	public MonitoringPageResponse {
		content = List.copyOf(content);
	}

	public static <T> MonitoringPageResponse<T> from(Page<T> result) {
		return new MonitoringPageResponse<>(
				result.getContent(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.isFirst(),
				result.isLast()
		);
	}
}
