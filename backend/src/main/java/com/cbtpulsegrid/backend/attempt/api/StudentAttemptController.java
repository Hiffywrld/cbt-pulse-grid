package com.cbtpulsegrid.backend.attempt.api;

import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.attempt.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Attempts", description = "Candidate-safe exam delivery, offline autosave and submission")
@SecurityRequirement(name = "bearerAuth")
public class StudentAttemptController {

	private final AttemptService attemptService;

	public StudentAttemptController(AttemptService attemptService) {
		this.attemptService = attemptService;
	}

	@GetMapping("/exams")
	@Operation(summary = "List exams assigned to the authenticated student")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Assigned exams returned with availability"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Student access is denied")
	})
	public List<StudentExamSummaryResponse> listExams(@AuthenticationPrincipal Jwt jwt) {
		return attemptService.listAssignedExams(StudentActor.from(jwt));
	}

	@GetMapping("/exams/{examId}")
	@Operation(summary = "Get candidate-safe assigned exam details")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Assigned exam returned"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Student or assignment access is denied"),
		@ApiResponse(responseCode = "404", description = "Assigned exam not found")
	})
	public StudentExamDetailResponse getExam(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID examId
	) {
		return attemptService.getAssignedExam(StudentActor.from(jwt), examId);
	}

	@PostMapping("/exams/{examId}/attempts")
	@Operation(summary = "Start or resume an exam attempt on the locked device")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Attempt started or resumed"),
		@ApiResponse(responseCode = "400", description = "Exam window or access credentials are invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Student or assignment access is denied"),
		@ApiResponse(responseCode = "409", description = "Attempt is submitted, expired, or locked to another device")
	})
	public AttemptPackageResponse startAttempt(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID examId,
			@Valid @RequestBody StartAttemptRequest request
	) {
		return attemptService.startOrResume(StudentActor.from(jwt), examId, request);
	}

	@GetMapping("/attempts/{attemptId}")
	@Operation(summary = "Get a resumable candidate-safe attempt package")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Attempt package returned"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Attempt ownership is denied"),
		@ApiResponse(responseCode = "404", description = "Attempt not found")
	})
	public AttemptPackageResponse getAttempt(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID attemptId
	) {
		return attemptService.getAttempt(StudentActor.from(jwt), attemptId);
	}

	@PutMapping("/attempts/{attemptId}/answers")
	@Operation(summary = "Apply one idempotent offline answer-sync batch")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Sync batch acknowledged"),
		@ApiResponse(responseCode = "400", description = "Answer or option ownership is invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Attempt ownership is denied"),
		@ApiResponse(responseCode = "409", description = "Attempt is expired or already submitted")
	})
	public SyncAnswersResponse syncAnswers(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID attemptId,
			@Valid @RequestBody SyncAnswersRequest request
	) {
		return attemptService.syncAnswers(StudentActor.from(jwt), attemptId, request);
	}

	@PostMapping("/attempts/{attemptId}/submit")
	@Operation(summary = "Submit and immediately score an attempt")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Saved result returned idempotently"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Attempt ownership is denied"),
		@ApiResponse(responseCode = "404", description = "Attempt not found")
	})
	public AttemptResultResponse submitAttempt(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID attemptId
	) {
		return attemptService.submit(StudentActor.from(jwt), attemptId);
	}

	@GetMapping("/attempts/{attemptId}/result")
	@Operation(summary = "Get the submission status and persisted attempt result")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Submission status and result returned"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Attempt ownership is denied"),
		@ApiResponse(responseCode = "404", description = "Attempt not found")
	})
	public AttemptResultResponse getResult(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID attemptId
	) {
		return attemptService.getResult(StudentActor.from(jwt), attemptId);
	}
}
