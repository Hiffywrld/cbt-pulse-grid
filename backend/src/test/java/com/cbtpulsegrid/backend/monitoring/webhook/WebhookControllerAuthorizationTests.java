package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.webhook.api.WebhookController;
import com.cbtpulsegrid.backend.monitoring.webhook.api.WebhookSubscriptionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WebhookControllerAuthorizationTests.TestConfiguration.class)
class WebhookControllerAuthorizationTests {

	@Autowired
	private WebhookController controller;
	@Autowired
	private WebhookService service;

	@Test
	void rejectsUnauthenticatedRequests() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> controller.getSubscription(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void rejectsStudents() {
		assertThrows(
				AccessDeniedException.class,
				() -> controller.getSubscription(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "EXAMINER")
	void rejectsNonAdministrativeStaff() {
		assertThrows(
				AccessDeniedException.class,
				() -> controller.getSubscription(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "INSTITUTION_ADMIN")
	void permitsInstitutionAdministrators() {
		UUID institutionId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Jwt jwt = jwt(institutionId);
		WebhookSubscriptionResponse expected = response(subscriptionId, institutionId);
		when(service.get(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(
				subscriptionId
		))).thenReturn(expected);

		assertSame(expected, controller.getSubscription(jwt, subscriptionId));
	}

	private static Jwt jwt(UUID institutionId) {
		Instant now = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(UUID.randomUUID().toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.claim("institutionId", institutionId.toString())
				.claim("roles", List.of("INSTITUTION_ADMIN"))
				.build();
	}

	private static WebhookSubscriptionResponse response(UUID id, UUID institutionId) {
		Instant now = Instant.now();
		UUID actorId = UUID.randomUUID();
		return new WebhookSubscriptionResponse(
				id,
				institutionId,
				"Receiver",
				"https://receiver.example/hook",
				WebhookSubscriptionStatus.ACTIVE,
				true,
				Set.of(),
				1,
				actorId,
				actorId,
				now,
				now,
				0
		);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class TestConfiguration {

		@Bean
		WebhookService webhookService() {
			return mock(WebhookService.class);
		}

		@Bean
		WebhookController webhookController(WebhookService webhookService) {
			return new WebhookController(webhookService);
		}
	}
}
