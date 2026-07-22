package com.cbtpulsegrid.backend.monitoring;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.attempt.AttemptMonitoringQuery;
import com.cbtpulsegrid.backend.attempt.AttemptMonitoringQuery.AttemptPage;
import com.cbtpulsegrid.backend.attempt.AttemptMonitoringQuery.AttemptView;
import com.cbtpulsegrid.backend.examination.MonitoringExamQuery;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery.CandidateProfile;
import com.cbtpulsegrid.backend.monitoring.api.ConnectivityState;
import com.cbtpulsegrid.backend.monitoring.api.HeartbeatRequest;
import com.cbtpulsegrid.backend.monitoring.api.HeartbeatResponse;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringActor;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringDashboardResponse;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringEventBatchRequest;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringEventBatchResponse;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringEventRequest;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringEventResponse;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringPageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoringService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Set<String> SENSITIVE_METADATA_KEYS = Set.of(
			"authorization",
			"password",
			"token",
			"jwt",
			"secret",
			"pin",
			"deviceid",
			"refreshtoken"
	);

	private final MonitoringStateRepository stateRepository;
	private final MonitoringHeartbeatReceiptRepository heartbeatReceiptRepository;
	private final MonitoringEventRepository eventRepository;
	private final MonitoringSyncBatchRepository syncBatchRepository;
	private final AttemptMonitoringQuery attemptQuery;
	private final MonitoringExamQuery examQuery;
	private final ExaminationCandidateQuery candidateQuery;
	private final MonitoringAuthorization authorization;
	private final Clock clock;

	public MonitoringService(
			MonitoringStateRepository stateRepository,
			MonitoringHeartbeatReceiptRepository heartbeatReceiptRepository,
			MonitoringEventRepository eventRepository,
			MonitoringSyncBatchRepository syncBatchRepository,
			AttemptMonitoringQuery attemptQuery,
			MonitoringExamQuery examQuery,
			ExaminationCandidateQuery candidateQuery,
			MonitoringAuthorization authorization,
			Clock clock
	) {
		this.stateRepository = stateRepository;
		this.heartbeatReceiptRepository = heartbeatReceiptRepository;
		this.eventRepository = eventRepository;
		this.syncBatchRepository = syncBatchRepository;
		this.attemptQuery = attemptQuery;
		this.examQuery = examQuery;
		this.candidateQuery = candidateQuery;
		this.authorization = authorization;
		this.clock = clock;
	}

	@Transactional
	public HeartbeatResponse recordHeartbeat(
			MonitoringActor actor,
			UUID attemptId,
			HeartbeatRequest request
	) {
		UUID institutionId = authorization.requireStudent(actor);
		AttemptView attempt = attemptQuery.requireOwnedActiveAttemptAndDevice(
				institutionId,
				actor.userId(),
				attemptId,
				request.deviceId()
		);
		MonitoringState state = stateRepository.findByAttemptId(attemptId).orElse(null);
		if (heartbeatReceiptRepository.existsByAttemptIdAndHeartbeatId(
				attemptId,
				request.heartbeatId()
		)) {
			if (state == null || state.getLastHeartbeatId() == null) {
				throw new IllegalStateException("Heartbeat receipt exists without monitoring state");
			}
			return toHeartbeatResponse(state, false);
		}
		if (state == null) {
			state = newState(attempt);
		}

		Instant receivedAt = clock.instant();
		boolean applied = state.applyHeartbeat(
				request.heartbeatId(),
				request.clientSequence(),
				request.clientTimestamp(),
				receivedAt,
				request.focused(),
				request.fullscreen(),
				request.online()
		);
		heartbeatReceiptRepository.save(new MonitoringHeartbeatReceipt(
				institutionId,
				attemptId,
				request.heartbeatId(),
				request.clientSequence(),
				receivedAt
		));
		stateRepository.saveAndFlush(state);
		return toHeartbeatResponse(state, applied);
	}

	@Transactional
	public MonitoringEventBatchResponse recordEvents(
			MonitoringActor actor,
			UUID attemptId,
			MonitoringEventBatchRequest request
	) {
		UUID institutionId = authorization.requireStudent(actor);
		AttemptView attempt = attemptQuery.requireOwnedActiveAttempt(
				institutionId,
				actor.userId(),
				attemptId
		);
		MonitoringState state = stateRepository.findByAttemptId(attemptId).orElse(null);
		Instant receivedAt = clock.instant();
		if (syncBatchRepository.existsByAttemptIdAndSyncId(attemptId, request.syncId())) {
			if (state == null) {
				throw new IllegalStateException("Monitoring sync receipt exists without monitoring state");
			}
			return new MonitoringEventBatchResponse(
					request.syncId(),
					0,
					request.events().size(),
					state.getEventCount(),
					state.getRiskScore(),
					receivedAt
			);
		}
		if (state == null) {
			state = newState(attempt);
		}

		Map<UUID, MonitoringEventRequest> uniqueRequests = new LinkedHashMap<>();
		for (MonitoringEventRequest event : request.events()) {
			uniqueRequests.putIfAbsent(event.eventId(), event);
		}
		Set<UUID> existingEventIds = Set.copyOf(eventRepository.findExistingClientEventIds(
				attemptId,
				uniqueRequests.keySet()
		));
		List<MonitoringEventRequest> acceptedRequests = uniqueRequests.values().stream()
				.filter(event -> !existingEventIds.contains(event.eventId()))
				.toList();
		List<MonitoringEvent> acceptedEvents = new java.util.ArrayList<>(acceptedRequests.size());
		for (MonitoringEventRequest event : acceptedRequests) {
			validateMetadata(event.metadata());
			int riskWeight = MonitoringRiskPolicy.weight(event.eventType());
			int riskApplied = state.recordEvent(riskWeight);
			applyConnectivityEvent(state, event);
			acceptedEvents.add(new MonitoringEvent(
					institutionId,
					attempt.examId(),
					attemptId,
					actor.userId(),
					event.eventId(),
					event.eventType(),
					event.occurredAt(),
					receivedAt,
					event.metadata(),
					riskWeight,
					riskApplied
			));
		}
		if (!acceptedEvents.isEmpty()) {
			eventRepository.saveAll(acceptedEvents);
		}
		syncBatchRepository.save(new MonitoringSyncBatch(
				institutionId,
				attemptId,
				request.syncId(),
				receivedAt
		));
		stateRepository.saveAndFlush(state);
		return new MonitoringEventBatchResponse(
				request.syncId(),
				acceptedEvents.size(),
				request.events().size() - acceptedEvents.size(),
				state.getEventCount(),
				state.getRiskScore(),
				receivedAt
		);
	}

	@Transactional(readOnly = true)
	public MonitoringPageResponse<MonitoringDashboardResponse> dashboard(
			MonitoringActor actor,
			UUID examId,
			int page,
			int size
	) {
		UUID institutionId = authorization.requireStaff(actor);
		validatePage(page, size);
		examQuery.requireExam(institutionId, examId);
		AttemptPage attempts = attemptQuery.findAttemptsByExam(institutionId, examId, page, size);
		Set<UUID> attemptIds = attempts.content().stream()
				.map(AttemptView::id)
				.collect(Collectors.toSet());
		Map<UUID, MonitoringState> states = attemptIds.isEmpty()
				? Map.of()
				: stateRepository.findAllByInstitutionIdAndAttemptIdIn(institutionId, attemptIds)
						.stream()
						.collect(Collectors.toMap(MonitoringState::getAttemptId, Function.identity()));
		Set<UUID> candidateIds = attempts.content().stream()
				.map(AttemptView::candidateId)
				.collect(Collectors.toSet());
		Map<UUID, CandidateProfile> candidates = candidateQuery.findByIds(candidateIds);
		List<MonitoringDashboardResponse> content = attempts.content().stream()
				.map(attempt -> toDashboardResponse(
						institutionId,
						attempt,
						states.get(attempt.id()),
						requireCandidate(candidates, attempt.candidateId())
				))
				.toList();
		return new MonitoringPageResponse<>(
				content,
				attempts.page(),
				attempts.size(),
				attempts.totalElements(),
				attempts.totalPages(),
				attempts.first(),
				attempts.last()
		);
	}

	@Transactional(readOnly = true)
	public MonitoringPageResponse<MonitoringEventResponse> events(
			MonitoringActor actor,
			UUID attemptId,
			int page,
			int size
	) {
		UUID institutionId = authorization.requireStaff(actor);
		validatePage(page, size);
		attemptQuery.requireAttempt(institutionId, attemptId);
		Page<MonitoringEventResponse> events = eventRepository.findByInstitutionIdAndAttemptId(
				institutionId,
				attemptId,
				PageRequest.of(
						page,
						size,
						Sort.by(
								Sort.Order.desc("occurredAt"),
								Sort.Order.desc("receivedAt"),
								Sort.Order.desc("id")
						)
				)
		).map(MonitoringService::toEventResponse);
		return MonitoringPageResponse.from(events);
	}

	private static MonitoringState newState(AttemptView attempt) {
		return new MonitoringState(
				attempt.institutionId(),
				attempt.examId(),
				attempt.id(),
				attempt.candidateId()
		);
	}

	private static HeartbeatResponse toHeartbeatResponse(MonitoringState state, boolean applied) {
		return new HeartbeatResponse(
				state.getAttemptId(),
				state.getLastHeartbeatId(),
				state.getLastClientSequence(),
				state.getLastClientTimestamp(),
				state.getLastHeartbeatReceivedAt(),
				Boolean.TRUE.equals(state.getFocused()),
				Boolean.TRUE.equals(state.getFullscreen()),
				Boolean.TRUE.equals(state.getOnline()),
				applied,
				state.getEventCount(),
				state.getRiskScore()
		);
	}

	private static MonitoringDashboardResponse toDashboardResponse(
			UUID institutionId,
			AttemptView attempt,
			MonitoringState state,
			CandidateProfile candidate
	) {
		if (!institutionId.equals(candidate.institutionId())) {
			throw new AccessDeniedException("Cross-institution candidate access is denied");
		}
		return new MonitoringDashboardResponse(
				attempt.id(),
				attempt.candidateId(),
				candidate.firstName(),
				candidate.lastName(),
				candidate.registrationNumber(),
				candidate.status(),
				attempt.status(),
				state == null ? null : state.getLastHeartbeatReceivedAt(),
				connectivity(state),
				state == null ? null : state.getFocused(),
				state == null ? null : state.getFullscreen(),
				state == null ? 0 : state.getEventCount(),
				state == null ? 0 : state.getRiskScore()
		);
	}

	private static CandidateProfile requireCandidate(
			Map<UUID, CandidateProfile> candidates,
			UUID candidateId
	) {
		CandidateProfile candidate = candidates.get(candidateId);
		if (candidate == null) {
			throw new NoSuchElementException("Candidate user not found");
		}
		return candidate;
	}

	private static ConnectivityState connectivity(MonitoringState state) {
		if (state == null || state.getOnline() == null) {
			return ConnectivityState.UNKNOWN;
		}
		return state.getOnline() ? ConnectivityState.ONLINE : ConnectivityState.OFFLINE;
	}

	private static void applyConnectivityEvent(
			MonitoringState state,
			MonitoringEventRequest event
	) {
		if (event.eventType() == MonitoringEventType.NETWORK_DISCONNECTED) {
			state.applyConnectivity(false, event.occurredAt());
		}
		else if (event.eventType() == MonitoringEventType.NETWORK_RECONNECTED) {
			state.applyConnectivity(true, event.occurredAt());
		}
	}

	private static void validateMetadata(Map<String, String> metadata) {
		for (String key : metadata.keySet()) {
			String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
			if (SENSITIVE_METADATA_KEYS.stream().anyMatch(normalized::contains)) {
				throw new IllegalArgumentException("Monitoring metadata contains a prohibited key");
			}
		}
	}

	private static MonitoringEventResponse toEventResponse(MonitoringEvent event) {
		return new MonitoringEventResponse(
				event.getId(),
				event.getClientEventId(),
				event.getEventType(),
				event.getOccurredAt(),
				event.getReceivedAt(),
				event.getMetadata(),
				event.getRiskWeight(),
				event.getRiskPointsApplied()
		);
	}

	private static void validatePage(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must not be negative");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Page size must be between 1 and 100");
		}
	}
}
