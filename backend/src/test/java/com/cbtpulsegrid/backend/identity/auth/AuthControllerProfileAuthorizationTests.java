package com.cbtpulsegrid.backend.identity.auth;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerProfileAuthorizationTests {

	private final AuthService authService = mock(AuthService.class);
	private final CurrentUserProfileService profileService = mock(CurrentUserProfileService.class);
	private final AuthController controller = new AuthController(authService, profileService);

	@ParameterizedTest
	@ValueSource(strings = {"SUPER_ADMIN", "INSTITUTION_ADMIN", "EXAMINER", "INVIGILATOR", "STUDENT"})
	void everyAuthenticatedDomainRoleCanUpdateItsOwnProfile(String role) {
		Jwt jwt = jwt(role);
		UpdateProfileRequest request = new UpdateProfileRequest("Profile", "User", "emerald-orbit");
		CurrentUserResponse expected = new CurrentUserResponse(
				UUID.fromString(jwt.getSubject()), "profile@example.test", "Profile", "User",
				null, institutionId(jwt), "Institution", "INST", "emerald-orbit", List.of(role));
		when(profileService.update(
				UUID.fromString(jwt.getSubject()), institutionId(jwt), List.of(role), request))
				.thenReturn(expected);

		assertSame(expected, controller.updateProfile(jwt, request));
	}

	@ParameterizedTest
	@ValueSource(strings = {"SUPER_ADMIN", "INSTITUTION_ADMIN", "EXAMINER", "INVIGILATOR", "STUDENT"})
	void everyAuthenticatedDomainRoleCanChangeItsOwnPassword(String role) {
		Jwt jwt = jwt(role);
		ChangePasswordRequest request = new ChangePasswordRequest("Current1!", "NewStrong1!", "NewStrong1!");

		assertEquals(HttpStatus.NO_CONTENT, controller.changePassword(jwt, request).getStatusCode());
		verify(profileService).changePassword(UUID.fromString(jwt.getSubject()), request);
	}

	@ParameterizedTest
	@ValueSource(strings = {"firstName", "lastName", "avatarKey"})
	void profileRequestContainsOnlyAllowedSelfServiceFields(String expectedField) {
		Set<String> fields = Arrays.stream(UpdateProfileRequest.class.getRecordComponents())
				.map(component -> component.getName())
				.collect(Collectors.toSet());
		assertEquals(Set.of("firstName", "lastName", "avatarKey"), fields);
		assertEquals(true, fields.contains(expectedField));
	}

	private static Jwt jwt(String role) {
		Instant now = Instant.now();
		Jwt.Builder builder = Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(UUID.randomUUID().toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.claim("roles", List.of(role));
		if (!role.equals("SUPER_ADMIN")) {
			builder.claim("institutionId", UUID.randomUUID().toString());
		}
		return builder.build();
	}

	private static UUID institutionId(Jwt jwt) {
		String value = jwt.getClaimAsString("institutionId");
		return value == null ? null : UUID.fromString(value);
	}
}
