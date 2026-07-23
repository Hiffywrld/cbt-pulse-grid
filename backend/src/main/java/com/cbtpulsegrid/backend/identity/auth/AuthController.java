package com.cbtpulsegrid.backend.identity.auth;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "JWT authentication and authenticated-user operations")
public class AuthController {

	private final AuthService authService;
	private final CurrentUserProfileService currentUserProfileService;

	public AuthController(AuthService authService, CurrentUserProfileService currentUserProfileService) {
		this.authService = authService;
		this.currentUserProfileService = currentUserProfileService;
	}

	@PostMapping("/login")
	@Operation(summary = "Authenticate with email and password")
	@SecurityRequirements
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Authentication succeeded"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "401", description = "Invalid email or password")
	})
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request.email(), request.password());
	}

	@PostMapping("/refresh")
	@Operation(summary = "Rotate a refresh token and issue new tokens")
	@SecurityRequirements
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Tokens refreshed"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "401", description = "Refresh token is invalid, expired, or revoked")
	})
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request.refreshToken());
	}

	@PostMapping("/logout")
	@Operation(summary = "Revoke a refresh token")
	@SecurityRequirements
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Refresh token revoked"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "401", description = "Refresh token is invalid, expired, or revoked")
	})
	public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	@Operation(summary = "Get the authenticated user profile")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Authenticated profile returned with safe user and institution display fields",
					content = @Content(schema = @Schema(implementation = CurrentUserResponse.class))
			),
			@ApiResponse(responseCode = "401", description = "Authentication is required")
	})
	public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
		String institutionClaim = jwt.getClaimAsString("institutionId");
		UUID institutionId = institutionClaim == null ? null : UUID.fromString(institutionClaim);
		List<String> roles = jwt.getClaimAsStringList("roles");

		return currentUserProfileService.get(
				UUID.fromString(jwt.getSubject()),
				institutionId,
				roles == null ? List.of() : List.copyOf(roles)
		);
	}
}
