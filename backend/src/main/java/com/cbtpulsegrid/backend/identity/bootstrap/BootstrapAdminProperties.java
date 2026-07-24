package com.cbtpulsegrid.backend.identity.bootstrap;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(
		boolean enabled,
		boolean forceReset,
		@NotBlank @Email String email,
		@NotBlank String password
) {
}
