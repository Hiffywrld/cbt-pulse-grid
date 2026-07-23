package com.cbtpulsegrid.backend;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

	private static final int UUID_TEXT_LENGTH = 36;

	private final ObjectMapper objectMapper;

	public RequestCorrelationFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		UUID requestId = UUID.randomUUID();
		String supplied = request.getHeader(RequestCorrelation.HEADER_NAME);
		boolean multipleValues = Collections.list(
				request.getHeaders(RequestCorrelation.HEADER_NAME)
		).size() > 1;
		if (supplied != null) {
			try {
				if (multipleValues || supplied.length() != UUID_TEXT_LENGTH) {
					throw new IllegalArgumentException("Invalid request ID");
				}
				UUID parsed = UUID.fromString(supplied);
				if (!parsed.toString().equalsIgnoreCase(supplied)) {
					throw new IllegalArgumentException("Invalid request ID");
				}
				requestId = parsed;
			}
			catch (IllegalArgumentException exception) {
				install(request, response, requestId);
				try {
					writeInvalidRequestId(request, response, requestId);
				}
				finally {
					clear();
				}
				return;
			}
		}

		install(request, response, requestId);
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			clear();
		}
	}

	private static void install(
			HttpServletRequest request,
			HttpServletResponse response,
			UUID requestId
	) {
		request.setAttribute(RequestCorrelation.REQUEST_ATTRIBUTE, requestId);
		response.setHeader(RequestCorrelation.HEADER_NAME, requestId.toString());
		RequestCorrelation.set(requestId);
		MDC.put(RequestCorrelation.MDC_KEY, requestId.toString());
	}

	private static void clear() {
		MDC.remove(RequestCorrelation.MDC_KEY);
		RequestCorrelation.clear();
	}

	private void writeInvalidRequestId(
			HttpServletRequest request,
			HttpServletResponse response,
			UUID requestId
	) throws IOException {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", HttpServletResponse.SC_BAD_REQUEST);
		body.put("error", "Bad Request");
		body.put("message", "X-Request-Id must contain one canonical UUID");
		body.put("path", request.getRequestURI());
		body.put("requestId", requestId);
		body.put("validationErrors", Map.of());
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
