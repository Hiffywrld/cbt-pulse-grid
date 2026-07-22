package com.cbtpulsegrid.backend.identity.auth;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class AuthExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

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

	@ExceptionHandler({
			HandlerMethodValidationException.class,
			MethodArgumentTypeMismatchException.class,
			ConstraintViolationException.class,
			HttpMessageNotReadableException.class,
			IllegalArgumentException.class
	})
	public ResponseEntity<ApiError> handleInvalidInput(
			Exception exception,
			HttpServletRequest request
	) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request,
				Map.of()
		);
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ApiError> handleNotFound(
			NoSuchElementException exception,
			HttpServletRequest request
	) {
		return response(
				HttpStatus.NOT_FOUND,
				exception.getMessage() == null ? "Resource not found" : exception.getMessage(),
				request,
				Map.of()
		);
	}

	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseEntity<ApiError> handleDuplicateKey(
			DuplicateKeyException exception,
			HttpServletRequest request
	) {
		return response(
				HttpStatus.CONFLICT,
				exception.getMessage() == null ? "Duplicate value" : exception.getMessage(),
				request,
				Map.of()
		);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleConflict(
			DataIntegrityViolationException exception,
			HttpServletRequest request
	) {
		return response(
				HttpStatus.CONFLICT,
				"A unique value already exists",
				request,
				Map.of()
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

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request
	) {
		log.error(
				"Unhandled exception for {} {}",
				request.getMethod(),
				request.getRequestURI(),
				exception
		);
		return response(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred",
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
