package com.cbtpulsegrid.backend.monitoring;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.attempt.AttemptMonitoringQuery;
import com.cbtpulsegrid.backend.attempt.AttemptMonitoringQuery.AttemptPage;
import com.cbtpulsegrid.backend.attempt.AttemptMonitoringQuery.AttemptView;
import com.cbtpulsegrid.backend.attempt.AttemptStatus;
import com.cbtpulsegrid.backend.examination.MonitoringExamQuery;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery.CandidateProfile;
import com.cbtpulsegrid.backend.identity.UserStatus;
import com.cbtpulsegrid.backend.monitoring.api.ConnectivityState;
import com.cbtpulsegrid.backend.monitoring.api.HeartbeatRequest;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringActor;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringEventBatchRequest;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringEventRequest;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringUpdateType;
import com.cbtpulsegrid.backend.monitoring.webhook.WebhookOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTests {

	private static final UUID INSTITUTION_ID = UUID.randomUUID();
	private static final UUID EXAM_ID = UUID.randomUUID();
	private static final UUID ATTEMPT_ID = UUID.randomUUID();
	private static final UUID CANDIDATE_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
	private static final MonitoringActor STUDENT = new MonitoringActor(
			CANDIDATE_ID,
			INSTITUTION_ID,
			Set.of("STUDENT")
	);
	private static final MonitoringActor STAFF = new MonitoringActor(
			UUID.randomUUID(),
			INSTITUTION_ID,
			Set.of("INVIGILATOR")
	);

	@Mock
	private MonitoringStateRepository stateRepository;
	@Mock
	private MonitoringHeartbeatReceiptRepository heartbeatReceiptRepository;
	@Mock
	private MonitoringEventRepository eventRepository;
	@Mock
	private MonitoringSyncBatchRepository syncBatchRepository;
	@Mock
	private AttemptMonitoringQuery attemptQuery;
	@Mock
	private MonitoringExamQuery examQuery;
	@Mock
	private ExaminationCandidateQuery candidateQuery;
	@Mock
	private MonitoringLiveUpdateNotifier liveUpdateNotifier;
	@Mock
	private WebhookOutboxService webhookOutboxService;

	private MonitoringService monitoringService;

	@BeforeEach
	void createService() {
		monitoringService = new MonitoringService(
				stateRepository,
				heartbeatReceiptRepository,
				eventRepository,
				syncBatchRepository,
				attemptQuery,
				examQuery,
				candidateQuery,
				new MonitoringAuthorization(),
				liveUpdateNotifier,
				webhookOutboxService,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void heartbeatUsesAttemptOwnershipAndDeviceValidationWithoutExposingTheDevice() {
		when(attemptQuery.requireOwnedActiveAttemptAndDevice(
				INSTITUTION_ID,
				CANDIDATE_ID,
				ATTEMPT_ID,
				"device-a"
		)).thenReturn(attempt());
		when(stateRepository.findByAttemptIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.empty());
		HeartbeatRequest request = heartbeat(1, "device-a", true, true, true);

		var response = monitoringService.recordHeartbeat(STUDENT, ATTEMPT_ID, request);

		assertTrue(response.applied());
		assertEquals(1, response.clientSequence());
		assertTrue(response.online());
		assertFalse(request.toString().contains("device-a"));
		verify(heartbeatReceiptRepository).save(any(MonitoringHeartbeatReceipt.class));
		verify(stateRepository).saveAndFlush(any(MonitoringState.class));
		verify(liveUpdateNotifier).publish(
				eq(attempt()),
				any(MonitoringState.class),
				eq(MonitoringUpdateType.HEARTBEAT),
				eq(NOW)
		);
	}

	@Test
	void staleAndDuplicateHeartbeatsCannotOverwriteNewerState() {
		MonitoringState state = state();
		UUID newestHeartbeatId = UUID.randomUUID();
		state.applyHeartbeat(newestHeartbeatId, 10, NOW.minusSeconds(1), NOW, true, true, true);
		when(attemptQuery.requireOwnedActiveAttemptAndDevice(
				INSTITUTION_ID,
				CANDIDATE_ID,
				ATTEMPT_ID,
				"device"
		)).thenReturn(attempt());
		when(stateRepository.findByAttemptIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.of(state));

		var stale = monitoringService.recordHeartbeat(
				STUDENT,
				ATTEMPT_ID,
				heartbeat(9, "device", false, false, false)
		);

		assertFalse(stale.applied());
		assertEquals(newestHeartbeatId, stale.heartbeatId());
		assertEquals(10, stale.clientSequence());
		assertTrue(stale.focused());
		assertTrue(stale.fullscreen());
		assertTrue(stale.online());

		when(heartbeatReceiptRepository.existsByAttemptIdAndHeartbeatId(
				ATTEMPT_ID,
				newestHeartbeatId
		)).thenReturn(true);
		HeartbeatRequest duplicate = new HeartbeatRequest(
				newestHeartbeatId,
				11,
				NOW,
				"device",
				false,
				false,
				false
		);
		var duplicateResponse = monitoringService.recordHeartbeat(STUDENT, ATTEMPT_ID, duplicate);

		assertFalse(duplicateResponse.applied());
		assertEquals(10, duplicateResponse.clientSequence());
		verify(liveUpdateNotifier, never()).publish(any(), any(), any(), any());
	}

	@Test
	void validHeartbeatRestoresAHeartbeatOutageAndPublishesOneRecovery() {
		MonitoringState state = state();
		state.applyHeartbeat(
				UUID.randomUUID(),
				1,
				NOW.minusSeconds(61),
				NOW.minusSeconds(60),
				true,
				true,
				true
		);
		state.markHeartbeatMissed(NOW.minusSeconds(30));
		when(attemptQuery.requireOwnedActiveAttemptAndDevice(
				INSTITUTION_ID,
				CANDIDATE_ID,
				ATTEMPT_ID,
				"device"
		)).thenReturn(attempt());
		when(stateRepository.findByAttemptIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.of(state));

		var response = monitoringService.recordHeartbeat(
				STUDENT,
				ATTEMPT_ID,
				heartbeat(2, "device", true, true, true)
		);

		assertTrue(response.applied());
		assertTrue(response.online());
		assertFalse(state.isHeartbeatOutageActive());
		verify(liveUpdateNotifier).publish(
				eq(attempt()),
				eq(state),
				eq(MonitoringUpdateType.HEARTBEAT_RESTORED),
				eq(NOW)
		);
	}

	@Test
	void duplicateSyncIdDoesNotInsertEventsOrIncreaseRisk() {
		MonitoringState state = state();
		state.recordEvent(10);
		UUID syncId = UUID.randomUUID();
		when(attemptQuery.requireOwnedActiveAttempt(INSTITUTION_ID, CANDIDATE_ID, ATTEMPT_ID))
				.thenReturn(attempt());
		when(stateRepository.findByAttemptIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.of(state));
		when(syncBatchRepository.existsByAttemptIdAndSyncId(ATTEMPT_ID, syncId)).thenReturn(true);
		MonitoringEventBatchRequest request = new MonitoringEventBatchRequest(
				syncId,
				List.of(event(UUID.randomUUID(), MonitoringEventType.DEVTOOLS_SUSPECTED, NOW))
		);

		var response = monitoringService.recordEvents(STUDENT, ATTEMPT_ID, request);

		assertEquals(0, response.acceptedEvents());
		assertEquals(1, response.duplicateEvents());
		assertEquals(10, response.riskScore());
		verify(eventRepository, never()).saveAll(anyList());
		verify(syncBatchRepository, never()).save(any(MonitoringSyncBatch.class));
		verify(liveUpdateNotifier, never()).publish(any(), any(), any(), any());
	}

	@Test
	void duplicateEventIdsAreStoredAndScoredOnlyOnce() {
		UUID existingId = UUID.randomUUID();
		UUID newId = UUID.randomUUID();
		when(attemptQuery.requireOwnedActiveAttempt(INSTITUTION_ID, CANDIDATE_ID, ATTEMPT_ID))
				.thenReturn(attempt());
		when(stateRepository.findByAttemptIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.empty());
		when(eventRepository.findExistingClientEventIds(ATTEMPT_ID, Set.of(existingId, newId)))
				.thenReturn(List.of(existingId));
		List<MonitoringEvent> saved = new ArrayList<>();
		when(eventRepository.saveAll(anyList())).thenAnswer(invocation -> {
			saved.addAll(invocation.getArgument(0));
			return invocation.getArgument(0);
		});
		MonitoringEventBatchRequest request = new MonitoringEventBatchRequest(
				UUID.randomUUID(),
				List.of(
						event(existingId, MonitoringEventType.TAB_HIDDEN, NOW.minusSeconds(2)),
						event(existingId, MonitoringEventType.TAB_HIDDEN, NOW.minusSeconds(2)),
						event(newId, MonitoringEventType.COPY_ATTEMPT, NOW.minusSeconds(1))
				)
		);

		var response = monitoringService.recordEvents(STUDENT, ATTEMPT_ID, request);

		assertEquals(1, response.acceptedEvents());
		assertEquals(2, response.duplicateEvents());
		assertEquals(20, response.riskScore());
		assertEquals(1, saved.size());
		assertEquals(newId, saved.getFirst().getClientEventId());
		verify(liveUpdateNotifier).publish(
				eq(attempt()),
				any(MonitoringState.class),
				eq(MonitoringUpdateType.MONITORING_EVENTS),
				eq(NOW)
		);
	}

	@Test
	void assignsServerOwnedWeightsAndZeroRiskToConnectivityEvents() {
		when(attemptQuery.requireOwnedActiveAttempt(INSTITUTION_ID, CANDIDATE_ID, ATTEMPT_ID))
				.thenReturn(attempt());
		when(stateRepository.findByAttemptIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.empty());
		when(eventRepository.findExistingClientEventIds(any(), any())).thenReturn(List.of());
		List<MonitoringEvent> saved = new ArrayList<>();
		when(eventRepository.saveAll(anyList())).thenAnswer(invocation -> {
			saved.addAll(invocation.getArgument(0));
			return invocation.getArgument(0);
		});
		List<MonitoringEventRequest> requests = java.util.Arrays.stream(MonitoringEventType.values())
				.filter(type -> type != MonitoringEventType.HEARTBEAT_MISSED)
				.map(type -> event(UUID.randomUUID(), type, NOW.plusSeconds(type.ordinal())))
				.toList();

		var response = monitoringService.recordEvents(
				STUDENT,
				ATTEMPT_ID,
				new MonitoringEventBatchRequest(UUID.randomUUID(), requests)
		);

		Map<MonitoringEventType, Integer> persistedWeights = saved.stream().collect(Collectors.toMap(
				MonitoringEvent::getEventType,
				MonitoringEvent::getRiskWeight,
				(first, second) -> first,
				() -> new EnumMap<>(MonitoringEventType.class)
		));
		Map<MonitoringEventType, Integer> clientWeights = new EnumMap<>(MonitoringRiskPolicy.weights());
		clientWeights.remove(MonitoringEventType.HEARTBEAT_MISSED);
		assertEquals(clientWeights, persistedWeights);
		assertEquals(95, response.riskScore());
		assertEquals(0, persistedWeights.get(MonitoringEventType.NETWORK_DISCONNECTED));
		assertEquals(0, persistedWeights.get(MonitoringEventType.NETWORK_RECONNECTED));
		ArgumentCaptor<MonitoringState> state = ArgumentCaptor.forClass(MonitoringState.class);
		verify(stateRepository).saveAndFlush(state.capture());
		assertEquals(Boolean.TRUE, state.getValue().getOnline());
		assertFalse(java.util.Arrays.stream(MonitoringEventRequest.class.getRecordComponents())
				.anyMatch(component -> component.getName().toLowerCase().contains("risk")));
	}

	@Test
	void rejectsClientSubmittedHeartbeatMissedEvents() {
		when(attemptQuery.requireOwnedActiveAttempt(INSTITUTION_ID, CANDIDATE_ID, ATTEMPT_ID))
				.thenReturn(attempt());
		when(stateRepository.findByAttemptIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.empty());
		when(eventRepository.findExistingClientEventIds(any(), any())).thenReturn(List.of());

		assertThrows(
				IllegalArgumentException.class,
				() -> monitoringService.recordEvents(
						STUDENT,
						ATTEMPT_ID,
						new MonitoringEventBatchRequest(
								UUID.randomUUID(),
								List.of(event(
										UUID.randomUUID(),
										MonitoringEventType.HEARTBEAT_MISSED,
										NOW
								))
						)
				)
		);
	}

	@Test
	void capsTotalRiskAtOneHundred() {
		MonitoringState state = state();
		state.recordEvent(90);
		when(attemptQuery.requireOwnedActiveAttempt(INSTITUTION_ID, CANDIDATE_ID, ATTEMPT_ID))
				.thenReturn(attempt());
		when(stateRepository.findByAttemptIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.of(state));
		when(eventRepository.findExistingClientEventIds(any(), any())).thenReturn(List.of());
		List<MonitoringEvent> saved = new ArrayList<>();
		when(eventRepository.saveAll(anyList())).thenAnswer(invocation -> {
			saved.addAll(invocation.getArgument(0));
			return invocation.getArgument(0);
		});

		var response = monitoringService.recordEvents(
				STUDENT,
				ATTEMPT_ID,
				new MonitoringEventBatchRequest(
						UUID.randomUUID(),
						List.of(event(
								UUID.randomUUID(),
								MonitoringEventType.DEVTOOLS_SUSPECTED,
								NOW
						))
				)
		);

		assertEquals(100, response.riskScore());
		assertEquals(25, saved.getFirst().getRiskWeight());
		assertEquals(10, saved.getFirst().getRiskPointsApplied());
	}

	@Test
	void paginatesStaffEventHistoryAndEnforcesAttemptTenancy() {
		MonitoringEvent event = monitoringEvent();
		when(eventRepository.findByInstitutionIdAndAttemptId(
				any(),
				any(),
				any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(event, event, event, event, event), PageRequest.of(2, 10), 25));

		var response = monitoringService.events(STAFF, ATTEMPT_ID, 2, 10);

		assertEquals(2, response.page());
		assertEquals(10, response.size());
		assertEquals(25, response.totalElements());
		assertEquals(5, response.content().size());
		verify(attemptQuery).requireAttempt(INSTITUTION_ID, ATTEMPT_ID);

		doThrow(new AccessDeniedException("denied"))
				.when(attemptQuery).requireAttempt(INSTITUTION_ID, ATTEMPT_ID);
		assertThrows(
				AccessDeniedException.class,
				() -> monitoringService.events(STAFF, ATTEMPT_ID, 0, 20)
		);
	}

	@Test
	void returnsPaginatedDashboardAndRejectsCrossInstitutionExamAccess() {
		AttemptPage attempts = new AttemptPage(
				List.of(attempt()),
				1,
				10,
				12,
				2,
				false,
				true
		);
		MonitoringState state = state();
		state.applyHeartbeat(UUID.randomUUID(), 4, NOW, NOW, true, false, false);
		state.recordEvent(15);
		when(attemptQuery.findAttemptsByExam(INSTITUTION_ID, EXAM_ID, 1, 10)).thenReturn(attempts);
		when(stateRepository.findAllByInstitutionIdAndAttemptIdIn(
				INSTITUTION_ID,
				Set.of(ATTEMPT_ID)
		)).thenReturn(List.of(state));
		when(candidateQuery.findByIds(Set.of(CANDIDATE_ID))).thenReturn(Map.of(
				CANDIDATE_ID,
				candidate(INSTITUTION_ID)
		));

		var response = monitoringService.dashboard(STAFF, EXAM_ID, 1, 10);

		assertEquals(1, response.page());
		assertEquals(12, response.totalElements());
		assertEquals(ConnectivityState.OFFLINE, response.content().getFirst().connectivity());
		assertEquals(15, response.content().getFirst().riskScore());
		assertEquals(UserStatus.ACTIVE, response.content().getFirst().candidateStatus());

		doThrow(new AccessDeniedException("denied"))
				.when(examQuery).requireExam(INSTITUTION_ID, EXAM_ID);
		assertThrows(
				AccessDeniedException.class,
				() -> monitoringService.dashboard(STAFF, EXAM_ID, 0, 20)
		);
	}

	private static HeartbeatRequest heartbeat(
			long sequence,
			String deviceId,
			boolean focused,
			boolean fullscreen,
			boolean online
	) {
		return new HeartbeatRequest(
				UUID.randomUUID(),
				sequence,
				NOW.minusSeconds(1),
				deviceId,
				focused,
				fullscreen,
				online
		);
	}

	private static MonitoringEventRequest event(
			UUID eventId,
			MonitoringEventType type,
			Instant occurredAt
	) {
		return new MonitoringEventRequest(eventId, type, occurredAt, Map.of("screen", "exam"));
	}

	private static AttemptView attempt() {
		return new AttemptView(
				ATTEMPT_ID,
				INSTITUTION_ID,
				EXAM_ID,
				CANDIDATE_ID,
				AttemptStatus.IN_PROGRESS
		);
	}

	private static MonitoringState state() {
		return new MonitoringState(INSTITUTION_ID, EXAM_ID, ATTEMPT_ID, CANDIDATE_ID);
	}

	private static CandidateProfile candidate(UUID institutionId) {
		return new CandidateProfile(
				CANDIDATE_ID,
				institutionId,
				"Ada",
				"Student",
				"ada@example.edu",
				"STU-001",
				UserStatus.ACTIVE
		);
	}

	private static MonitoringEvent monitoringEvent() {
		MonitoringEvent event = new MonitoringEvent(
				INSTITUTION_ID,
				EXAM_ID,
				ATTEMPT_ID,
				CANDIDATE_ID,
				UUID.randomUUID(),
				MonitoringEventType.TAB_HIDDEN,
				NOW.minusSeconds(1),
				NOW,
				Map.of("screen", "exam"),
				10,
				10
		);
		ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
		return event;
	}
}
