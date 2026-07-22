package com.cbtpulsegrid.backend.identity.account;

import java.util.List;

import org.springframework.data.domain.Page;

public record UserPageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
) {

	public static <T> UserPageResponse<T> from(Page<T> result) {
		return new UserPageResponse<>(
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
