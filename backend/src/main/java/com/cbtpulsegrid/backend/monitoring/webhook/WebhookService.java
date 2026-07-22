package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.ApiConflictException;
import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringActor;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringPageResponse;
import com.cbtpulsegrid.backend.monitoring.webhook.api.ChangeWebhookSubscriptionStatusRequest;
import com.cbtpulsegrid.backend.monitoring.webhook.api.CreateWebhookSubscriptionRequest;
import com.cbtpulsegrid.backend.monitoring.webhook.api.WebhookDeliveryResponse;
import com.cbtpulsegrid.backend.monitoring.webhook.api.WebhookSubscriptionResponse;
import com.cbtpulsegrid.backend.monitoring.webhook.api.WebhookSubscriptionSecretResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

	private static final int MAX_PAGE_SIZE = 100;

	private final WebhookSubscriptionRepository subscriptionRepository;
	private final WebhookDeliveryRepository deliveryRepository;
	private final WebhookAuthorization authorization;
	private final WebhookUrlValidator urlValidator;
	private final WebhookSigner signer;
	private final WebhookProperties properties;
	private final Clock clock;

	WebhookService(
			WebhookSubscriptionRepository subscriptionRepository,
			WebhookDeliveryRepository deliveryRepository,
			WebhookAuthorization authorization,
			WebhookUrlValidator urlValidator,
			WebhookSigner signer,
			WebhookProperties properties,
			Clock clock
	) {
		this.subscriptionRepository = subscriptionRepository;
		this.deliveryRepository = deliveryRepository;
		this.authorization = authorization;
		this.urlValidator = urlValidator;
		this.signer = signer;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public WebhookSubscriptionSecretResponse create(
			MonitoringActor actor,
			CreateWebhookSubscriptionRequest request
	) {
		requireEnabled();
		UUID institutionId = authorization.requireInstitutionAdministrator(actor);
		String name = request.name().trim();
		if (subscriptionRepository.existsByInstitutionIdAndNameIgnoreCase(institutionId, name)) {
			throw new ApiConflictException("Webhook subscription name already exists");
		}
		String destinationUrl = urlValidator.validate(request.destinationUrl()).toASCIIString();
		Set<MonitoringEventType> eventTypes = request.eventTypes() == null
				? Set.of()
				: Set.copyOf(request.eventTypes());
		WebhookSubscription subscription = subscriptionRepository.saveAndFlush(
				new WebhookSubscription(
						institutionId,
						name,
						destinationUrl,
						eventTypes,
						actor.userId()
				)
		);
		return secretResponse(subscription);
	}

	@Transactional(readOnly = true)
	public MonitoringPageResponse<WebhookSubscriptionResponse> list(
			MonitoringActor actor,
			int page,
			int size
	) {
		requireEnabled();
		UUID institutionId = authorization.requireInstitutionAdministrator(actor);
		validatePage(page, size);
		Page<WebhookSubscriptionResponse> subscriptions = subscriptionRepository.findByInstitutionId(
				institutionId,
				PageRequest.of(
						page,
						size,
						Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
				)
		).map(WebhookService::toResponse);
		return MonitoringPageResponse.from(subscriptions);
	}

	@Transactional(readOnly = true)
	public WebhookSubscriptionResponse get(MonitoringActor actor, UUID id) {
		requireEnabled();
		UUID institutionId = authorization.requireInstitutionAdministrator(actor);
		return toResponse(subscriptionRepository.findByInstitutionIdAndId(institutionId, id)
				.orElseThrow(() -> new NoSuchElementException("Webhook subscription not found")));
	}

	@Transactional
	public WebhookSubscriptionResponse changeStatus(
			MonitoringActor actor,
			UUID id,
			ChangeWebhookSubscriptionStatusRequest request
	) {
		requireEnabled();
		UUID institutionId = authorization.requireInstitutionAdministrator(actor);
		WebhookSubscription subscription = requireForUpdate(id);
		authorization.requireTenant(institutionId, subscription.getInstitutionId());
		subscription.changeStatus(request.status(), actor.userId());
		subscriptionRepository.flush();
		return toResponse(subscription);
	}

	@Transactional
	public WebhookSubscriptionSecretResponse rotateSecret(MonitoringActor actor, UUID id) {
		requireEnabled();
		UUID institutionId = authorization.requireInstitutionAdministrator(actor);
		WebhookSubscription subscription = requireForUpdate(id);
		authorization.requireTenant(institutionId, subscription.getInstitutionId());
		subscription.rotateSecret(actor.userId());
		subscriptionRepository.flush();
		return secretResponse(subscription);
	}

	@Transactional(readOnly = true)
	public MonitoringPageResponse<WebhookDeliveryResponse> deliveries(
			MonitoringActor actor,
			WebhookDeliveryStatus status,
			int page,
			int size
	) {
		requireEnabled();
		UUID institutionId = authorization.requireInstitutionAdministrator(actor);
		validatePage(page, size);
		PageRequest pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
		);
		Page<WebhookDelivery> deliveries = status == null
				? deliveryRepository.findByInstitutionId(institutionId, pageable)
				: deliveryRepository.findByInstitutionIdAndStatus(institutionId, status, pageable);
		return MonitoringPageResponse.from(deliveries.map(WebhookService::toResponse));
	}

	@Transactional
	public WebhookDeliveryResponse retry(MonitoringActor actor, UUID deliveryId) {
		requireEnabled();
		UUID institutionId = authorization.requireInstitutionAdministrator(actor);
		WebhookDelivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
				.orElseThrow(() -> new NoSuchElementException("Webhook delivery not found"));
		authorization.requireTenant(institutionId, delivery.getInstitutionId());
		delivery.manualRetry(clock.instant());
		deliveryRepository.flush();
		return toResponse(delivery);
	}

	private WebhookSubscription requireForUpdate(UUID id) {
		return subscriptionRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new NoSuchElementException("Webhook subscription not found"));
	}

	private WebhookSubscriptionSecretResponse secretResponse(WebhookSubscription subscription) {
		return new WebhookSubscriptionSecretResponse(
				toResponse(subscription),
				signer.deriveEncodedSecret(subscription.getId(), subscription.getSecretVersion())
		);
	}

	private static WebhookSubscriptionResponse toResponse(WebhookSubscription subscription) {
		return new WebhookSubscriptionResponse(
				subscription.getId(),
				subscription.getInstitutionId(),
				subscription.getName(),
				subscription.getDestinationUrl(),
				subscription.getStatus(),
				subscription.isAllEventTypes(),
				subscription.getEventTypes(),
				subscription.getSecretVersion(),
				subscription.getCreatedBy(),
				subscription.getUpdatedBy(),
				subscription.getCreatedAt(),
				subscription.getUpdatedAt(),
				subscription.getVersion()
		);
	}

	private static WebhookDeliveryResponse toResponse(WebhookDelivery delivery) {
		return new WebhookDeliveryResponse(
				delivery.getId(),
				delivery.getSubscriptionId(),
				delivery.getMonitoringEventId(),
				delivery.getEventType(),
				delivery.getStatus(),
				delivery.getAttemptCount(),
				delivery.getNextAttemptAt(),
				delivery.getResponseStatus(),
				delivery.getFailureReason(),
				delivery.getDeliveredAt(),
				delivery.getCreatedAt(),
				delivery.getUpdatedAt(),
				delivery.getVersion()
		);
	}

	private void requireEnabled() {
		if (!properties.enabled()) {
			throw new IllegalStateException("Webhook delivery is disabled");
		}
	}

	private static void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Webhook pagination is invalid");
		}
	}
}
