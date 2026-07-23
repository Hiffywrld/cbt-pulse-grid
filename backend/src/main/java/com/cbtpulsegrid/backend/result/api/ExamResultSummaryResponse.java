package com.cbtpulsegrid.backend.result.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamResultSummaryResponse(
		UUID examId,
		String examCode,
		String examTitle,
		long assignedCandidates,
		long notStarted,
		long absent,
		long inProgress,
		long submitted,
		long autoSubmitted,
		long passed,
		long failed,
		BigDecimal averagePercentage,
		BigDecimal minimumPercentage,
		BigDecimal maximumPercentage,
		BigDecimal passRate,
		BigDecimal totalObtainableMarks
) {
	public ExamResultSummaryResponse(
			UUID examId, String examCode, String examTitle, long assignedCandidates,
			long notStarted, long inProgress, long submitted, long autoSubmitted,
			long passed, long failed, BigDecimal averagePercentage, BigDecimal minimumPercentage,
			BigDecimal maximumPercentage, BigDecimal passRate, BigDecimal totalObtainableMarks
	) {
		this(examId, examCode, examTitle, assignedCandidates, notStarted, 0, inProgress,
				submitted, autoSubmitted, passed, failed, averagePercentage, minimumPercentage,
				maximumPercentage, passRate, totalObtainableMarks);
	}
}
