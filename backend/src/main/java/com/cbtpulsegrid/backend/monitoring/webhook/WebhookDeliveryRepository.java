package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

	boolean existsBySubscriptionIdAndMonitoringEventId(UUID subscriptionId, UUID monitoringEventId);

	Page<WebhookDelivery> findByInstitutionId(UUID institutionId, Pageable pageable);

	Page<WebhookDelivery> findByInstitutionIdAndStatus(
			UUID institutionId,
			WebhookDeliveryStatus status,
			Pageable pageable
	);

	Optional<WebhookDelivery> findByInstitutionIdAndId(UUID institutionId, UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select delivery from WebhookDelivery delivery where delivery.id = :id")
	Optional<WebhookDelivery> findByIdForUpdate(@Param("id") UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select delivery
			from WebhookDelivery delivery
			where delivery.id = :id
			and delivery.leaseOwner = :leaseOwner
			and delivery.status = :status
			""")
	Optional<WebhookDelivery> findClaimForUpdate(
			@Param("id") UUID id,
			@Param("leaseOwner") UUID leaseOwner,
			@Param("status") WebhookDeliveryStatus status
	);

	@Query(value = """
			select delivery.*
			from webhook_deliveries delivery
			join webhook_subscriptions subscription
			  on subscription.id = delivery.subscription_id
			where subscription.status = 'ACTIVE'
			and (
			  (delivery.status = 'PENDING' and delivery.next_attempt_at <= :now)
			  or (delivery.status = 'IN_FLIGHT' and delivery.lease_expires_at <= :now)
			)
			order by delivery.next_attempt_at, delivery.id
			limit :batchSize
			for update of delivery skip locked
			""", nativeQuery = true)
	List<WebhookDelivery> findDueForUpdate(
			@Param("now") Instant now,
			@Param("batchSize") int batchSize
	);
}
