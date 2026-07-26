package com.cbtpulsegrid.backend.audit.api;

import java.time.Instant;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditAction;
import com.cbtpulsegrid.backend.audit.AuditResourceType;
import com.cbtpulsegrid.backend.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/audit/events")
@PreAuthorize("hasRole('INSTITUTION_ADMIN')")
@Tag(name = "Audit", description = "Tenant-secured immutable administrative and security audit history")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

	private final AuditService auditService;

	public AuditController(AuditService auditService) {
		this.auditService = auditService;
	}

	@GetMapping
	@Operation(summary = "List immutable audit events for the authenticated institution")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Audit events returned"),
		@ApiResponse(responseCode = "400", description = "Filters or pagination are invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Institution administrator access is required")
	})
	public AuditPageResponse<AuditEventResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) AuditAction action,
			@RequestParam(required = false) AuditResourceType resourceType,
			@RequestParam(required = false) UUID actorId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			Instant to,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return auditService.findEvents(
				AuditActor.from(jwt),
				action,
				resourceType,
				actorId,
				from,
				to,
				page,
				size
		);
	}
}
