package com.cbtpulsegrid.backend.monitoring;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringUpdateType;
import com.cbtpulsegrid.backend.monitoring.webhook.WebhookOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissedHeartbeatServiceTests {

	private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
	private static final Instant CUTOFF = NOW.minusSeconds(30);

	@Mock
	private MonitoringStateRepository stateRepository;
	@Mock
	private MonitoringEventRepository eventRepository;
	@Mock
	private MonitoringLiveUpdateNotifier liveUpdateNotifier;
	@Mock
	private WebhookOutboxService webhookOutboxService;
	@Mock
	private AuditTrail auditTrail;

	private MissedHeartbeatService service;

	@BeforeEach
	void createService() {
		service = serviceAt(NOW);
	}

	@Test
	void detectsTimeoutOnceAndRecordsZeroRiskMissedEvent() {
		MonitoringState state = onlineState(NOW.minusSeconds(31));
		when(stateRepository.findTimedOutForUpdate(CUTOFF, NOW, 100)).thenReturn(List.of(state));
		when(eventRepository.save(any(MonitoringEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		assertEquals(1, service.detectMissedHeartbeats());

		assertTrue(state.isHeartbeatOutageActive());
		assertFalse(state.getOnline());
		assertEquals(1, state.getEventCount());
		assertEquals(0, state.getRiskScore());
		ArgumentCaptor<MonitoringEvent> event = ArgumentCaptor.forClass(MonitoringEvent.class);
		verify(eventRepository).save(event.capture());
		assertEquals(MonitoringEventType.HEARTBEAT_MISSED, event.getValue().getEventType());
		assertEquals(0, event.getValue().getRiskWeight());
		assertEquals(0, event.getValue().getRiskPointsApplied());
		verify(webhookOutboxService).enqueue(event.getValue(), 0);
		verify(liveUpdateNotifier).publish(
				any(),
				eq(state),
				eq(MonitoringUpdateType.HEARTBEAT_MISSED),
				eq(NOW)
		);
	}

	@Test
	void doesNotCreateAnotherMissedEventDuringTheSameOutage() {
		MonitoringState state = onlineState(NOW.minusSeconds(31));
		when(stateRepository.findTimedOutForUpdate(CUTOFF, NOW, 100))
				.thenReturn(List.of(state))
				.thenReturn(List.of());

		assertEquals(1, service.detectMissedHeartbeats());
		assertEquals(0, service.detectMissedHeartbeats());

		verify(eventRepository, times(1)).save(any(MonitoringEvent.class));
		verify(liveUpdateNotifier, times(1)).publish(any(), any(), any(), any());
	}

	@Test
	void recoveryAllowsASecondLaterOutageEpisode() {
		MonitoringState state = onlineState(NOW.minusSeconds(31));
		when(stateRepository.findTimedOutForUpdate(CUTOFF, NOW, 100)).thenReturn(List.of(state));
		service.detectMissedHeartbeats();

		state.applyHeartbeat(
				UUID.randomUUID(),
				2,
				NOW.plusSeconds(1),
				NOW.plusSeconds(1),
				true,
				true,
				true
		);
		assertTrue(state.restoreFromHeartbeat(NOW.plusSeconds(1)));
		assertFalse(state.isHeartbeatOutageActive());

		Instant secondScan = NOW.plusSeconds(40);
		when(stateRepository.findTimedOutForUpdate(secondScan.minusSeconds(30), secondScan, 100))
				.thenReturn(List.of(state));
		assertEquals(1, serviceAt(secondScan).detectMissedHeartbeats());

		assertTrue(state.isHeartbeatOutageActive());
		assertEquals(2, state.getEventCount());
		assertEquals(0, state.getRiskScore());
		verify(eventRepository, times(2)).save(any(MonitoringEvent.class));
	}

	@Test
	void timeoutSelectionUsesPostgresqlSkipLockedForReplicaSafety() throws NoSuchMethodException {
		Method method = MonitoringStateRepository.class.getDeclaredMethod(
				"findTimedOutForUpdate",
				Instant.class,
				Instant.class,
				int.class
		);
		String sql = method.getAnnotation(Query.class).value().toLowerCase();

		assertTrue(sql.contains("for update of state skip locked"));
		assertTrue(sql.contains("heartbeat_outage_active = false"));
		assertTrue(sql.contains("attempt.status = 'in_progress'"));
		assertTrue(sql.contains("attempt.expires_at > :now"));
	}

	private MissedHeartbeatService serviceAt(Instant instant) {
		return new MissedHeartbeatService(
				stateRepository,
				eventRepository,
				liveUpdateNotifier,
				webhookOutboxService,
				auditTrail,
				new MissedHeartbeatProperties(
						Duration.ofSeconds(10),
						Duration.ofSeconds(30),
						100
				),
				Clock.fixed(instant, ZoneOffset.UTC)
		);
	}

	private static MonitoringState onlineState(Instant receivedAt) {
		MonitoringState state = new MonitoringState(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID()
		);
		state.applyHeartbeat(
				UUID.randomUUID(),
				1,
				receivedAt,
				receivedAt,
				true,
				true,
				true
		);
		return state;
	}
}
