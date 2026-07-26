package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.cbtpulsegrid.backend.monitoring.MonitoringEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class WebhookOutboxService {

	private static final String PAYLOAD_VERSION = "1";

	private final WebhookSubscriptionRepository subscriptionRepository;
	private final WebhookDeliveryRepository deliveryRepository;
	private final WebhookProperties properties;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	WebhookOutboxService(
			WebhookSubscriptionRepository subscriptionRepository,
			WebhookDeliveryRepository deliveryRepository,
			WebhookProperties properties,
			ObjectMapper objectMapper,
			Clock clock
	) {
		this.subscriptionRepository = subscriptionRepository;
		this.deliveryRepository = deliveryRepository;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public int enqueue(MonitoringEvent event, int totalRiskScore) {
		if (!properties.enabled()) {
			return 0;
		}
		if (event.getId() == null) {
			throw new IllegalStateException("Monitoring event must be persisted before outbox creation");
		}
		List<WebhookSubscription> subscriptions = subscriptionRepository.findMatchingActive(
				event.getInstitutionId(),
				WebhookSubscriptionStatus.ACTIVE,
				event.getEventType()
		);
		if (subscriptions.isEmpty()) {
			return 0;
		}
		Instant serverTimestamp = clock.instant();
		byte[] payloadBody = serialize(new WebhookPayload(
				PAYLOAD_VERSION,
				event.getId(),
				event.getEventType(),
				event.getInstitutionId(),
				event.getExamId(),
				event.getAttemptId(),
				event.getCandidateId(),
				event.getOccurredAt(),
				event.getReceivedAt(),
				event.getRiskPointsApplied(),
				totalRiskScore,
				serverTimestamp
		));
		List<WebhookDelivery> deliveries = new ArrayList<>();
		for (WebhookSubscription subscription : subscriptions) {
			if (!subscription.matches(event.getEventType())
					|| deliveryRepository.existsBySubscriptionIdAndMonitoringEventId(
							subscription.getId(),
							event.getId()
					)) {
				continue;
			}
			deliveries.add(new WebhookDelivery(
					event.getInstitutionId(),
					subscription.getId(),
					event.getId(),
					event.getEventType(),
					subscription.getSecretVersion(),
					payloadBody,
					serverTimestamp
			));
		}
		if (!deliveries.isEmpty()) {
			deliveryRepository.saveAll(deliveries);
		}
		return deliveries.size();
	}

	private byte[] serialize(WebhookPayload payload) {
		try {
			return objectMapper.writeValueAsBytes(payload);
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("Webhook payload serialization failed", exception);
		}
	}
}
