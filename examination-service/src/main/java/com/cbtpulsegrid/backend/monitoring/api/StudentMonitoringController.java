package com.cbtpulsegrid.backend.monitoring.api;

import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/attempts")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Monitoring", description = "Persistent candidate heartbeat and anti-cheat evidence ingestion")
@SecurityRequirement(name = "bearerAuth")
public class StudentMonitoringController {

	private final MonitoringService monitoringService;

	public StudentMonitoringController(MonitoringService monitoringService) {
		this.monitoringService = monitoringService;
	}

	@PostMapping("/{attemptId}/heartbeat")
	@Operation(summary = "Record the latest device-locked student heartbeat")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Heartbeat acknowledged; stale state is not applied"),
		@ApiResponse(responseCode = "400", description = "Heartbeat data is invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Attempt ownership is denied"),
		@ApiResponse(responseCode = "404", description = "Attempt not found"),
		@ApiResponse(responseCode = "409", description = "Attempt is closed or locked to another device")
	})
	public HeartbeatResponse heartbeat(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID attemptId,
			@Valid @RequestBody HeartbeatRequest request
	) {
		return monitoringService.recordHeartbeat(MonitoringActor.from(jwt), attemptId, request);
	}

	@PostMapping("/{attemptId}/monitoring-events")
	@Operation(summary = "Ingest one idempotent offline monitoring-event batch")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Monitoring sync batch acknowledged"),
		@ApiResponse(responseCode = "400", description = "Event batch or metadata is invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Attempt ownership is denied"),
		@ApiResponse(responseCode = "404", description = "Attempt not found"),
		@ApiResponse(responseCode = "409", description = "Attempt is no longer active")
	})
	public MonitoringEventBatchResponse events(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID attemptId,
			@Valid @RequestBody MonitoringEventBatchRequest request
	) {
		return monitoringService.recordEvents(MonitoringActor.from(jwt), attemptId, request);
	}
}
