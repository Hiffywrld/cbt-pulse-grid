package com.cbtpulsegrid.backend.identity.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@NotBlank String issuer,
		@NotNull Duration accessTokenExpiry,
		@NotNull Duration refreshTokenExpiry,
		@NotBlank @Size(min = 32) String secret
) {

	public JwtProperties {
		if (accessTokenExpiry != null && (accessTokenExpiry.isZero() || accessTokenExpiry.isNegative())) {
			throw new IllegalArgumentException("Access token expiry must be positive");
		}
		if (refreshTokenExpiry != null && (refreshTokenExpiry.isZero() || refreshTokenExpiry.isNegative())) {
			throw new IllegalArgumentException("Refresh token expiry must be positive");
		}
	}
}
