package com.cbtpulsegrid.backend.institution;

import java.util.Set;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.institution.api.ChangeInstitutionStatusRequest;
import com.cbtpulsegrid.backend.institution.api.CreateInstitutionRequest;
import com.cbtpulsegrid.backend.institution.api.UpdateInstitutionRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstitutionRequestValidationTests {

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
	void rejectsBlankCreationValues() {
		Set<String> fields = fields(validator.validate(new CreateInstitutionRequest(" ", " ")));

		assertEquals(Set.of("name", "code"), fields);
	}

	@Test
	void rejectsAnOversizedName() {
		Set<String> fields = fields(validator.validate(new UpdateInstitutionRequest("x".repeat(161))));

		assertEquals(Set.of("name"), fields);
	}

	@Test
	void requiresAStatusValue() {
		assertTrue(fields(validator.validate(new ChangeInstitutionStatusRequest(null))).contains("status"));
	}

	private static Set<String> fields(Set<? extends ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(violation -> violation.getPropertyPath().toString())
				.collect(Collectors.toSet());
	}
}
