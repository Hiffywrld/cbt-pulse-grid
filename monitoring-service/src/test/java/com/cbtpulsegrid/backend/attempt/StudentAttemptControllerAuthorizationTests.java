package com.cbtpulsegrid.backend.attempt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.api.StudentActor;
import com.cbtpulsegrid.backend.attempt.api.StudentAttemptController;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StudentAttemptControllerAuthorizationTests.TestConfiguration.class)
class StudentAttemptControllerAuthorizationTests {

	@Autowired
	private StudentAttemptController controller;

	@Autowired
	private AttemptService attemptService;

	@Test
	void rejectsUnauthenticatedStudentExamAccess() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> controller.listExams(null)
		);
	}

	@Test
	@WithMockUser(roles = "EXAMINER")
	void rejectsStaffFromStudentAttemptEndpoints() {
		assertThrows(AccessDeniedException.class, () -> controller.listExams(null));
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void permitsStudentAccess() {
		UUID institutionId = UUID.randomUUID();
		when(attemptService.listAssignedExams(any(StudentActor.class))).thenReturn(List.of());

		assertEquals(0, controller.listExams(jwt(institutionId)).size());
	}

	private static Jwt jwt(UUID institutionId) {
		Instant now = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(UUID.randomUUID().toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.claim("institutionId", institutionId.toString())
				.claim("roles", List.of("STUDENT"))
				.build();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class TestConfiguration {

		@Bean
		AttemptService attemptService() {
			return mock(AttemptService.class);
		}

		@Bean
		StudentAttemptController studentAttemptController(AttemptService attemptService) {
			return new StudentAttemptController(attemptService);
		}
	}
}
