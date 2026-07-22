package com.cbtpulsegrid.backend.monitoring.webhook;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.ApiConflictException;
import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringActor;
import com.cbtpulsegrid.backend.monitoring.webhook.api.ChangeWebhookSubscriptionStatusRequest;
import com.cbtpulsegrid.backend.monitoring.webhook.api.CreateWebhookSubscriptionRequest;
import com.cbtpulsegrid.backend.monitoring.webhook.api.WebhookSubscriptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTests {

	private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
	private static final UUID INSTITUTION_ID = UUID.randomUUID();
	private static final UUID ADMIN_ID = UUID.randomUUID();
	private static final MonitoringActor ADMIN = new MonitoringActor(
			ADMIN_ID,
			INSTITUTION_ID,
			Set.of("INSTITUTION_ADMIN")
	);

	@Mock
	private WebhookSubscriptionRepository subscriptionRepository;
	@Mock
	private WebhookDeliveryRepository deliveryRepository;
	@Mock
	private WebhookUrlValidator urlValidator;
	@Mock
	private WebhookSigner signer;

	private WebhookService service;

	@BeforeEach
	void createService() {
		service = new WebhookService(
				subscriptionRepository,
				deliveryRepository,
				new WebhookAuthorization(),
				urlValidator,
				signer,
				WebhookSecurityTests.properties(false),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void returnsDerivedSecretOnlyAtCreationAndNormalResponsesHaveNoSecretField() {
		when(urlValidator.validate("https://receiver.example/hook"))
				.thenReturn(URI.create("https://receiver.example/hook"));
		when(subscriptionRepository.saveAndFlush(any(WebhookSubscription.class)))
				.thenAnswer(invocation -> persisted(invocation.getArgument(0)));
		when(signer.deriveEncodedSecret(any(UUID.class), anyInt())).thenReturn("derived-secret");

		var created = service.create(
				ADMIN,
				new CreateWebhookSubscriptionRequest(
						" Security receiver ",
						"https://receiver.example/hook",
						Set.of(MonitoringEventType.COPY_ATTEMPT)
				)
		);

		assertEquals("derived-secret", created.secret());
		assertEquals("Security receiver", created.subscription().name());
		assertEquals(1, created.subscription().secretVersion());
		assertTrue(created.toString().contains("[REDACTED]"));
		assertFalse(java.util.Arrays.stream(WebhookSubscriptionResponse.class.getRecordComponents())
				.anyMatch(component -> component.getName().equalsIgnoreCase("secret")));
	}

	@Test
	void rotationIncrementsVersionAndReturnsASeparateSecretOnce() {
		WebhookSubscription subscription = persisted(subscription(INSTITUTION_ID));
		when(subscriptionRepository.findByIdForUpdate(subscription.getId()))
				.thenReturn(Optional.of(subscription));
		when(signer.deriveEncodedSecret(subscription.getId(), 2)).thenReturn("rotated-secret");

		var response = service.rotateSecret(ADMIN, subscription.getId());

		assertEquals(2, response.subscription().secretVersion());
		assertEquals("rotated-secret", response.secret());
	}

	@Test
	void enforcesAdministratorRoleAndCrossTenantOwnership() {
		MonitoringActor student = new MonitoringActor(
				UUID.randomUUID(),
				INSTITUTION_ID,
				Set.of("STUDENT")
		);
		assertThrows(
				AccessDeniedException.class,
				() -> service.get(student, UUID.randomUUID())
		);

		WebhookSubscription otherTenant = persisted(subscription(UUID.randomUUID()));
		when(subscriptionRepository.findByIdForUpdate(otherTenant.getId()))
				.thenReturn(Optional.of(otherTenant));
		assertThrows(
				AccessDeniedException.class,
				() -> service.changeStatus(
						ADMIN,
						otherTenant.getId(),
						new ChangeWebhookSubscriptionStatusRequest(WebhookSubscriptionStatus.PAUSED)
				)
		);
	}

	@Test
	void rejectsDuplicateSubscriptionNames() {
		when(subscriptionRepository.existsByInstitutionIdAndNameIgnoreCase(
				INSTITUTION_ID,
				"Receiver"
		)).thenReturn(true);

		assertThrows(
				ApiConflictException.class,
				() -> service.create(
						ADMIN,
						new CreateWebhookSubscriptionRequest(
								"Receiver",
								"https://receiver.example/hook",
								Set.of()
						)
		));
	}

	@Test
	void manualRetryIsTenantScopedAndResetsFailedDelivery() {
		WebhookDelivery owned = delivery(INSTITUTION_ID);
		owned.fail(400, "Non-retryable HTTP 400");
		when(deliveryRepository.findByIdForUpdate(owned.getId())).thenReturn(Optional.of(owned));

		var response = service.retry(ADMIN, owned.getId());

		assertEquals(WebhookDeliveryStatus.PENDING, response.status());
		assertEquals(0, response.attemptCount());

		WebhookDelivery otherTenant = delivery(UUID.randomUUID());
		otherTenant.deadLetter(503, "Retry exhausted");
		when(deliveryRepository.findByIdForUpdate(otherTenant.getId()))
				.thenReturn(Optional.of(otherTenant));
		assertThrows(
				AccessDeniedException.class,
				() -> service.retry(ADMIN, otherTenant.getId())
		);
	}

	private static WebhookSubscription subscription(UUID institutionId) {
		return new WebhookSubscription(
				institutionId,
				"Receiver",
				"https://receiver.example/hook",
				Set.of(),
				ADMIN_ID
		);
	}

	private static WebhookSubscription persisted(WebhookSubscription subscription) {
		if (subscription.getId() == null) {
			ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
		}
		ReflectionTestUtils.setField(subscription, "createdAt", NOW);
		ReflectionTestUtils.setField(subscription, "updatedAt", NOW);
		return subscription;
	}

	private static WebhookDelivery delivery(UUID institutionId) {
		WebhookDelivery delivery = new WebhookDelivery(
				institutionId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				MonitoringEventType.TAB_HIDDEN,
				1,
				"{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
				NOW
		);
		ReflectionTestUtils.setField(delivery, "id", UUID.randomUUID());
		return delivery;
	}
}
