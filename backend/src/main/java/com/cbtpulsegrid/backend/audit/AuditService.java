package com.cbtpulsegrid.backend.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.RequestCorrelation;
import com.cbtpulsegrid.backend.audit.api.AuditActor;
import com.cbtpulsegrid.backend.audit.api.AuditEventResponse;
import com.cbtpulsegrid.backend.audit.api.AuditPageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditService implements AuditTrail {

	private static final int MAX_PAGE_SIZE = 100;
	private static final int MAX_METADATA_ENTRIES = 20;
	private static final int MAX_METADATA_VALUE_LENGTH = 256;
	private static final int MAX_METADATA_LENGTH = 2000;
	// Mirrors the deliberately small identity.Role model without introducing an audit -> identity
	// module dependency (identity already records actions through AuditTrail).
	private static final Set<String> DOMAIN_ROLE_AUTHORITIES = Set.of(
			"ROLE_SUPER_ADMIN",
			"ROLE_INSTITUTION_ADMIN",
			"ROLE_EXAMINER",
			"ROLE_INVIGILATOR",
			"ROLE_STUDENT"
	);
	private static final Set<String> FORBIDDEN_METADATA_TERMS = Set.of(
			"password", "token", "secret", "pin", "device", "correct", "answer", "jwt", "key"
	);

	private final AuditEventRepository repository;
	private final AuditEventQueryRepository queryRepository;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public AuditService(
			AuditEventRepository repository,
			AuditEventQueryRepository queryRepository,
			ObjectMapper objectMapper,
			Clock clock
	) {
		this.repository = repository;
		this.queryRepository = queryRepository;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void record(
			UUID institutionId,
			AuditAction action,
			AuditResourceType resourceType,
			UUID resourceId,
			Map<String, ?> metadata
	) {
		ActorSnapshot actor = currentActor();
		repository.save(new AuditEvent(
				institutionId,
				actor.id(),
				actor.roles(),
				action,
				resourceType,
				resourceId,
				AuditOutcome.SUCCESS,
				clock.instant(),
				RequestCorrelation.currentId().orElse(null),
				serializeMetadata(metadata)
		));
	}

	@Transactional(readOnly = true)
	public AuditPageResponse<AuditEventResponse> findEvents(
			AuditActor actor,
			AuditAction action,
			AuditResourceType resourceType,
			UUID actorId,
			Instant from,
			Instant to,
			int page,
			int size
	) {
		if (actor == null || !actor.isInstitutionAdministrator()) {
			throw new AccessDeniedException("Institution administrator access is required");
		}
		if (from != null && to != null && from.isAfter(to)) {
			throw new IllegalArgumentException("from must not be after to");
		}
		validatePage(page, size);
		var pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))
		);
		return AuditPageResponse.from(queryRepository.findHistory(
				actor.institutionId(),
				action,
				resourceType,
				actorId,
				from,
				to,
				pageable
		).map(this::toResponse));
	}

	private AuditEventResponse toResponse(AuditEvent event) {
		return new AuditEventResponse(
				event.getId(),
				event.getInstitutionId(),
				event.getActorId(),
				event.getActorRoles(),
				event.getAction(),
				event.getResourceType(),
				event.getResourceId(),
				event.getOutcome(),
				event.getOccurredAt(),
				event.getRequestId(),
				parseMetadata(event.getMetadata())
		);
	}

	private String serializeMetadata(Map<String, ?> metadata) {
		Map<String, Object> safe = new TreeMap<>();
		if (metadata != null) {
			if (metadata.size() > MAX_METADATA_ENTRIES) {
				throw new IllegalArgumentException("Audit metadata contains too many entries");
			}
			metadata.forEach((key, value) -> safe.put(validateKey(key), sanitizeValue(value)));
		}
		try {
			String json = objectMapper.writeValueAsString(safe);
			if (json.length() > MAX_METADATA_LENGTH) {
				throw new IllegalArgumentException("Audit metadata is too large");
			}
			return json;
		}
		catch (JacksonException exception) {
			throw new IllegalArgumentException("Audit metadata is invalid", exception);
		}
	}

	private Map<String, Object> parseMetadata(String metadata) {
		try {
			return objectMapper.readValue(metadata, new TypeReference<LinkedHashMap<String, Object>>() { });
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("Stored audit metadata is invalid", exception);
		}
	}

	private static String validateKey(String key) {
		if (key == null || key.isBlank() || key.length() > 80) {
			throw new IllegalArgumentException("Audit metadata key is invalid");
		}
		String normalized = key.toLowerCase(Locale.ROOT);
		if (FORBIDDEN_METADATA_TERMS.stream().anyMatch(normalized::contains)) {
			throw new IllegalArgumentException("Sensitive audit metadata is not permitted");
		}
		return key;
	}

	private static Object sanitizeValue(Object value) {
		if (value == null || value instanceof Number || value instanceof Boolean) {
			return value;
		}
		if (value instanceof UUID || value instanceof Instant || value instanceof Enum<?>) {
			return value.toString();
		}
		if (value instanceof Collection<?> values) {
			if (values.size() > 20) {
				throw new IllegalArgumentException("Audit metadata collection is too large");
			}
			return values.stream().map(AuditService::sanitizeValue).toList();
		}
		String text = value.toString().replaceAll("[\\r\\n\\t]", " ").trim();
		return text.length() <= MAX_METADATA_VALUE_LENGTH
				? text
				: text.substring(0, MAX_METADATA_VALUE_LENGTH);
	}

	private static ActorSnapshot currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return new ActorSnapshot(null, "SYSTEM");
		}
		UUID actorId = null;
		if (authentication.getPrincipal() instanceof Jwt jwt) {
			try {
				actorId = UUID.fromString(jwt.getSubject());
			}
			catch (IllegalArgumentException ignored) {
				actorId = null;
			}
		}
		String roles = authentication.getAuthorities().stream()
				.map(org.springframework.security.core.GrantedAuthority::getAuthority)
				.filter(DOMAIN_ROLE_AUTHORITIES::contains)
				.map(authority -> authority.substring("ROLE_".length()))
				.sorted()
				.collect(Collectors.joining(","));
		return new ActorSnapshot(actorId, roles.isBlank() ? "SYSTEM" : roles);
	}

	private static void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Audit pagination is invalid");
		}
	}

	private record ActorSnapshot(UUID id, String roles) {
	}
}
