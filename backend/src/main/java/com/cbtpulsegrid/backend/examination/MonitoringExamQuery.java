package com.cbtpulsegrid.backend.examination;

import java.util.UUID;

/**
 * Narrow examination-module tenancy contract used by monitoring dashboards.
 */
public interface MonitoringExamQuery {

	void requireExam(UUID institutionId, UUID examId);
}
