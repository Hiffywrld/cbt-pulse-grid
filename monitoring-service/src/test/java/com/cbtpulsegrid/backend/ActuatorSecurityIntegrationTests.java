package com.cbtpulsegrid.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void livenessAndReadinessArePublicWithoutSensitiveDetails() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());

		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void nonHealthActuatorInformationRemainsProtected() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.requestId").exists());
	}

	@Test
	void localCorsAllowsLocalhostAndIpv4LoopbackWithoutWildcard() throws Exception {
		for (String origin : new String[] {"http://localhost:5173", "http://127.0.0.1:5173"}) {
			mockMvc.perform(options("/api/v1/auth/login")
							.header("Origin", origin)
							.header("Access-Control-Request-Method", "POST")
							.header("Access-Control-Request-Headers", "content-type"))
					.andExpect(status().isOk())
					.andExpect(header().string("Access-Control-Allow-Origin", origin));
		}

		mockMvc.perform(options("/api/v1/auth/login")
						.header("Origin", "http://192.0.2.10:5173")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
