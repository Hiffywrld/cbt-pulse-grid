package com.cbtpulsegrid.backend;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI cbtPulseGridOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("CBT-Pulse Grid API")
						.version("v1")
						.description(
								"Distributed institutional computer based testing platform with anti-cheat controls"
						))
				.components(new Components().addSecuritySchemes(
						"bearerAuth",
						new SecurityScheme()
								.name("bearerAuth")
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
				));
	}
}
