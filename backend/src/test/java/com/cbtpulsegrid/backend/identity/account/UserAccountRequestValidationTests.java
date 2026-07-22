package com.cbtpulsegrid.backend.identity.account;

import java.util.Set;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.identity.UserStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountRequestValidationTests {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void validatesRequiredCreationFieldsAndPasswordLength() {
		CreateUserRequest request = new CreateUserRequest(
				" ",
				" ",
				"invalid-email",
				"short",
				null,
				Set.of(),
				null
		);
		Set<String> fields = fields(validator.validate(request));

		assertTrue(fields.containsAll(Set.of("firstName", "lastName", "email", "password", "roles")));
	}

	@Test
	void validatesUserUpdateNames() {
		Set<String> fields = fields(validator.validate(new UpdateUserRequest(" ", " ", null)));

		assertTrue(fields.containsAll(Set.of("firstName", "lastName")));
	}

	@Test
	void validatesStatusChanges() {
		assertTrue(fields(validator.validate(new ChangeUserStatusRequest(null))).contains("status"));
		assertTrue(fields(validator.validate(new ChangeUserStatusRequest(UserStatus.ACTIVE))).isEmpty());
	}

	private static Set<String> fields(Set<? extends ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(violation -> violation.getPropertyPath().toString())
				.collect(Collectors.toSet());
	}
}
