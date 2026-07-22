package com.cbtpulsegrid.backend.monitoring;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.api.HeartbeatResponse;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringActor;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringController;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringDashboardResponse;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringPageResponse;
import com.cbtpulsegrid.backend.monitoring.api.StudentMonitoringController;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MonitoringControllerAuthorizationTests.TestConfiguration.class)
class MonitoringControllerAuthorizationTests {

	@Autowired
	private StudentMonitoringController studentController;
	@Autowired
	private MonitoringController staffController;
	@Autowired
	private MonitoringService monitoringService;

	@Test
	void rejectsUnauthenticatedMonitoringAccess() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> studentController.heartbeat(null, UUID.randomUUID(), null)
		);
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> staffController.dashboard(null, UUID.randomUUID(), 0, 20)
		);
	}

	@Test
	@WithMockUser(roles = "EXAMINER")
	void rejectsStaffFromStudentMonitoringIngestion() {
		assertThrows(
				AccessDeniedException.class,
				() -> studentController.events(null, UUID.randomUUID(), null)
		);
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void rejectsStudentsFromStaffMonitoringReads() {
		assertThrows(
				AccessDeniedException.class,
				() -> staffController.events(null, UUID.randomUUID(), 0, 20)
		);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void rejectsSuperAdminFromInstitutionMonitoring() {
		assertThrows(
				AccessDeniedException.class,
				() -> staffController.dashboard(null, UUID.randomUUID(), 0, 20)
		);
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void permitsStudentHeartbeatEndpoint() {
		UUID institutionId = UUID.randomUUID();
		UUID attemptId = UUID.randomUUID();
		HeartbeatResponse expected = new HeartbeatResponse(
				attemptId,
				UUID.randomUUID(),
				1,
				Instant.now(),
				Instant.now(),
				true,
				true,
				true,
				true,
				0,
				0
		);
		when(monitoringService.recordHeartbeat(
				any(MonitoringActor.class),
				eq(attemptId),
				any()
		)).thenReturn(expected);

		assertSame(expected, studentController.heartbeat(jwt("STUDENT", institutionId), attemptId, null));
	}

	@Test
	@WithMockUser(roles = "INVIGILATOR")
	void permitsInvigilatorDashboardEndpoint() {
		UUID institutionId = UUID.randomUUID();
		UUID examId = UUID.randomUUID();
		MonitoringPageResponse<MonitoringDashboardResponse> expected = new MonitoringPageResponse<>(
				List.of(),
				0,
				20,
				0,
				0,
				true,
				true
		);
		when(monitoringService.dashboard(
				any(MonitoringActor.class),
				eq(examId),
				anyInt(),
				anyInt()
		)).thenReturn(expected);

		assertSame(expected, staffController.dashboard(jwt("INVIGILATOR", institutionId), examId, 0, 20));
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
		MonitoringService monitoringService() {
			return mock(MonitoringService.class);
		}

		@Bean
		StudentMonitoringController studentMonitoringController(MonitoringService monitoringService) {
			return new StudentMonitoringController(monitoringService);
		}

		@Bean
		MonitoringController monitoringController(MonitoringService monitoringService) {
			return new MonitoringController(monitoringService);
		}
	}
}
