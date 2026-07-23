package com.cbtpulsegrid.backend.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.api.AuditActor;
import com.cbtpulsegrid.backend.audit.api.AuditController;
import com.cbtpulsegrid.backend.audit.api.AuditPageResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AuditControllerAuthorizationTests.TestConfiguration.class)
class AuditControllerAuthorizationTests {

	@Autowired
	private AuditController controller;
	@Autowired
	private AuditService auditService;

	@Test
	void rejectsUnauthenticatedAccess() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> controller.list(null, null, null, null, null, null, 0, 20)
		);
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void rejectsStudents() {
		assertThrows(
				AccessDeniedException.class,
				() -> controller.list(null, null, null, null, null, null, 0, 20)
		);
	}

	@Test
	@WithMockUser(roles = "INSTITUTION_ADMIN")
	void permitsInstitutionAdministrator() {
		UUID institutionId = UUID.randomUUID();
		AuditPageResponse<?> expected = new AuditPageResponse<>(List.of(), 0, 20, 0, 0);
		when(auditService.findEvents(any(AuditActor.class), any(), any(), any(), any(), any(), anyInt(), anyInt()))
				.thenReturn((AuditPageResponse) expected);

		assertSame(
				expected,
				controller.list(jwt(institutionId), null, null, null, null, null, 0, 20)
		);
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

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class TestConfiguration {

		@Bean
		AuditService auditService() {
			return mock(AuditService.class);
		}

		@Bean
		AuditController auditController(AuditService auditService) {
			return new AuditController(auditService);
		}
	}
}
