package com.cbtpulsegrid.backend.identity.account;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTITUTION_ADMIN')")
@Tag(name = "Users", description = "Institutional user-account administration")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

	private final UserAccountService userAccountService;

	public UserAccountController(UserAccountService userAccountService) {
		this.userAccountService = userAccountService;
	}

	@PostMapping({"", "/"})
	@Operation(summary = "Create an institutional user account")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "User account created"),
			@ApiResponse(responseCode = "400", description = "Request or role combination is invalid"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Institution not found"),
			@ApiResponse(responseCode = "409", description = "Email or registration number already exists")
	})
	public ResponseEntity<UserResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateUserRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(userAccountService.create(toActor(jwt), request));
	}

	@GetMapping({"", "/"})
	@Operation(summary = "List and filter institutional user accounts")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User-account page returned"),
			@ApiResponse(responseCode = "400", description = "Invalid pagination or filter value"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied")
	})
	public UserPageResponse<UserResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) @Size(max = 254) String search,
			@RequestParam(required = false) UUID institutionId,
			@RequestParam(required = false) Role role,
			@RequestParam(required = false) UserStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return userAccountService.list(
				toActor(jwt),
				search,
				institutionId,
				role,
				status,
				page,
				size
		);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get an institutional user account")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User account returned"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "User not found")
	})
	public UserResponse get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id
	) {
		return userAccountService.get(toActor(jwt), id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update user profile fields")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User account updated"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = "409", description = "Registration number already exists")
	})
	public UserResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody UpdateUserRequest request
	) {
		return userAccountService.update(toActor(jwt), id, request);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Change a user-account status")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "User status changed"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "User not found")
	})
	public UserResponse changeStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody ChangeUserStatusRequest request
	) {
		return userAccountService.changeStatus(toActor(jwt), id, request.status());
	}

	private static ActorContext toActor(Jwt jwt) {
		List<String> roleClaims = jwt.getClaimAsStringList("roles");
		Set<Role> roles = roleClaims == null
				? Set.of()
				: roleClaims.stream().map(Role::valueOf).collect(Collectors.toUnmodifiableSet());

		String institutionClaim = jwt.getClaimAsString("institutionId");
		UUID institutionId = institutionClaim == null ? null : UUID.fromString(institutionClaim);
		return new ActorContext(UUID.fromString(jwt.getSubject()), institutionId, roles);
	}
}
