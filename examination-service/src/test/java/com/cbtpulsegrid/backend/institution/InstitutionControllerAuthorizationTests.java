package com.cbtpulsegrid.backend.institution;

import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.institution.api.InstitutionController;
import com.cbtpulsegrid.backend.institution.api.InstitutionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InstitutionControllerAuthorizationTests.TestConfiguration.class)
class InstitutionControllerAuthorizationTests {

	@Autowired
	private InstitutionController institutionController;

	@Autowired
	private InstitutionService institutionService;

	@Test
	void rejectsUnauthenticatedAccess() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> institutionController.get(UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "STUDENT")
	void rejectsUsersWithoutSuperAdminRole() {
		assertThrows(
				AccessDeniedException.class,
				() -> institutionController.get(UUID.randomUUID())
		);
	}

	@Test
	@WithMockUser(roles = "SUPER_ADMIN")
	void permitsSuperAdminAccess() {
		UUID id = UUID.randomUUID();
		InstitutionResponse expected = new InstitutionResponse(
				id,
				"Central College",
				"CBT-01",
				InstitutionStatus.ACTIVE,
				Instant.EPOCH,
				Instant.EPOCH,
				0
		);
		when(institutionService.get(id)).thenReturn(expected);

		assertSame(expected, institutionController.get(id));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class TestConfiguration {

		@Bean
		InstitutionService institutionService() {
			return mock(InstitutionService.class);
		}

		@Bean
		InstitutionController institutionController(InstitutionService institutionService) {
			return new InstitutionController(institutionService);
		}
	}
}
