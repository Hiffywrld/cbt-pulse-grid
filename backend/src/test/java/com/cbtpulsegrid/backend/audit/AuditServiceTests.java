package com.cbtpulsegrid.backend.audit;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;

import com.cbtpulsegrid.backend.audit.api.AuditActor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTests {

	private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
	private static final UUID INSTITUTION_ID = UUID.randomUUID();
	private static final UUID ACTOR_ID = UUID.randomUUID();

	@Mock
	private AuditEventRepository repository;
	@Mock
	private AuditEventQueryRepository queryRepository;

	private AuditService service;

	@BeforeEach
	void createService() {
		service = new AuditService(
				repository,
				queryRepository,
				JsonMapper.builder().build(),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void capturesAuthenticatedActorRolesWithoutSensitiveMetadata() {
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
				jwt(ACTOR_ID),
				List.of(new SimpleGrantedAuthority("ROLE_INSTITUTION_ADMIN"))
		));
		when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.record(
				INSTITUTION_ID,
				AuditAction.USER_STATUS_CHANGED,
				AuditResourceType.USER,
				UUID.randomUUID(),
				Map.of("status", "LOCKED")
		);

		ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
		verify(repository).save(event.capture());
		assertEquals(ACTOR_ID, event.getValue().getActorId());
		assertEquals("INSTITUTION_ADMIN", event.getValue().getActorRoles());
		assertEquals(NOW, event.getValue().getOccurredAt());
		assertEquals("{\"status\":\"LOCKED\"}", event.getValue().getMetadata());
		assertFalse(event.getValue().getMetadata().toLowerCase().contains("password"));
	}

	@Test
	void auditsOnlyDomainRolesWhenAuthenticationContainsFrameworkAuthorities() {
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
				jwt(ACTOR_ID),
				List.of(
						new SimpleGrantedAuthority("FACTOR_BEARER"),
						new SimpleGrantedAuthority("SCOPE_openid"),
						new SimpleGrantedAuthority("ROLE_INSTITUTION_ADMIN")
				)
		));
		when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.record(
				INSTITUTION_ID,
				AuditAction.USER_UPDATED,
				AuditResourceType.USER,
				UUID.randomUUID(),
				Map.of()
		);

		ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
		verify(repository).save(event.capture());
		assertEquals("INSTITUTION_ADMIN", event.getValue().getActorRoles());
	}

	@Test
	void rejectsSensitiveMetadataBeforePersistence() {
		assertThrows(
				IllegalArgumentException.class,
				() -> service.record(
						INSTITUTION_ID,
						AuditAction.USER_UPDATED,
						AuditResourceType.USER,
						UUID.randomUUID(),
						Map.of("passwordHash", "must-not-be-stored")
				)
		);
		verify(repository, never()).save(any());
	}

	@Test
	void repositorySurfaceHasNoMutationMethodsOtherThanInsertSave() {
		Set<String> methodNames = Arrays.stream(AuditEventRepository.class.getMethods())
				.map(java.lang.reflect.Method::getName)
				.collect(java.util.stream.Collectors.toSet());

		assertTrue(methodNames.contains("save"));
		assertFalse(methodNames.stream().anyMatch(name -> name.startsWith("delete")));
	}

	@Test
	void auditHistoryRequiresInstitutionAdministratorAndUsesTenantScope() {
		assertThrows(
				AccessDeniedException.class,
				() -> service.findEvents(
						new AuditActor(ACTOR_ID, INSTITUTION_ID, Set.of("STUDENT")),
						null, null, null, null, null, 0, 20
				)
		);
		when(queryRepository.findHistory(any(), any(), any(), any(), any(), any(),
				any(org.springframework.data.domain.Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));

		var page = service.findEvents(
				new AuditActor(ACTOR_ID, INSTITUTION_ID, Set.of("INSTITUTION_ADMIN")),
				null, null, null, null, null, 0, 20
		);

		assertEquals(0, page.totalElements());
	}

	private static Jwt jwt(UUID subject) {
		return Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(subject.toString())
				.issuedAt(NOW.minusSeconds(60))
				.expiresAt(NOW.plusSeconds(60))
				.build();
	}
}
