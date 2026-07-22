package com.cbtpulsegrid.backend.identity.auth;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		Map<String, String> validationErrors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error -> validationErrors.putIfAbsent(
				error.getField(),
				error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()
		));
		return response(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request,
				validationErrors
		);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleBadCredentials(
			AuthenticationException exception,
			HttpServletRequest request
	) {
		return response(
				HttpStatus.UNAUTHORIZED,
				"Invalid email or password",
				request,
				Map.of()
		);
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ApiError> handleInvalidRefreshToken(
			InvalidRefreshTokenException exception,
			HttpServletRequest request
	) {
		return response(
				HttpStatus.UNAUTHORIZED,
				"Invalid refresh token",
				request,
				Map.of()
		);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(
			AccessDeniedException exception,
			HttpServletRequest request
	) {
		return response(
				HttpStatus.FORBIDDEN,
				"Access is denied",
				request,
				Map.of()
		);
	}

	private static ResponseEntity<ApiError> response(
			HttpStatus status,
			String message,
			HttpServletRequest request,
			Map<String, String> validationErrors
	) {
		ApiError error = new ApiError(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI(),
				validationErrors
		);
		return ResponseEntity.status(status).body(error);
	}
}
