package com.cbtpulsegrid.backend.monitoring;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.AttemptMonitoringQuery.AttemptView;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery.CandidateProfile;
import com.cbtpulsegrid.backend.monitoring.api.ConnectivityState;
import com.cbtpulsegrid.backend.monitoring.api.LiveMonitoringUpdate;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringUpdateType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
class MonitoringLiveUpdateNotifier {

	private final ExaminationCandidateQuery candidateQuery;
	private final ApplicationEventPublisher eventPublisher;

	MonitoringLiveUpdateNotifier(
			ExaminationCandidateQuery candidateQuery,
			ApplicationEventPublisher eventPublisher
	) {
		this.candidateQuery = candidateQuery;
		this.eventPublisher = eventPublisher;
	}

	void publish(
			AttemptView attempt,
			MonitoringState state,
			MonitoringUpdateType updateType,
			Instant serverTimestamp
	) {
		Map<UUID, CandidateProfile> candidates = candidateQuery.findByIds(Set.of(attempt.candidateId()));
		CandidateProfile candidate = candidates.get(attempt.candidateId());
		if (candidate == null) {
			throw new IllegalStateException("Monitoring candidate was not found");
		}
		if (!attempt.institutionId().equals(candidate.institutionId())) {
			throw new AccessDeniedException("Cross-institution monitoring update is denied");
		}

		eventPublisher.publishEvent(new MonitoringUpdateEvent(
				attempt.examId(),
				new LiveMonitoringUpdate(
						attempt.id(),
						attempt.candidateId(),
						candidate.firstName(),
						candidate.lastName(),
						candidate.registrationNumber(),
						candidate.status(),
						attempt.status(),
						state.getLastHeartbeatReceivedAt(),
						connectivity(state),
						state.getFocused(),
						state.getFullscreen(),
						state.getEventCount(),
						state.getRiskScore(),
						updateType,
						serverTimestamp
				)
		));
	}

	private static ConnectivityState connectivity(MonitoringState state) {
		if (state.getOnline() == null) {
			return ConnectivityState.UNKNOWN;
		}
		return state.getOnline() ? ConnectivityState.ONLINE : ConnectivityState.OFFLINE;
	}
}
