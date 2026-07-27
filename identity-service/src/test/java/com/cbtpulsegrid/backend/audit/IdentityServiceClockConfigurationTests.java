package com.cbtpulsegrid.backend.audit;

import java.time.Clock;

import com.cbtpulsegrid.backend.IdentityServiceClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(classes = {
		IdentityServiceClockConfiguration.class,
		AuditService.class,
		IdentityServiceClockConfigurationTests.Dependencies.class
})
class IdentityServiceClockConfigurationTests {

	@Autowired
	private Clock clock;

	@Autowired
	private AuditService auditService;

	@Test
	void providesUtcClockAndCreatesAuditService() {
		assertThat(clock.getZone()).isEqualTo(Clock.systemUTC().getZone());
		assertThat(auditService).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	static class Dependencies {

		@Bean
		AuditEventRepository auditEventRepository() {
			return mock(AuditEventRepository.class);
		}

		@Bean
		AuditEventQueryRepository auditEventQueryRepository() {
			return mock(AuditEventQueryRepository.class);
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}
	}
}
