package com.cbtpulsegrid.backend.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
class AuditEventQueryRepository {

	private final EntityManager entityManager;

	AuditEventQueryRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	Page<AuditEvent> findHistory(
			UUID institutionId,
			AuditAction action,
			AuditResourceType resourceType,
			UUID actorId,
			Instant from,
			Instant to,
			Pageable pageable
	) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		var query = builder.createQuery(AuditEvent.class);
		Root<AuditEvent> root = query.from(AuditEvent.class);
		query.where(predicates(builder, root, institutionId, action, resourceType, actorId, from, to));
		query.orderBy(builder.desc(root.get("occurredAt")), builder.desc(root.get("id")));
		List<AuditEvent> content = entityManager.createQuery(query)
				.setFirstResult(Math.toIntExact(pageable.getOffset()))
				.setMaxResults(pageable.getPageSize())
				.getResultList();

		var countQuery = builder.createQuery(Long.class);
		Root<AuditEvent> countRoot = countQuery.from(AuditEvent.class);
		countQuery.select(builder.count(countRoot));
		countQuery.where(predicates(
				builder,
				countRoot,
				institutionId,
				action,
				resourceType,
				actorId,
				from,
				to
		));
		long total = entityManager.createQuery(countQuery).getSingleResult();
		return new PageImpl<>(content, pageable, total);
	}

	private static Predicate[] predicates(
			CriteriaBuilder builder,
			Root<AuditEvent> root,
			UUID institutionId,
			AuditAction action,
			AuditResourceType resourceType,
			UUID actorId,
			Instant from,
			Instant to
	) {
		List<Predicate> predicates = new ArrayList<>();
		predicates.add(builder.equal(root.get("institutionId"), institutionId));
		if (action != null) {
			predicates.add(builder.equal(root.get("action"), action));
		}
		if (resourceType != null) {
			predicates.add(builder.equal(root.get("resourceType"), resourceType));
		}
		if (actorId != null) {
			predicates.add(builder.equal(root.get("actorId"), actorId));
		}
		if (from != null) {
			predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), from));
		}
		if (to != null) {
			predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), to));
		}
		return predicates.toArray(Predicate[]::new);
	}
}
