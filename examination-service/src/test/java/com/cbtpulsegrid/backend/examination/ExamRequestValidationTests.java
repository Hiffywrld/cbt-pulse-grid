package com.cbtpulsegrid.backend.examination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.examination.api.CreateExamRequest;
import com.cbtpulsegrid.backend.examination.api.ExamPoolRuleRequest;
import com.cbtpulsegrid.backend.examination.api.RotateExamPinRequest;
import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamRequestValidationTests {

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
	void requiresExactlySixPinDigits() {
		assertEquals(Set.of("accessPin"), fields(validator.validate(new RotateExamPinRequest("12ab56"))));
	}

	@Test
	void validatesDurationAndNestedPoolRules() {
		Instant startsAt = Instant.parse("2030-01-01T09:00:00Z");
		CreateExamRequest request = new CreateExamRequest(
				"MAT-202",
				UUID.randomUUID(),
				"Algebra Examination",
				null,
				481,
				startsAt,
				startsAt.plusSeconds(7200),
				"123456",
				false,
				false,
				List.of(new ExamPoolRuleRequest(QuestionDifficulty.EASY, 0, BigDecimal.ZERO))
		);

		Set<String> violations = fields(validator.validate(request));

		assertTrue(violations.contains("durationMinutes"));
		assertTrue(violations.contains("poolRules[0].questionCount"));
		assertTrue(violations.contains("poolRules[0].marksPerQuestion"));
	}

	@Test
	void validatesPassMarkPercentageRange() {
		Instant startsAt = Instant.parse("2030-01-01T09:00:00Z");
		CreateExamRequest request = new CreateExamRequest(
				"MAT-202",
				UUID.randomUUID(),
				"Algebra Examination",
				null,
				60,
				startsAt,
				startsAt.plusSeconds(7200),
				"123456",
				false,
				false,
				List.of(new ExamPoolRuleRequest(QuestionDifficulty.EASY, 1, BigDecimal.ONE)),
				new BigDecimal("100.01")
		);

		assertEquals(Set.of("passMarkPercentage"), fields(validator.validate(request)));
	}

	private static Set<String> fields(Set<? extends ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(violation -> violation.getPropertyPath().toString())
				.collect(Collectors.toSet());
	}
}
