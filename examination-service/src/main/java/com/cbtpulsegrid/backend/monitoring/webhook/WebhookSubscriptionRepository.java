package com.cbtpulsegrid.backend.monitoring.webhook;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {

	boolean existsByInstitutionIdAndNameIgnoreCase(UUID institutionId, String name);

	Page<WebhookSubscription> findByInstitutionId(UUID institutionId, Pageable pageable);

	Optional<WebhookSubscription> findByInstitutionIdAndId(UUID institutionId, UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select subscription from WebhookSubscription subscription where subscription.id = :id")
	Optional<WebhookSubscription> findByIdForUpdate(@Param("id") UUID id);

	@Query("""
			select distinct subscription
			from WebhookSubscription subscription
			left join subscription.eventTypes eventType
			where subscription.institutionId = :institutionId
			and subscription.status = :status
			and (subscription.allEventTypes = true or eventType = :eventType)
			""")
	List<WebhookSubscription> findMatchingActive(
			@Param("institutionId") UUID institutionId,
			@Param("status") WebhookSubscriptionStatus status,
			@Param("eventType") MonitoringEventType eventType
	);
}
