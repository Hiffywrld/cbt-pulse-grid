package com.cbtpulsegrid.backend.monitoring;

import java.security.Principal;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cbtpulsegrid.backend.examination.MonitoringExamQuery;
import com.cbtpulsegrid.backend.identity.MonitoringPrincipalQuery;
import com.cbtpulsegrid.backend.identity.MonitoringPrincipalQuery.PrincipalView;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringActor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
class MonitoringStompSecurityInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final Pattern EXAM_MONITORING_TOPIC = Pattern.compile(
			"^/topic/exams/([0-9a-fA-F-]{36})/monitoring$"
	);

	private final JwtDecoder jwtDecoder;
	private final MonitoringPrincipalQuery principalQuery;
	private final MonitoringExamQuery examQuery;
	private final MonitoringAuthorization authorization;

	MonitoringStompSecurityInterceptor(
			JwtDecoder jwtDecoder,
			MonitoringPrincipalQuery principalQuery,
			MonitoringExamQuery examQuery,
			MonitoringAuthorization authorization
	) {
		this.jwtDecoder = jwtDecoder;
		this.principalQuery = principalQuery;
		this.examQuery = examQuery;
		this.authorization = authorization;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
				message,
				StompHeaderAccessor.class
		);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			authenticate(accessor);
		}
		else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			authorizeSubscription(accessor);
		}
		else if (StompCommand.SEND.equals(accessor.getCommand())) {
			throw new AccessDeniedException("Client WebSocket messages are not allowed");
		}
		return message;
	}

	private void authenticate(StompHeaderAccessor accessor) {
		String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			throw new AuthenticationCredentialsNotFoundException(
					"A bearer access token is required for WebSocket connections"
			);
		}
		String tokenValue = authorization.substring(BEARER_PREFIX.length()).trim();
		if (tokenValue.isEmpty()) {
			throw new BadCredentialsException("The WebSocket access token is invalid");
		}

		try {
			Jwt jwt = jwtDecoder.decode(tokenValue);
			UUID userId = UUID.fromString(jwt.getSubject());
			PrincipalView principal = principalQuery.findActive(userId)
					.orElseThrow(() -> new BadCredentialsException(
							"The WebSocket principal is no longer active"
					));
			validateInstitutionClaim(jwt, principal);
			Collection<GrantedAuthority> authorities = principal.roles().stream()
					.map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
					.toList();
			accessor.setUser(new JwtAuthenticationToken(jwt, authorities, userId.toString()));
		}
		catch (JwtException | IllegalArgumentException exception) {
			throw new BadCredentialsException("The WebSocket access token is invalid", exception);
		}
	}

	private void authorizeSubscription(StompHeaderAccessor accessor) {
		Principal connectionPrincipal = accessor.getUser();
		if (!(connectionPrincipal instanceof JwtAuthenticationToken authentication)) {
			throw new AuthenticationCredentialsNotFoundException(
					"An authenticated WebSocket connection is required"
			);
		}
		Matcher destination = EXAM_MONITORING_TOPIC.matcher(
				Objects.requireNonNullElse(accessor.getDestination(), "")
		);
		if (!destination.matches()) {
			throw new AccessDeniedException("The WebSocket subscription destination is not allowed");
		}

		UUID userId = UUID.fromString(authentication.getToken().getSubject());
		PrincipalView principal = principalQuery.findActive(userId)
				.orElseThrow(() -> new AccessDeniedException(
						"The WebSocket principal is no longer active"
				));
		MonitoringActor actor = new MonitoringActor(
				principal.userId(),
				principal.institutionId(),
				principal.roles()
		);
		UUID institutionId = authorization.requireStaff(actor);
		examQuery.requireExam(institutionId, UUID.fromString(destination.group(1)));
	}

	private static void validateInstitutionClaim(Jwt jwt, PrincipalView principal) {
		String institutionClaim = jwt.getClaimAsString("institutionId");
		UUID claimedInstitution = institutionClaim == null
				? null
				: UUID.fromString(institutionClaim);
		if (!Objects.equals(claimedInstitution, principal.institutionId())) {
			throw new BadCredentialsException("The WebSocket tenant claim is no longer valid");
		}
	}
}
