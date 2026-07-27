package com.cbtpulsegrid.backend.questionbank;

import java.math.BigDecimal;
import java.util.List;

import com.cbtpulsegrid.backend.questionbank.QuestionStructureValidator.OptionRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionStructureValidatorTests {

	private final QuestionStructureValidator validator = new QuestionStructureValidator();

	@Test
	void acceptsValidQuestionTypes() {
		assertDoesNotThrow(() -> validator.validate(
				QuestionType.SINGLE_CHOICE,
				BigDecimal.ONE,
				List.of(new OptionRule(true, 1), new OptionRule(false, 2))
		));
		assertDoesNotThrow(() -> validator.validate(
				QuestionType.MULTIPLE_CHOICE,
				BigDecimal.ONE,
				List.of(new OptionRule(true, 1), new OptionRule(true, 2), new OptionRule(false, 3))
		));
		assertDoesNotThrow(() -> validator.validate(
				QuestionType.TRUE_FALSE,
				BigDecimal.ONE,
				List.of(new OptionRule(true, 1), new OptionRule(false, 2))
		));
	}

	@Test
	void enforcesCorrectAnswerCountsForEveryQuestionType() {
		assertThrows(IllegalArgumentException.class, () -> validator.validate(
				QuestionType.SINGLE_CHOICE,
				BigDecimal.ONE,
				List.of(new OptionRule(true, 1), new OptionRule(true, 2))
		));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(
				QuestionType.MULTIPLE_CHOICE,
				BigDecimal.ONE,
				List.of(new OptionRule(true, 1), new OptionRule(false, 2))
		));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(
				QuestionType.TRUE_FALSE,
				BigDecimal.ONE,
				List.of(new OptionRule(true, 1), new OptionRule(false, 2), new OptionRule(false, 3))
		));
	}

	@Test
	void rejectsRepeatedDisplayOrdersAndNonPositiveMarks() {
		assertThrows(IllegalArgumentException.class, () -> validator.validate(
				QuestionType.SINGLE_CHOICE,
				BigDecimal.ONE,
				List.of(new OptionRule(true, 1), new OptionRule(false, 1))
		));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(
				QuestionType.SINGLE_CHOICE,
				BigDecimal.ZERO,
				List.of(new OptionRule(true, 1), new OptionRule(false, 2))
		));
	}
}
