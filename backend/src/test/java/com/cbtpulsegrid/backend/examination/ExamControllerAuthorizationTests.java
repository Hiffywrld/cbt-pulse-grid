package com.cbtpulsegrid.backend.examination;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.examination.api.ExamActor;
import com.cbtpulsegrid.backend.examination.api.ExamController;
import com.cbtpulsegrid.backend.examination.api.ExamDetailResponse;
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
@ContextConfiguration(classes = ExamControllerAuthorizationTests.TestConfiguration.class)
class ExamControllerAuthorizationTests {

	@Autowired
	private ExamController examController;
	@Autowired
	private ExamService examService;

	@Test
	void rejectsUnauthenticatedExamAccess() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> examController.get(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void rejectsStudentExamAccess() {
		assertThrows(
				AccessDeniedException.class,
				() -> examController.get(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void rejectsSuperAdminExamManagement() {
		assertThrows(
				AccessDeniedException.class,
				() -> examController.create(null, null)
		);
	}

	@Test
	@WithMockUser(roles = "INVIGILATOR")
	void rejectsInvigilatorExamWrites() {
		assertThrows(
				AccessDeniedException.class,
				() -> examController.create(null, null)
		);
	}

	@Test
	@WithMockUser(roles = "INVIGILATOR")
	void permitsInvigilatorExamReads() {
		UUID institutionId = UUID.randomUUID();
		UUID examId = UUID.randomUUID();
		Jwt jwt = jwt("INVIGILATOR", institutionId);
		ExamDetailResponse expected = response(examId, institutionId, ExamStatus.PUBLISHED);
		when(examService.get(any(ExamActor.class), eq(examId))).thenReturn(expected);

		assertSame(expected, examController.get(jwt, examId));
	}

	@Test
	@WithMockUser(roles = "EXAMINER")
	void permitsExaminerExamManagement() {
		UUID institutionId = UUID.randomUUID();
		Jwt jwt = jwt("EXAMINER", institutionId);
		ExamDetailResponse expected = response(UUID.randomUUID(), institutionId, ExamStatus.DRAFT);
		when(examService.create(any(ExamActor.class), any())).thenReturn(expected);

		assertSame(expected, examController.create(jwt, null).getBody());
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

	private static ExamDetailResponse response(UUID id, UUID institutionId, ExamStatus status) {
		Instant now = Instant.now();
		return new ExamDetailResponse(
				id,
				institutionId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				"MAT-202",
				"Algebra Examination",
				null,
				60,
				now.plusSeconds(3600),
				now.plusSeconds(7200),
				true,
				true,
				true,
				status,
				List.of(),
				now,
				now,
				0
		);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class TestConfiguration {

		@Bean
		ExamService examService() {
			return mock(ExamService.class);
		}

		@Bean
		ExamController examController(ExamService examService) {
			return new ExamController(examService);
		}
	}
}
