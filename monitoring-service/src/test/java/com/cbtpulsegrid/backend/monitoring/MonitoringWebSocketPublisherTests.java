package com.cbtpulsegrid.backend.monitoring;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import java.util.Set;

import com.cbtpulsegrid.backend.attempt.AttemptMonitoringQuery.AttemptView;
import com.cbtpulsegrid.backend.attempt.AttemptStatus;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery;
import com.cbtpulsegrid.backend.identity.ExaminationCandidateQuery.CandidateProfile;
import com.cbtpulsegrid.backend.identity.UserStatus;
import com.cbtpulsegrid.backend.monitoring.api.ConnectivityState;
import com.cbtpulsegrid.backend.monitoring.api.LiveMonitoringUpdate;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringUpdateType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringWebSocketPublisherTests {

	@Test
	void buildsDashboardSafeCandidateAndAttemptUpdate() {
		ExaminationCandidateQuery candidateQuery = mock(ExaminationCandidateQuery.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		MonitoringLiveUpdateNotifier notifier = new MonitoringLiveUpdateNotifier(
				candidateQuery,
				eventPublisher
		);
		UUID institutionId = UUID.randomUUID();
		UUID examId = UUID.randomUUID();
		UUID attemptId = UUID.randomUUID();
		UUID candidateId = UUID.randomUUID();
		Instant now = Instant.parse("2030-01-01T10:00:00Z");
		AttemptView attempt = new AttemptView(
				attemptId,
				institutionId,
				examId,
				candidateId,
				AttemptStatus.IN_PROGRESS
		);
		MonitoringState state = new MonitoringState(
				institutionId,
				examId,
				attemptId,
				candidateId
		);
		state.applyHeartbeat(UUID.randomUUID(), 1, now, now, true, false, true);
		state.recordEvent(10);
		when(candidateQuery.findByIds(Set.of(candidateId))).thenReturn(Map.of(
				candidateId,
				new CandidateProfile(
						candidateId,
						institutionId,
						"Ada",
						"Student",
						"ada@example.edu",
						"STU-001",
						UserStatus.ACTIVE
				)
		));

		notifier.publish(attempt, state, MonitoringUpdateType.MONITORING_EVENTS, now);

		ArgumentCaptor<MonitoringUpdateEvent> event = ArgumentCaptor.forClass(
				MonitoringUpdateEvent.class
		);
		verify(eventPublisher).publishEvent(event.capture());
		assertEquals(examId, event.getValue().examId());
		assertEquals(attemptId, event.getValue().update().attemptId());
		assertEquals("Ada", event.getValue().update().candidateFirstName());
		assertEquals("STU-001", event.getValue().update().registrationNumber());
		assertEquals(ConnectivityState.ONLINE, event.getValue().update().connectivity());
		assertEquals(10, event.getValue().update().riskScore());
	}

	@Test
	void publishesSafeUpdateToExamTopicOnlyAfterCommit() throws NoSuchMethodException {
		SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
		MonitoringWebSocketPublisher publisher = new MonitoringWebSocketPublisher(messagingTemplate);
		UUID examId = UUID.randomUUID();
		LiveMonitoringUpdate update = update();

		publisher.publish(new MonitoringUpdateEvent(examId, update));

		verify(messagingTemplate).convertAndSend(
				"/topic/exams/" + examId + "/monitoring",
				update
		);
		Method method = MonitoringWebSocketPublisher.class.getDeclaredMethod(
				"publish",
				MonitoringUpdateEvent.class
		);
		TransactionalEventListener annotation = method.getAnnotation(
				TransactionalEventListener.class
		);
		assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
		assertFalse(annotation.fallbackExecution());
		assertFalse(java.util.Arrays.stream(LiveMonitoringUpdate.class.getRecordComponents())
				.map(component -> component.getName().toLowerCase())
				.anyMatch(name -> name.contains("device")
						|| name.contains("hash")
						|| name.contains("pin")
						|| name.contains("token")
						|| name.contains("correct")));
	}

	private static LiveMonitoringUpdate update() {
		Instant now = Instant.parse("2030-01-01T10:00:00Z");
		return new LiveMonitoringUpdate(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"Ada",
				"Student",
				"STU-001",
				UserStatus.ACTIVE,
				AttemptStatus.IN_PROGRESS,
				now,
				ConnectivityState.ONLINE,
				true,
				true,
				2,
				10,
				MonitoringUpdateType.HEARTBEAT,
				now
		);
	}
}
