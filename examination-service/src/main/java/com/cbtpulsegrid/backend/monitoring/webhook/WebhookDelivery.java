package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "webhook_deliveries",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_webhook_deliveries_subscription_event",
				columnNames = {"subscription_id", "monitoring_event_id"}
		)
)
class WebhookDelivery {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "institution_id", nullable = false, updatable = false)
	private UUID institutionId;

	@Column(name = "subscription_id", nullable = false, updatable = false)
	private UUID subscriptionId;

	@Column(name = "monitoring_event_id", nullable = false, updatable = false)
	private UUID monitoringEventId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40, updatable = false)
	private MonitoringEventType eventType;

	@Column(name = "secret_version", nullable = false, updatable = false)
	private int secretVersion;

	@Column(name = "payload_body", nullable = false, updatable = false)
	private byte[] payloadBody;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private WebhookDeliveryStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "next_attempt_at", nullable = false)
	private Instant nextAttemptAt;

	@Column(name = "lease_owner")
	private UUID leaseOwner;

	@Column(name = "lease_expires_at")
	private Instant leaseExpiresAt;

	@Column(name = "response_status")
	private Integer responseStatus;

	@Column(name = "failure_reason", length = 500)
	private String failureReason;

	@Column(name = "delivered_at")
	private Instant deliveredAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected WebhookDelivery() {
	}

	WebhookDelivery(
			UUID institutionId,
			UUID subscriptionId,
			UUID monitoringEventId,
			MonitoringEventType eventType,
			int secretVersion,
			byte[] payloadBody,
			Instant createdAt
	) {
		this.institutionId = institutionId;
		this.subscriptionId = subscriptionId;
		this.monitoringEventId = monitoringEventId;
		this.eventType = eventType;
		this.secretVersion = secretVersion;
		this.payloadBody = payloadBody.clone();
		this.status = WebhookDeliveryStatus.PENDING;
		this.nextAttemptAt = createdAt;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	void claim(UUID owner, Instant now, Duration leaseDuration) {
		status = WebhookDeliveryStatus.IN_FLIGHT;
		leaseOwner = owner;
		leaseExpiresAt = now.plus(leaseDuration);
		attemptCount++;
		responseStatus = null;
		failureReason = null;
	}

	void succeed(Instant completedAt, int responseStatus) {
		status = WebhookDeliveryStatus.SUCCEEDED;
		deliveredAt = completedAt;
		this.responseStatus = responseStatus;
		failureReason = null;
		clearLease();
	}

	void retryAt(Instant nextAttemptAt, Integer responseStatus, String failureReason) {
		status = WebhookDeliveryStatus.PENDING;
		this.nextAttemptAt = nextAttemptAt;
		this.responseStatus = responseStatus;
		this.failureReason = failureReason;
		clearLease();
	}

	void fail(Integer responseStatus, String failureReason) {
		status = WebhookDeliveryStatus.FAILED;
		this.responseStatus = responseStatus;
		this.failureReason = failureReason;
		clearLease();
	}

	void deadLetter(Integer responseStatus, String failureReason) {
		status = WebhookDeliveryStatus.DEAD_LETTER;
		this.responseStatus = responseStatus;
		this.failureReason = failureReason;
		clearLease();
	}

	void manualRetry(Instant now) {
		if (status != WebhookDeliveryStatus.FAILED
				&& status != WebhookDeliveryStatus.DEAD_LETTER) {
			throw new IllegalArgumentException("Only failed webhook deliveries may be retried");
		}
		status = WebhookDeliveryStatus.PENDING;
		attemptCount = 0;
		nextAttemptAt = now;
		responseStatus = null;
		failureReason = null;
		deliveredAt = null;
		clearLease();
	}

	void releasePausedClaim(Instant now) {
		status = WebhookDeliveryStatus.PENDING;
		attemptCount = Math.max(0, attemptCount - 1);
		nextAttemptAt = now;
		clearLease();
	}

	private void clearLease() {
		leaseOwner = null;
		leaseExpiresAt = null;
	}

	UUID getId() {
		return id;
	}

	UUID getInstitutionId() {
		return institutionId;
	}

	UUID getSubscriptionId() {
		return subscriptionId;
	}

	UUID getMonitoringEventId() {
		return monitoringEventId;
	}

	MonitoringEventType getEventType() {
		return eventType;
	}

	int getSecretVersion() {
		return secretVersion;
	}

	byte[] getPayloadBody() {
		return payloadBody.clone();
	}

	WebhookDeliveryStatus getStatus() {
		return status;
	}

	int getAttemptCount() {
		return attemptCount;
	}

	Instant getNextAttemptAt() {
		return nextAttemptAt;
	}

	UUID getLeaseOwner() {
		return leaseOwner;
	}

	Instant getLeaseExpiresAt() {
		return leaseExpiresAt;
	}

	Integer getResponseStatus() {
		return responseStatus;
	}

	String getFailureReason() {
		return failureReason;
	}

	Instant getDeliveredAt() {
		return deliveredAt;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	long getVersion() {
		return version;
	}
}
