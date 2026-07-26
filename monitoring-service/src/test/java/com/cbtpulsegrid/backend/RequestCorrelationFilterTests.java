package com.cbtpulsegrid.backend;

import java.util.Map;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.auth.AuthExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestCorrelationFilterTests {

	private final JsonMapper mapper = JsonMapper.builder().build();
	private MockMvc mockMvc;

	@BeforeEach
	void configureMvc() {
		mockMvc = MockMvcBuilders.standaloneSetup(new CorrelationController())
				.addFilters(new RequestCorrelationFilter(mapper))
				.setControllerAdvice(new AuthExceptionHandler())
				.build();
	}

	@Test
	void generatesOrPreservesCanonicalRequestIds() throws Exception {
		MvcResult generated = mockMvc.perform(get("/correlation/ok"))
				.andExpect(status().isOk())
				.andReturn();
		String generatedId = generated.getResponse().getHeader(RequestCorrelation.HEADER_NAME);
		assertNotNull(generatedId);
		assertEquals(generatedId, UUID.fromString(generatedId).toString());

		String supplied = UUID.randomUUID().toString();
		mockMvc.perform(get("/correlation/ok").header(RequestCorrelation.HEADER_NAME, supplied))
				.andExpect(status().isOk())
				.andExpect(result -> assertEquals(
						supplied,
						result.getResponse().getHeader(RequestCorrelation.HEADER_NAME)
				));
	}

	@Test
	void rejectsMalformedOrUnreasonablyLongRequestIds() throws Exception {
		MvcResult result = mockMvc.perform(get("/correlation/ok")
					.header(RequestCorrelation.HEADER_NAME, "not-a-uuid-" + "x".repeat(100)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("X-Request-Id must contain one canonical UUID"))
				.andReturn();

		JsonNode body = mapper.readTree(result.getResponse().getContentAsByteArray());
		String responseId = result.getResponse().getHeader(RequestCorrelation.HEADER_NAME);
		assertEquals(responseId, body.get("requestId").asText());
		assertEquals(responseId, UUID.fromString(responseId).toString());
	}

	@Test
	void genericErrorsContainTheSameSafeCorrelationId() throws Exception {
		MvcResult result = mockMvc.perform(get("/correlation/fail"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("An unexpected error occurred"))
				.andExpect(jsonPath("$.requestId").exists())
				.andReturn();

		JsonNode body = mapper.readTree(result.getResponse().getContentAsByteArray());
		assertEquals(
				result.getResponse().getHeader(RequestCorrelation.HEADER_NAME),
				body.get("requestId").asText()
		);
		assertTrue(!result.getResponse().getContentAsString().contains("sensitive-internal-detail"));
	}

	@RestController
	static class CorrelationController {

		@GetMapping("/correlation/ok")
		Map<String, String> ok() {
			return Map.of("status", "ok");
		}

		@GetMapping("/correlation/fail")
		Map<String, String> fail() {
			throw new RuntimeException("sensitive-internal-detail");
		}
	}
}
