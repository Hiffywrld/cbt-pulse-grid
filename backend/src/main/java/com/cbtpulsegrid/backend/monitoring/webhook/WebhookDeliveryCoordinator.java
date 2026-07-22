package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class WebhookDeliveryCoordinator {

	private static final int MAX_FAILURE_REASON_LENGTH = 500;

	private final WebhookDeliveryRepository deliveryRepository;
	private final WebhookSubscriptionRepository subscriptionRepository;
	private final WebhookRetryPolicy retryPolicy;
	private final WebhookProperties properties;
	private final Clock clock;

	WebhookDeliveryCoordinator(
			WebhookDeliveryRepository deliveryRepository,
			WebhookSubscriptionRepository subscriptionRepository,
			WebhookRetryPolicy retryPolicy,
			WebhookProperties properties,
			Clock clock
	) {
		this.deliveryRepository = deliveryRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.retryPolicy = retryPolicy;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public List<UUID> claimDue(UUID leaseOwner) {
		Instant now = clock.instant();
		List<WebhookDelivery> due = deliveryRepository.findDueForUpdate(now, properties.batchSize());
		List<UUID> claimed = new ArrayList<>(due.size());
		for (WebhookDelivery delivery : due) {
			if (delivery.getAttemptCount() >= properties.maximumAttempts()) {
				delivery.deadLetter(
						delivery.getResponseStatus(),
						"Delivery lease expired after the maximum attempt count"
				);
				continue;
			}
			delivery.claim(leaseOwner, now, properties.leaseDuration());
			claimed.add(delivery.getId());
		}
		deliveryRepository.flush();
		return List.copyOf(claimed);
	}

	@Transactional(readOnly = true)
	public Optional<WebhookDeliveryContext> prepare(UUID deliveryId, UUID leaseOwner) {
		WebhookDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
		if (delivery == null
				|| delivery.getStatus() != WebhookDeliveryStatus.IN_FLIGHT
				|| !leaseOwner.equals(delivery.getLeaseOwner())) {
			return Optional.empty();
		}
		WebhookSubscription subscription = subscriptionRepository.findById(
				delivery.getSubscriptionId()
		).orElseThrow(() -> new NoSuchElementException("Webhook subscription not found"));
		if (subscription.getStatus() != WebhookSubscriptionStatus.ACTIVE) {
			return Optional.empty();
		}
		return Optional.of(new WebhookDeliveryContext(
				delivery.getId(),
				delivery.getSubscriptionId(),
				delivery.getMonitoringEventId(),
				delivery.getEventType(),
				subscription.getDestinationUrl(),
				delivery.getSecretVersion(),
				delivery.getPayloadBody(),
				delivery.getAttemptCount()
		));
	}

	@Transactional
	public void releasePaused(UUID deliveryId, UUID leaseOwner) {
		deliveryRepository.findClaimForUpdate(
				deliveryId,
				leaseOwner,
				WebhookDeliveryStatus.IN_FLIGHT
		).ifPresent(delivery -> delivery.releasePausedClaim(clock.instant()));
	}

	@Transactional
	public void completeHttp(UUID deliveryId, UUID leaseOwner, int responseStatus) {
		WebhookDelivery delivery = findClaim(deliveryId, leaseOwner);
		Instant now = clock.instant();
		if (responseStatus >= 200 && responseStatus < 300) {
			delivery.succeed(now, responseStatus);
			return;
		}
		if (isRetryable(responseStatus)) {
			handleRetryable(delivery, now, responseStatus, "Retryable HTTP " + responseStatus);
			return;
		}
		delivery.fail(responseStatus, sanitize("Non-retryable HTTP " + responseStatus));
	}

	@Transactional
	public void completeTransportFailure(
			UUID deliveryId,
			UUID leaseOwner,
			boolean timeout
	) {
		WebhookDelivery delivery = findClaim(deliveryId, leaseOwner);
		handleRetryable(
				delivery,
				clock.instant(),
				null,
				timeout ? "Webhook request timed out" : "Webhook network delivery failed"
		);
	}

    @Transactional
    public void completePermanentFailure(
            UUID deliveryId,
            UUID leaseOwner,
            String failureReason
    ) {
        WebhookDelivery delivery = findClaim(deliveryId, leaseOwner);
        delivery.fail(null, sanitize(failureReason));
    }

	private WebhookDelivery findClaim(UUID deliveryId, UUID leaseOwner) {
		return deliveryRepository.findClaimForUpdate(
				deliveryId,
				leaseOwner,
				WebhookDeliveryStatus.IN_FLIGHT
		).orElseThrow(() -> new NoSuchElementException("Webhook delivery lease is no longer owned"));
	}

	private void handleRetryable(
			WebhookDelivery delivery,
			Instant now,
			Integer responseStatus,
			String failureReason
	) {
		String safeReason = sanitize(failureReason);
		if (delivery.getAttemptCount() >= properties.maximumAttempts()) {
			delivery.deadLetter(responseStatus, safeReason);
			return;
		}
		delivery.retryAt(
				now.plus(retryPolicy.delayAfterAttempt(delivery.getAttemptCount())),
				responseStatus,
				safeReason
		);
	}

	private static boolean isRetryable(int status) {
		return status == 408 || status == 429 || status >= 500;
	}

	private static String sanitize(String reason) {
		String normalized = reason == null
				? "Webhook delivery failed"
				: reason.replaceAll("[\\r\\n\\t]", " ").trim();
		return normalized.length() <= MAX_FAILURE_REASON_LENGTH
				? normalized
				: normalized.substring(0, MAX_FAILURE_REASON_LENGTH);
	}
}
