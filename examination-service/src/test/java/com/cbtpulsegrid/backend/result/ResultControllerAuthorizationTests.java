package com.cbtpulsegrid.backend.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.result.api.ExamResultSummaryResponse;
import com.cbtpulsegrid.backend.result.api.ResultActor;
import com.cbtpulsegrid.backend.result.api.ResultController;
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
@ContextConfiguration(classes = ResultControllerAuthorizationTests.TestConfiguration.class)
class ResultControllerAuthorizationTests {

	@Autowired
	private ResultController controller;
	@Autowired
	private ResultService resultService;

	@Test
	void rejectsUnauthenticatedAccess() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> controller.summary(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void rejectsStudentsFromStaffResults() {
		assertThrows(
				AccessDeniedException.class,
				() -> controller.summary(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "INVIGILATOR")
	void rejectsInvigilatorResultsAndCsvExport() {
		UUID examId = UUID.randomUUID();
		assertThrows(
				AccessDeniedException.class,
				() -> controller.summary(null, examId)
		);
		assertThrows(
				AccessDeniedException.class,
				() -> controller.export(null, examId, null, null, null)
		);
	}

	@Test
	@WithMockUser(roles = "EXAMINER")
	void permitsInstitutionExaminerResults() {
		UUID institutionId = UUID.randomUUID();
		UUID examId = UUID.randomUUID();
		ExamResultSummaryResponse expected = new ExamResultSummaryResponse(
				examId,
				"EX-1",
				"Exam",
				0, 0, 0, 0, 0, 0, 0,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO
		);
		when(resultService.summary(any(ResultActor.class), eq(examId))).thenReturn(expected);

		assertSame(expected, controller.summary(jwt("EXAMINER", institutionId), examId));
	}

	private static Jwt jwt(String role, UUID institutionId) {
		Instant now = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(UUID.randomUUID().toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.claim("institutionId", institutionId.toString())
				.claim("roles", List.of(role))
				.build();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class TestConfiguration {

		@Bean
		ResultService resultService() {
			return mock(ResultService.class);
		}

		@Bean
		ResultController resultController(ResultService resultService) {
			return new ResultController(resultService);
		}
	}
}
