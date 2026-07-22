package com.cbtpulsegrid.backend.identity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "users",
		uniqueConstraints = {
				@UniqueConstraint(name = "uq_users_email", columnNames = "email"),
				@UniqueConstraint(
						name = "uq_users_institution_registration",
						columnNames = {"institution_id", "registration_number"}
				)
		}
)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "institution_id")
	private UUID institutionId;

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 80)
	private String lastName;

	@Column(name = "email", nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "registration_number", length = 64)
	private String registrationNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 32)
	private Set<Role> roles = new HashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected User() {
	}

	public User(String firstName, String lastName, String email, String passwordHash, UserStatus status) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.status = status;
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

	public UUID getId() {
		return id;
	}

	public UUID getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(UUID institutionId) {
		this.institutionId = institutionId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public long getVersion() {
		return version;
	}
}
