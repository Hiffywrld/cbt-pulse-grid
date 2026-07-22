package com.cbtpulsegrid.backend.identity.account;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.UserStatus;
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
public class UserAccountController {

	private final UserAccountService userAccountService;

	public UserAccountController(UserAccountService userAccountService) {
		this.userAccountService = userAccountService;
	}

	@PostMapping({"", "/"})
	public ResponseEntity<UserResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateUserRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(userAccountService.create(toActor(jwt), request));
	}

	@GetMapping({"", "/"})
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
	public UserResponse get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id
	) {
		return userAccountService.get(toActor(jwt), id);
	}

	@PutMapping("/{id}")
	public UserResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody UpdateUserRequest request
	) {
		return userAccountService.update(toActor(jwt), id, request);
	}

	@PatchMapping("/{id}/status")
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
