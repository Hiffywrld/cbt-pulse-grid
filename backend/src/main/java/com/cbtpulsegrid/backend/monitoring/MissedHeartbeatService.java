package com.cbtpulsegrid.backend.monitoring;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.AttemptMonitoringQuery.AttemptView;
import com.cbtpulsegrid.backend.attempt.AttemptStatus;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringUpdateType;
import com.cbtpulsegrid.backend.monitoring.webhook.WebhookOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MissedHeartbeatService {

	private final MonitoringStateRepository stateRepository;
	private final MonitoringEventRepository eventRepository;
	private final MonitoringLiveUpdateNotifier liveUpdateNotifier;
	private final WebhookOutboxService webhookOutboxService;
	private final MissedHeartbeatProperties properties;
	private final Clock clock;

	MissedHeartbeatService(
			MonitoringStateRepository stateRepository,
			MonitoringEventRepository eventRepository,
			MonitoringLiveUpdateNotifier liveUpdateNotifier,
			WebhookOutboxService webhookOutboxService,
			MissedHeartbeatProperties properties,
			Clock clock
	) {
		this.stateRepository = stateRepository;
		this.eventRepository = eventRepository;
		this.liveUpdateNotifier = liveUpdateNotifier;
		this.webhookOutboxService = webhookOutboxService;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public int detectMissedHeartbeats() {
		Instant detectedAt = clock.instant();
		Instant cutoff = detectedAt.minus(properties.timeout());
		var timedOutStates = stateRepository.findTimedOutForUpdate(
				cutoff,
				properties.batchSize()
		);
		int processed = 0;
		for (MonitoringState state : timedOutStates) {
			if (!state.markHeartbeatMissed(detectedAt)) {
				continue;
			}
			processed++;
			int riskWeight = MonitoringRiskPolicy.weight(MonitoringEventType.HEARTBEAT_MISSED);
			int riskApplied = state.recordEvent(riskWeight);
			MonitoringEvent missedEvent = eventRepository.save(new MonitoringEvent(
					state.getInstitutionId(),
					state.getExamId(),
					state.getAttemptId(),
					state.getCandidateId(),
					UUID.randomUUID(),
					MonitoringEventType.HEARTBEAT_MISSED,
					detectedAt,
					detectedAt,
					Map.of("reason", "heartbeat_timeout"),
					riskWeight,
					riskApplied
			));
			webhookOutboxService.enqueue(missedEvent, state.getRiskScore());
			liveUpdateNotifier.publish(
					new AttemptView(
							state.getAttemptId(),
							state.getInstitutionId(),
							state.getExamId(),
							state.getCandidateId(),
							AttemptStatus.IN_PROGRESS
					),
					state,
					MonitoringUpdateType.HEARTBEAT_MISSED,
					detectedAt
			);
		}
		stateRepository.flush();
		return processed;
	}
}
