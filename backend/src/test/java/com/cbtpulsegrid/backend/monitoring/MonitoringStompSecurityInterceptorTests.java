package com.cbtpulsegrid.backend.monitoring;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.examination.MonitoringExamQuery;
import com.cbtpulsegrid.backend.identity.MonitoringPrincipalQuery;
import com.cbtpulsegrid.backend.identity.MonitoringPrincipalQuery.PrincipalView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringStompSecurityInterceptorTests {

	private static final UUID USER_ID = UUID.randomUUID();
	private static final UUID INSTITUTION_ID = UUID.randomUUID();
	private static final UUID EXAM_ID = UUID.randomUUID();

	@Mock
	private JwtDecoder jwtDecoder;
	@Mock
	private MonitoringPrincipalQuery principalQuery;
	@Mock
	private MonitoringExamQuery examQuery;

	private MonitoringStompSecurityInterceptor interceptor;

	@BeforeEach
	void createInterceptor() {
		interceptor = new MonitoringStompSecurityInterceptor(
				jwtDecoder,
				principalQuery,
				examQuery,
				new MonitoringAuthorization()
		);
	}

	@Test
	void acceptsValidStaffConnectionAndTenantOwnedSubscription() {
		Jwt jwt = jwt(INSTITUTION_ID, Set.of("INVIGILATOR"));
		when(jwtDecoder.decode("valid-token")).thenReturn(jwt);
		when(principalQuery.findActive(USER_ID)).thenReturn(Optional.of(principal(
				INSTITUTION_ID,
				Set.of("INVIGILATOR")
		)));

		JwtAuthenticationToken authentication = connect("Bearer valid-token");

		assertDoesNotThrow(() -> interceptor.preSend(
				subscribe(authentication, "/topic/exams/" + EXAM_ID + "/monitoring"),
				mock(MessageChannel.class)
		));
		verify(examQuery).requireExam(INSTITUTION_ID, EXAM_ID);
	}

	@Test
	void rejectsMissingInvalidAndInactiveAuthentication() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> interceptor.preSend(connectMessage(null), mock(MessageChannel.class))
		);

		when(jwtDecoder.decode("invalid")).thenThrow(new JwtException("invalid"));
		assertThrows(BadCredentialsException.class, () -> connect("Bearer invalid"));

		Jwt jwt = jwt(INSTITUTION_ID, Set.of("INVIGILATOR"));
		when(jwtDecoder.decode("revoked-user")).thenReturn(jwt);
		when(principalQuery.findActive(USER_ID)).thenReturn(Optional.empty());
		assertThrows(BadCredentialsException.class, () -> connect("Bearer revoked-user"));
	}

	@Test
	void rejectsStudentSubscription() {
		Jwt jwt = jwt(INSTITUTION_ID, Set.of("STUDENT"));
		when(jwtDecoder.decode("student-token")).thenReturn(jwt);
		when(principalQuery.findActive(USER_ID)).thenReturn(Optional.of(principal(
				INSTITUTION_ID,
				Set.of("STUDENT")
		)));
		JwtAuthenticationToken authentication = connect("Bearer student-token");

		assertThrows(
				AccessDeniedException.class,
				() -> interceptor.preSend(
						subscribe(authentication, "/topic/exams/" + EXAM_ID + "/monitoring"),
						mock(MessageChannel.class)
				)
		);
	}

	@Test
	void rejectsExaminerSubscription() {
		Jwt jwt = jwt(INSTITUTION_ID, Set.of("EXAMINER"));
		when(jwtDecoder.decode("examiner-token")).thenReturn(jwt);
		when(principalQuery.findActive(USER_ID)).thenReturn(Optional.of(principal(
				INSTITUTION_ID,
				Set.of("EXAMINER")
		)));
		JwtAuthenticationToken authentication = connect("Bearer examiner-token");

		assertThrows(
				AccessDeniedException.class,
				() -> interceptor.preSend(
						subscribe(authentication, "/topic/exams/" + EXAM_ID + "/monitoring"),
						mock(MessageChannel.class)
				)
		);
	}

	@Test
	void rejectsCrossInstitutionSubscription() {
		Jwt jwt = jwt(INSTITUTION_ID, Set.of("INVIGILATOR"));
		when(jwtDecoder.decode("staff-token")).thenReturn(jwt);
		when(principalQuery.findActive(USER_ID)).thenReturn(Optional.of(principal(
				INSTITUTION_ID,
				Set.of("INVIGILATOR")
		)));
		doThrow(new AccessDeniedException("cross tenant"))
				.when(examQuery).requireExam(INSTITUTION_ID, EXAM_ID);
		JwtAuthenticationToken authentication = connect("Bearer staff-token");

		assertThrows(
				AccessDeniedException.class,
				() -> interceptor.preSend(
						subscribe(authentication, "/topic/exams/" + EXAM_ID + "/monitoring"),
						mock(MessageChannel.class)
				)
		);
	}

	@Test
	void rejectsClientMessagesToServerOwnedBrokerDestinations() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/topic/exams/" + EXAM_ID + "/monitoring");
		accessor.setLeaveMutable(true);
		Message<byte[]> message = MessageBuilder.createMessage(
				new byte[0],
				accessor.getMessageHeaders()
		);

		assertThrows(
				AccessDeniedException.class,
				() -> interceptor.preSend(message, mock(MessageChannel.class))
		);
	}

	private JwtAuthenticationToken connect(String authorization) {
		Message<?> connected = interceptor.preSend(
				connectMessage(authorization),
				mock(MessageChannel.class)
		);
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(connected);
		return assertInstanceOf(JwtAuthenticationToken.class, accessor.getUser());
	}

	private static Message<byte[]> connectMessage(String authorization) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		if (authorization != null) {
			accessor.setNativeHeader("Authorization", authorization);
		}
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private static Message<byte[]> subscribe(
			JwtAuthenticationToken authentication,
			String destination
	) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setSubscriptionId("subscription-1");
		accessor.setDestination(destination);
		accessor.setUser(authentication);
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private static Jwt jwt(UUID institutionId, Set<String> roles) {
		return Jwt.withTokenValue("access-token")
				.header("alg", "HS256")
				.subject(USER_ID.toString())
				.issuedAt(Instant.parse("2030-01-01T00:00:00Z"))
				.expiresAt(Instant.parse("2030-01-01T00:15:00Z"))
				.claim("institutionId", institutionId.toString())
				.claim("roles", roles)
				.build();
	}

	private static PrincipalView principal(UUID institutionId, Set<String> roles) {
		return new PrincipalView(USER_ID, institutionId, roles);
	}
}
