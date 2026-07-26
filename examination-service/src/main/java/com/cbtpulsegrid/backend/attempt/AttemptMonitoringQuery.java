package com.cbtpulsegrid.backend.attempt;

import java.util.List;
import java.util.UUID;

/**
 * Narrow attempt-module contract used by persistent exam monitoring.
 */
public interface AttemptMonitoringQuery {

	AttemptView requireOwnedActiveAttempt(
			UUID institutionId,
			UUID candidateId,
			UUID attemptId
	);

	AttemptView requireOwnedActiveAttemptAndDevice(
			UUID institutionId,
			UUID candidateId,
			UUID attemptId,
			String deviceId
	);

	AttemptView requireAttempt(UUID institutionId, UUID attemptId);

	AttemptPage findAttemptsByExam(UUID institutionId, UUID examId, int page, int size);

	record AttemptView(
			UUID id,
			UUID institutionId,
			UUID examId,
			UUID candidateId,
			AttemptStatus status
	) {
	}

	record AttemptPage(
			List<AttemptView> content,
			int page,
			int size,
			long totalElements,
			int totalPages,
			boolean first,
			boolean last
	) {
		public AttemptPage {
			content = List.copyOf(content);
		}
	}
}
