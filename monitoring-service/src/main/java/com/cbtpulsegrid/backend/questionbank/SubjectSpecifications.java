package com.cbtpulsegrid.backend.questionbank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

final class SubjectSpecifications {

	private SubjectSpecifications() {
	}

	static Specification<Subject> filteredBy(UUID institutionId, SubjectStatus status, String search) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("institutionId"), institutionId));
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern)
				));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
