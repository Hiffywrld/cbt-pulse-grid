package com.cbtpulsegrid.backend.identity.auth;

import java.util.List;
import java.util.UUID;

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
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request.email(), request.password());
	}

	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request.refreshToken());
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
		String institutionClaim = jwt.getClaimAsString("institutionId");
		UUID institutionId = institutionClaim == null ? null : UUID.fromString(institutionClaim);
		List<String> roles = jwt.getClaimAsStringList("roles");

		return new CurrentUserResponse(
				UUID.fromString(jwt.getSubject()),
				jwt.getClaimAsString("email"),
				institutionId,
				roles == null ? List.of() : List.copyOf(roles)
		);
	}
}
