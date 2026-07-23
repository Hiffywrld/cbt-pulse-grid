package com.cbtpulsegrid.backend.institution;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

final class InstitutionSpecifications {

	private InstitutionSpecifications() {
	}

	static Specification<Institution> filteredBy(String search, InstitutionStatus status) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern)
				));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
