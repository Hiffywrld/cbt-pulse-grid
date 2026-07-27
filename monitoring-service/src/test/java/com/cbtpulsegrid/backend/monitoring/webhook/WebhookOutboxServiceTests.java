package com.cbtpulsegrid.backend.monitoring.webhook;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEvent;
import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookOutboxServiceTests {

	private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
	private static final UUID INSTITUTION_ID = UUID.randomUUID();

	@Mock
	private WebhookSubscriptionRepository subscriptionRepository;
	@Mock
	private WebhookDeliveryRepository deliveryRepository;

	private WebhookOutboxService outboxService;

	@BeforeEach
	void createService() {
		outboxService = new WebhookOutboxService(
				subscriptionRepository,
				deliveryRepository,
				WebhookSecurityTests.properties(false),
				JsonMapper.builder().findAndAddModules().build(),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void createsFilteredImmutableSafePayloadDeliveriesInTheCallingTransaction() throws Exception {
		MonitoringEvent event = event(MonitoringEventType.TAB_HIDDEN, 10);
		WebhookSubscription allEvents = subscription(Set.of());
		WebhookSubscription selected = subscription(Set.of(MonitoringEventType.TAB_HIDDEN));
		when(subscriptionRepository.findMatchingActive(
				INSTITUTION_ID,
				WebhookSubscriptionStatus.ACTIVE,
				MonitoringEventType.TAB_HIDDEN
		)).thenReturn(List.of(allEvents, selected));
		List<WebhookDelivery> saved = new ArrayList<>();
		when(deliveryRepository.saveAll(anyList())).thenAnswer(invocation -> {
			saved.addAll(invocation.getArgument(0));
			return invocation.getArgument(0);
		});

		assertEquals(2, outboxService.enqueue(event, 35));

		assertEquals(2, saved.size());
		assertEquals(event.getId(), saved.getFirst().getMonitoringEventId());
		assertEquals(WebhookDeliveryStatus.PENDING, saved.getFirst().getStatus());
		JsonNode payload = JsonMapper.builder().findAndAddModules().build()
				.readTree(saved.getFirst().getPayloadBody());
		assertEquals("1", payload.get("payloadVersion").asText());
		assertEquals(event.getId().toString(), payload.get("eventId").asText());
		assertEquals("TAB_HIDDEN", payload.get("eventType").asText());
		assertEquals(10, payload.get("riskPointsApplied").asInt());
		assertEquals(35, payload.get("totalRiskScore").asInt());
		String serialized = payload.toString().toLowerCase();
		assertFalse(serialized.contains("device"));
		assertFalse(serialized.contains("password"));
		assertFalse(serialized.contains("token"));
		assertFalse(serialized.contains("pin"));
		assertFalse(serialized.contains("correct"));

		Method method = WebhookOutboxService.class.getMethod(
				"enqueue",
				MonitoringEvent.class,
				int.class
		);
		Transactional transactional = method.getAnnotation(Transactional.class);
		assertEquals(Propagation.MANDATORY, transactional.propagation());
	}

	@Test
	void filtersNonMatchingSubscriptionsAndPreventsDuplicateEventDelivery() {
		MonitoringEvent event = event(MonitoringEventType.WINDOW_BLUR, 5);
		WebhookSubscription nonMatching = subscription(Set.of(MonitoringEventType.COPY_ATTEMPT));
		WebhookSubscription duplicate = subscription(Set.of());
		when(subscriptionRepository.findMatchingActive(
				INSTITUTION_ID,
				WebhookSubscriptionStatus.ACTIVE,
				MonitoringEventType.WINDOW_BLUR
		)).thenReturn(List.of(nonMatching, duplicate));
		when(deliveryRepository.existsBySubscriptionIdAndMonitoringEventId(
				duplicate.getId(),
				event.getId()
		)).thenReturn(true);

		assertEquals(0, outboxService.enqueue(event, 5));

		verify(deliveryRepository, never()).saveAll(anyList());
	}

	@Test
	void disabledFeatureDoesNotCreateOutboxRecords() {
		WebhookProperties disabled = new WebhookProperties(
				false,
				"",
				false,
				Duration.ofSeconds(3),
				Duration.ofSeconds(5),
				Duration.ofSeconds(5),
				Duration.ofSeconds(30),
				Duration.ofSeconds(10),
				Duration.ofMinutes(15),
				25,
				8
		);
		WebhookOutboxService disabledService = new WebhookOutboxService(
				subscriptionRepository,
				deliveryRepository,
				disabled,
				JsonMapper.builder().findAndAddModules().build(),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);

		assertEquals(0, disabledService.enqueue(event(MonitoringEventType.COPY_ATTEMPT, 20), 20));
		verify(subscriptionRepository, never()).findMatchingActive(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any()
		);
	}

	private static WebhookSubscription subscription(Set<MonitoringEventType> eventTypes) {
		WebhookSubscription subscription = new WebhookSubscription(
				INSTITUTION_ID,
				"Receiver " + UUID.randomUUID(),
				"https://receiver.example/hook",
				eventTypes,
				UUID.randomUUID()
		);
		ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
		return subscription;
	}

	private static MonitoringEvent event(MonitoringEventType type, int risk) {
		MonitoringEvent event = new MonitoringEvent(
				INSTITUTION_ID,
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				type,
				NOW.minusSeconds(1),
				NOW,
				Map.of("screen", "exam"),
				risk,
				risk
		);
		ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
		return event;
	}
}
