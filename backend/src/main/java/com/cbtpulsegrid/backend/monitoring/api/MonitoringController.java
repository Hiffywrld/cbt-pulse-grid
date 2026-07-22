package com.cbtpulsegrid.backend.monitoring.api;

import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/monitoring")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'EXAMINER', 'INVIGILATOR')")
@Tag(name = "Exam Monitoring", description = "Tenant-scoped invigilation dashboards and persisted event history")
@SecurityRequirement(name = "bearerAuth")
public class MonitoringController {

	private final MonitoringService monitoringService;

	public MonitoringController(MonitoringService monitoringService) {
		this.monitoringService = monitoringService;
	}

	@GetMapping("/exams/{examId}/dashboard")
	@Operation(summary = "Get a paginated live-state dashboard for an institution exam")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Monitoring dashboard page returned"),
		@ApiResponse(responseCode = "400", description = "Pagination is invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
		@ApiResponse(responseCode = "404", description = "Exam not found")
	})
	public MonitoringPageResponse<MonitoringDashboardResponse> dashboard(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID examId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return monitoringService.dashboard(MonitoringActor.from(jwt), examId, page, size);
	}

	@GetMapping("/attempts/{attemptId}/events")
	@Operation(summary = "Get paginated immutable monitoring events for an attempt")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Monitoring event page returned"),
		@ApiResponse(responseCode = "400", description = "Pagination is invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
		@ApiResponse(responseCode = "404", description = "Attempt not found")
	})
	public MonitoringPageResponse<MonitoringEventResponse> events(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID attemptId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return monitoringService.events(MonitoringActor.from(jwt), attemptId, page, size);
	}
}
