package com.cbtpulsegrid.backend.identity.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

final class UserSpecifications {

	private UserSpecifications() {
	}

	static Specification<User> filteredBy(
			String search,
			UUID institutionId,
			Role role,
			UserStatus status
	) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (institutionId != null) {
				predicates.add(criteriaBuilder.equal(root.get("institutionId"), institutionId));
			}
			if (role != null) {
				predicates.add(criteriaBuilder.isMember(role, root.get("roles")));
			}
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (search != null && !search.isBlank()) {
				String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
				Expression<String> fullName = criteriaBuilder.concat(
						criteriaBuilder.concat(root.get("firstName"), " "),
						root.get("lastName")
				);
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(fullName), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("registrationNumber")), pattern)
				));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
