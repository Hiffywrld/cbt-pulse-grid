package com.cbtpulsegrid.backend.monitoring.webhook;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringEventType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "webhook_subscriptions")
class WebhookSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "institution_id", nullable = false, updatable = false)
	private UUID institutionId;

	@Column(name = "name", nullable = false, length = 120)
	private String name;

	@Column(name = "destination_url", nullable = false, length = 2048)
	private String destinationUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private WebhookSubscriptionStatus status;

	@Column(name = "all_event_types", nullable = false)
	private boolean allEventTypes;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
			name = "webhook_subscription_event_types",
			joinColumns = @JoinColumn(name = "subscription_id")
	)
	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private Set<MonitoringEventType> eventTypes = new LinkedHashSet<>();

	@Column(name = "secret_version", nullable = false)
	private int secretVersion;

	@Column(name = "created_by", nullable = false, updatable = false)
	private UUID createdBy;

	@Column(name = "updated_by", nullable = false)
	private UUID updatedBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected WebhookSubscription() {
	}

	WebhookSubscription(
			UUID institutionId,
			String name,
			String destinationUrl,
			Set<MonitoringEventType> eventTypes,
			UUID actorId
	) {
		this.institutionId = institutionId;
		this.name = name;
		this.destinationUrl = destinationUrl;
		this.status = WebhookSubscriptionStatus.ACTIVE;
		this.allEventTypes = eventTypes == null || eventTypes.isEmpty();
		if (!this.allEventTypes) {
			this.eventTypes.addAll(eventTypes);
		}
		this.secretVersion = 1;
		this.createdBy = actorId;
		this.updatedBy = actorId;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	void changeStatus(WebhookSubscriptionStatus status, UUID actorId) {
		this.status = status;
		this.updatedBy = actorId;
	}

	int rotateSecret(UUID actorId) {
		secretVersion++;
		updatedBy = actorId;
		return secretVersion;
	}

	boolean matches(MonitoringEventType eventType) {
		return allEventTypes || eventTypes.contains(eventType);
	}

	UUID getId() {
		return id;
	}

	UUID getInstitutionId() {
		return institutionId;
	}

	String getName() {
		return name;
	}

	String getDestinationUrl() {
		return destinationUrl;
	}

	WebhookSubscriptionStatus getStatus() {
		return status;
	}

	boolean isAllEventTypes() {
		return allEventTypes;
	}

	Set<MonitoringEventType> getEventTypes() {
		return Set.copyOf(eventTypes);
	}

	int getSecretVersion() {
		return secretVersion;
	}

	UUID getCreatedBy() {
		return createdBy;
	}

	UUID getUpdatedBy() {
		return updatedBy;
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
