package com.cbtpulsegrid.backend.questionbank;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class QuestionStructureValidator {

	public void validate(QuestionType type, BigDecimal marks, List<OptionRule> options) {
		if (marks == null || marks.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("marks must be greater than zero");
		}
		if (type == null) {
			throw new IllegalArgumentException("question type is required");
		}
		if (options == null || options.size() < 2) {
			throw new IllegalArgumentException("at least two options are required");
		}

		Set<Integer> displayOrders = new HashSet<>();
		for (OptionRule option : options) {
			if (!displayOrders.add(option.displayOrder())) {
				throw new IllegalArgumentException("option display orders cannot repeat");
			}
		}

		long correctOptions = options.stream().filter(OptionRule::correct).count();
		switch (type) {
			case SINGLE_CHOICE -> {
				if (correctOptions != 1) {
					throw new IllegalArgumentException("SINGLE_CHOICE requires exactly one correct option");
				}
			}
			case MULTIPLE_CHOICE -> {
				if (correctOptions < 2) {
					throw new IllegalArgumentException("MULTIPLE_CHOICE requires at least two correct options");
				}
			}
			case TRUE_FALSE -> {
				if (options.size() != 2 || correctOptions != 1) {
					throw new IllegalArgumentException(
							"TRUE_FALSE requires exactly two options and exactly one correct option"
					);
				}
			}
		}
	}

	public record OptionRule(boolean correct, int displayOrder) {
	}
}
