package com.cbtpulsegrid.backend.monitoring.webhook;

import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Query;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryTests {

	private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");

	@Mock
	private WebhookDeliveryRepository deliveryRepository;
	@Mock
	private WebhookSubscriptionRepository subscriptionRepository;

	private WebhookProperties properties;
	private WebhookDeliveryCoordinator coordinator;

	@BeforeEach
	void createCoordinator() {
		properties = WebhookSecurityTests.properties(false);
		coordinator = new WebhookDeliveryCoordinator(
				deliveryRepository,
				subscriptionRepository,
				new WebhookRetryPolicy(properties),
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void claimsBoundedDueDeliveriesAndRecoversExpiredLeases() {
		WebhookDelivery delivery = delivery();
		UUID firstReplica = UUID.randomUUID();
		when(deliveryRepository.findDueForUpdate(NOW, 25)).thenReturn(List.of(delivery));

		assertEquals(List.of(delivery.getId()), coordinator.claimDue(firstReplica));
		assertEquals(WebhookDeliveryStatus.IN_FLIGHT, delivery.getStatus());
		assertEquals(1, delivery.getAttemptCount());
		assertEquals(firstReplica, delivery.getLeaseOwner());
		assertEquals(NOW.plusSeconds(30), delivery.getLeaseExpiresAt());

		UUID secondReplica = UUID.randomUUID();
		assertEquals(List.of(delivery.getId()), coordinator.claimDue(secondReplica));
		assertEquals(2, delivery.getAttemptCount());
		assertEquals(secondReplica, delivery.getLeaseOwner());
	}

	@Test
	void successfulTwoHundredResponseCompletesDelivery() {
		UUID owner = UUID.randomUUID();
		WebhookDelivery delivery = claimed(owner, 1);
		when(deliveryRepository.findClaimForUpdate(
				delivery.getId(),
				owner,
				WebhookDeliveryStatus.IN_FLIGHT
		)).thenReturn(Optional.of(delivery));

		coordinator.completeHttp(delivery.getId(), owner, 204);

		assertEquals(WebhookDeliveryStatus.SUCCEEDED, delivery.getStatus());
		assertEquals(204, delivery.getResponseStatus());
		assertEquals(NOW, delivery.getDeliveredAt());
	}

	@ParameterizedTest
	@ValueSource(ints = {408, 429, 500, 503})
	void schedulesRetryableHttpResponsesWithExponentialBackoff(int status) {
		UUID owner = UUID.randomUUID();
		WebhookDelivery delivery = claimed(owner, 1);
		when(deliveryRepository.findClaimForUpdate(
				delivery.getId(),
				owner,
				WebhookDeliveryStatus.IN_FLIGHT
		)).thenReturn(Optional.of(delivery));

		coordinator.completeHttp(delivery.getId(), owner, status);

		assertEquals(WebhookDeliveryStatus.PENDING, delivery.getStatus());
		assertEquals(NOW.plusSeconds(10), delivery.getNextAttemptAt());
		assertEquals(status, delivery.getResponseStatus());
	}

	@Test
	void marksOtherFourHundredResponsesAsPermanentFailures() {
		UUID owner = UUID.randomUUID();
		WebhookDelivery delivery = claimed(owner, 1);
		when(deliveryRepository.findClaimForUpdate(
				delivery.getId(),
				owner,
				WebhookDeliveryStatus.IN_FLIGHT
		)).thenReturn(Optional.of(delivery));

		coordinator.completeHttp(delivery.getId(), owner, 422);

		assertEquals(WebhookDeliveryStatus.FAILED, delivery.getStatus());
		assertEquals("Non-retryable HTTP 422", delivery.getFailureReason());
	}

	@Test
	void retriesTimeoutsThenDeadLettersAtMaximumAttempts() {
		UUID owner = UUID.randomUUID();
		WebhookDelivery delivery = claimed(owner, properties.maximumAttempts());
		when(deliveryRepository.findClaimForUpdate(
				delivery.getId(),
				owner,
				WebhookDeliveryStatus.IN_FLIGHT
		)).thenReturn(Optional.of(delivery));

		coordinator.completeTransportFailure(delivery.getId(), owner, true);

		assertEquals(WebhookDeliveryStatus.DEAD_LETTER, delivery.getStatus());
		assertEquals("Webhook request timed out", delivery.getFailureReason());
		assertEquals(null, delivery.getResponseStatus());
	}

	@Test
	void calculatesCappedExponentialSchedule() {
		WebhookRetryPolicy policy = new WebhookRetryPolicy(properties);

		assertEquals(Duration.ofSeconds(10), policy.delayAfterAttempt(1));
		assertEquals(Duration.ofSeconds(20), policy.delayAfterAttempt(2));
		assertEquals(Duration.ofSeconds(40), policy.delayAfterAttempt(3));
		assertEquals(Duration.ofMinutes(15), policy.delayAfterAttempt(20));
	}

	@Test
	void pausedSubscriptionIsNotPreparedForNetworkDelivery() {
		UUID owner = UUID.randomUUID();
		WebhookDelivery delivery = claimed(owner, 1);
		WebhookSubscription subscription = subscription(WebhookSubscriptionStatus.PAUSED);
		when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
		when(subscriptionRepository.findById(delivery.getSubscriptionId()))
				.thenReturn(Optional.of(subscription));

		assertTrue(coordinator.prepare(delivery.getId(), owner).isEmpty());
	}

	@Test
	void dueQueryUsesActiveSubscriptionsAndPostgresqlSkipLocked() throws Exception {
		Method method = WebhookDeliveryRepository.class.getDeclaredMethod(
				"findDueForUpdate",
				Instant.class,
				int.class
		);
		String sql = method.getAnnotation(Query.class).value().toLowerCase();

		assertTrue(sql.contains("subscription.status = 'active'"));
		assertTrue(sql.contains("for update of delivery skip locked"));
		assertTrue(sql.contains("lease_expires_at <= :now"));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void httpSenderUsesSignedHeadersExactBodyAndNoRedirects() throws Exception {
		HttpClient client = org.mockito.Mockito.mock(HttpClient.class);
		HttpResponse<Void> response = org.mockito.Mockito.mock(HttpResponse.class);
		WebhookUrlValidator validator = org.mockito.Mockito.mock(WebhookUrlValidator.class);
		WebhookSigner signer = org.mockito.Mockito.mock(WebhookSigner.class);
		when(validator.validate("https://receiver.example/hook"))
				.thenReturn(URI.create("https://receiver.example/hook"));
		when(signer.sign(any(), eq(1), eq("1893492000"), any()))
				.thenReturn("v1=abcdef");
		when(response.statusCode()).thenReturn(202);
		when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenReturn(response);
		WebhookHttpSender sender = new WebhookHttpSender(
				client,
				validator,
				signer,
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		WebhookDeliveryContext context = context();

		assertEquals(202, sender.send(context));

		ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
		verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
		assertEquals("POST", request.getValue().method());
		assertEquals(context.eventId().toString(), request.getValue().headers()
				.firstValue(WebhookHttpSender.EVENT_ID_HEADER).orElseThrow());
		assertEquals("v1=abcdef", request.getValue().headers()
				.firstValue(WebhookHttpSender.SIGNATURE_HEADER).orElseThrow());
		assertEquals(properties.responseTimeout(), request.getValue().timeout().orElseThrow());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void httpSenderClassifiesTimeoutWithoutLeakingRemoteDetails() throws Exception {
		HttpClient client = org.mockito.Mockito.mock(HttpClient.class);
		WebhookUrlValidator validator = org.mockito.Mockito.mock(WebhookUrlValidator.class);
		WebhookSigner signer = org.mockito.Mockito.mock(WebhookSigner.class);
		when(validator.validate("https://receiver.example/hook"))
				.thenReturn(URI.create("https://receiver.example/hook"));
		when(signer.sign(any(), anyInt(), any(), any())).thenReturn("v1=abcdef");
		doThrow(new HttpTimeoutException("internal remote detail"))
				.when(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
		WebhookHttpSender sender = new WebhookHttpSender(
				client,
				validator,
				signer,
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);

		WebhookTransportException exception = assertThrows(
				WebhookTransportException.class,
				() -> sender.send(context())
		);
		assertTrue(exception.isTimeout());
		assertFalse(exception.getMessage().contains("internal remote detail"));
	}

	private static WebhookDelivery delivery() {
		WebhookDelivery delivery = new WebhookDelivery(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				MonitoringEventType.TAB_HIDDEN,
				1,
				"{}".getBytes(StandardCharsets.UTF_8),
				NOW
		);
		ReflectionTestUtils.setField(delivery, "id", UUID.randomUUID());
		return delivery;
	}

	private static WebhookDelivery claimed(UUID owner, int attemptCount) {
		WebhookDelivery delivery = delivery();
		ReflectionTestUtils.setField(delivery, "status", WebhookDeliveryStatus.IN_FLIGHT);
		ReflectionTestUtils.setField(delivery, "leaseOwner", owner);
		ReflectionTestUtils.setField(delivery, "leaseExpiresAt", NOW.plusSeconds(30));
		ReflectionTestUtils.setField(delivery, "attemptCount", attemptCount);
		return delivery;
	}

	private static WebhookSubscription subscription(WebhookSubscriptionStatus status) {
		WebhookSubscription subscription = new WebhookSubscription(
				UUID.randomUUID(),
				"Receiver",
				"https://receiver.example/hook",
				java.util.Set.of(),
				UUID.randomUUID()
		);
		ReflectionTestUtils.setField(subscription, "status", status);
		return subscription;
	}

	private static WebhookDeliveryContext context() {
		return new WebhookDeliveryContext(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				MonitoringEventType.DEVTOOLS_SUSPECTED,
				"https://receiver.example/hook",
				1,
				"{\"payloadVersion\":\"1\"}".getBytes(StandardCharsets.UTF_8),
				1
		);
	}
}
