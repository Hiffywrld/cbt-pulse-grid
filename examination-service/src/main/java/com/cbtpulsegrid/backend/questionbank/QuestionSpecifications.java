package com.cbtpulsegrid.backend.questionbank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

final class QuestionSpecifications {

	private QuestionSpecifications() {
	}

	static Specification<Question> filteredBy(
			UUID institutionId,
			UUID subjectId,
			QuestionType type,
			QuestionDifficulty difficulty,
			QuestionStatus status,
			String search
	) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("institutionId"), institutionId));

			if (subjectId != null) {
				predicates.add(criteriaBuilder.equal(root.get("subjectId"), subjectId));
			}
			if (type != null) {
				predicates.add(criteriaBuilder.equal(root.get("type"), type));
			}
			if (difficulty != null) {
				predicates.add(criteriaBuilder.equal(root.get("difficulty"), difficulty));
			}
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.like(
						criteriaBuilder.lower(root.get("questionText")),
						pattern
				));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
