package com.cbtpulsegrid.backend;

import java.util.Objects;

import com.cbtpulsegrid.backend.identity.security.SecurityConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigurationTests {

	@Test
	void apiDocsArePublicAndContainApiTitleAndBearerScheme() {
		AntPathMatcher pathMatcher = new AntPathMatcher();
		boolean apiDocsArePublic = SecurityConfiguration.PUBLIC_DOCUMENTATION_ENDPOINTS.stream()
				.filter(Objects::nonNull)
				.anyMatch(pattern -> pathMatcher.match(pattern, "/v3/api-docs"));
		OpenAPI openApi = new OpenApiConfiguration().cbtPulseGridOpenApi();
		SecurityScheme bearerAuth = openApi.getComponents().getSecuritySchemes().get("bearerAuth");

		assertTrue(apiDocsArePublic);
		assertEquals("CBT-Pulse Grid API", openApi.getInfo().getTitle());
		assertNotNull(bearerAuth);
		assertEquals(SecurityScheme.Type.HTTP, bearerAuth.getType());
		assertEquals("bearer", bearerAuth.getScheme());
	}
}
