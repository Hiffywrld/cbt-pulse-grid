package com.cbtpulsegrid.backend.questionbank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.api.QuestionBankActor;
import com.cbtpulsegrid.backend.questionbank.api.QuestionController;
import com.cbtpulsegrid.backend.questionbank.api.StaffQuestionResponse;
import com.cbtpulsegrid.backend.questionbank.api.SubjectController;
import com.cbtpulsegrid.backend.questionbank.api.SubjectResponse;
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
@ContextConfiguration(classes = QuestionBankControllerAuthorizationTests.TestConfiguration.class)
class QuestionBankControllerAuthorizationTests {

	@Autowired
	private SubjectController subjectController;

	@Autowired
	private QuestionController questionController;

	@Autowired
	private SubjectService subjectService;

	@Autowired
	private QuestionService questionService;

	@Test
	void rejectsUnauthenticatedSubjectAccess() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> subjectController.get(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void rejectsStudentQuestionAccess() {
		assertThrows(
				AccessDeniedException.class,
				() -> questionController.get(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "INVIGILATOR")
	void rejectsInvigilatorQuestionAccess() {
		assertThrows(
				AccessDeniedException.class,
				() -> questionController.get(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void rejectsSuperAdminAcademicContentAccess() {
		assertThrows(
				AccessDeniedException.class,
				() -> questionController.get(null, UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "EXAMINER")
	void permitsExaminerSubjectReadsButNotSubjectWrites() {
		UUID institutionId = UUID.randomUUID();
		UUID subjectId = UUID.randomUUID();
		Instant now = Instant.now();
		Jwt jwt = jwt("EXAMINER", institutionId);
		SubjectResponse expected = new SubjectResponse(
				subjectId,
				institutionId,
				"MAT-101",
				"Mathematics",
				null,
				SubjectStatus.ACTIVE,
				now,
				now,
				0
		);
		when(subjectService.get(any(QuestionBankActor.class), eq(subjectId))).thenReturn(expected);

		assertSame(expected, subjectController.get(jwt, subjectId));
		assertThrows(AccessDeniedException.class, () -> subjectController.create(jwt, null));
	}

	@Test
	@WithMockUser(roles = "EXAMINER")
	void permitsExaminerQuestionManagement() {
		UUID institutionId = UUID.randomUUID();
		UUID questionId = UUID.randomUUID();
		UUID subjectId = UUID.randomUUID();
		UUID examinerId = UUID.randomUUID();
		Instant now = Instant.now();
		Jwt jwt = jwt("EXAMINER", institutionId);
		StaffQuestionResponse expected = new StaffQuestionResponse(
				questionId,
				institutionId,
				subjectId,
				examinerId,
				"Which value is prime?",
				QuestionType.SINGLE_CHOICE,
				QuestionDifficulty.EASY,
				BigDecimal.ONE,
				QuestionStatus.DRAFT,
				List.of(),
				now,
				now,
				0
		);
		when(questionService.get(any(QuestionBankActor.class), eq(questionId))).thenReturn(expected);

		assertSame(expected, questionController.get(jwt, questionId));
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
		SubjectService subjectService() {
			return mock(SubjectService.class);
		}

		@Bean
		QuestionService questionService() {
			return mock(QuestionService.class);
		}

		@Bean
		SubjectController subjectController(SubjectService subjectService) {
			return new SubjectController(subjectService);
		}

		@Bean
		QuestionController questionController(QuestionService questionService) {
			return new QuestionController(questionService);
		}
	}
}
