package com.cbtpulsegrid.backend.identity.account;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.UserStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserAccountControllerAuthorizationTests.TestConfiguration.class)
class UserAccountControllerAuthorizationTests {

	@Autowired
	private UserAccountController userAccountController;

	@Autowired
	private UserAccountService userAccountService;

	@Test
	void rejectsUnauthenticatedAccess() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> userAccountController.get(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void rejectsUsersWithoutAnAdministrativeRole() {
		assertThrows(
				AccessDeniedException.class,
				() -> userAccountController.get(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "INSTITUTION_ADMIN")
	void permitsInstitutionAdminAccess() {
		UUID actorId = UUID.randomUUID();
		UUID institutionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Instant now = Instant.now();
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(actorId.toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.claim("institutionId", institutionId.toString())
				.claim("roles", List.of("INSTITUTION_ADMIN"))
				.build();
		UserResponse expected = new UserResponse(
				userId,
				"Exam",
				"Officer",
				"examiner@one.edu",
				institutionId,
				Set.of(Role.EXAMINER),
				null,
				UserStatus.ACTIVE,
				now,
				now,
				0
		);
		when(userAccountService.get(any(ActorContext.class), eq(userId))).thenReturn(expected);

		assertSame(expected, userAccountController.get(jwt, userId));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class TestConfiguration {

		@Bean
		UserAccountService userAccountService() {
			return mock(UserAccountService.class);
		}

		@Bean
		UserAccountController userAccountController(UserAccountService userAccountService) {
			return new UserAccountController(userAccountService);
		}
	}
}
