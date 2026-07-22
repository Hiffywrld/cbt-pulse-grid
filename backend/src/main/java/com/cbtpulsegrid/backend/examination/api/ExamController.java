package com.cbtpulsegrid.backend.examination.api;

import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.examination.ExamService;
import com.cbtpulsegrid.backend.examination.ExamStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/exams")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'EXAMINER', 'INVIGILATOR')")
@Tag(name = "Exams", description = "Institutional examination configuration and publication")
@SecurityRequirement(name = "bearerAuth")
public class ExamController {

	private static final String MANAGER_ROLES = "hasAnyRole('INSTITUTION_ADMIN', 'EXAMINER')";

	private final ExamService examService;

	public ExamController(ExamService examService) {
		this.examService = examService;
	}

	@PostMapping({"", "/"})
	@PreAuthorize(MANAGER_ROLES)
	@Operation(summary = "Create a draft exam")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Draft exam created"),
			@ApiResponse(responseCode = "400", description = "Exam definition is invalid"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Subject not found"),
			@ApiResponse(responseCode = "409", description = "Exam code already exists")
	})
	public ResponseEntity<ExamDetailResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateExamRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(examService.create(ExamActor.from(jwt), request));
	}

	@GetMapping({"", "/"})
	@Operation(summary = "List and filter staff-visible exams")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Exam summary page returned"),
			@ApiResponse(responseCode = "400", description = "Pagination or filter is invalid"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Filtered subject not found")
	})
	public ExamPageResponse<ExamSummaryResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) @Size(max = 200) String search,
			@RequestParam(required = false) UUID subjectId,
			@RequestParam(required = false) ExamStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return examService.list(ExamActor.from(jwt), search, subjectId, status, page, size);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get staff-visible exam details")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Exam details returned"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role, status, or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Exam not found")
	})
	public ExamDetailResponse get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id
	) {
		return examService.get(ExamActor.from(jwt), id);
	}

	@PutMapping("/{id}")
	@PreAuthorize(MANAGER_ROLES)
	@Operation(summary = "Update a draft exam and replace its pool rules")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Draft exam updated"),
			@ApiResponse(responseCode = "400", description = "Exam is not draft or definition is invalid"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Exam or subject not found"),
			@ApiResponse(responseCode = "409", description = "Exam code already exists")
	})
	public ExamDetailResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody UpdateExamRequest request
	) {
		return examService.update(ExamActor.from(jwt), id, request);
	}

	@PostMapping("/{id}/publish")
	@PreAuthorize(MANAGER_ROLES)
	@Operation(summary = "Publish a validated draft exam")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Exam published"),
			@ApiResponse(responseCode = "400", description = "Publication prerequisites are not satisfied"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Exam or subject not found")
	})
	public ExamDetailResponse publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return examService.publish(ExamActor.from(jwt), id);
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize(MANAGER_ROLES)
	@Operation(summary = "Cancel a draft or published exam")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Exam cancelled"),
			@ApiResponse(responseCode = "400", description = "Exam cannot transition to cancelled"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Exam not found")
	})
	public ExamDetailResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return examService.cancel(ExamActor.from(jwt), id);
	}

	@PostMapping("/{id}/close")
	@PreAuthorize(MANAGER_ROLES)
	@Operation(summary = "Close a published exam")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Exam closed"),
			@ApiResponse(responseCode = "400", description = "Exam cannot transition to closed"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Exam not found")
	})
	public ExamDetailResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return examService.close(ExamActor.from(jwt), id);
	}

	@PutMapping("/{id}/access-pin")
	@PreAuthorize(MANAGER_ROLES)
	@Operation(summary = "Rotate a draft exam access PIN")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Access PIN rotated"),
			@ApiResponse(responseCode = "400", description = "Exam is not draft or PIN is invalid"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Exam not found")
	})
	public ExamDetailResponse rotateAccessPin(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody RotateExamPinRequest request
	) {
		return examService.rotateAccessPin(ExamActor.from(jwt), id, request.accessPin());
	}

	@PostMapping("/{id}/candidates")
	@PreAuthorize(MANAGER_ROLES)
	@Operation(summary = "Assign a batch of candidates to a draft exam")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Candidates assigned"),
			@ApiResponse(responseCode = "400", description = "Candidate batch or exam state is invalid"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Exam or candidate not found"),
			@ApiResponse(responseCode = "409", description = "Candidate is already assigned")
	})
	public ResponseEntity<List<ExamCandidateResponse>> assignCandidates(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody AssignExamCandidatesRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(examService.assignCandidates(ExamActor.from(jwt), id, request));
	}

	@GetMapping("/{id}/candidates")
	@Operation(summary = "List candidates assigned to a staff-visible exam")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Candidate-assignment page returned"),
			@ApiResponse(responseCode = "400", description = "Pagination is invalid"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role, status, or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Exam not found")
	})
	public ExamPageResponse<ExamCandidateResponse> listCandidates(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return examService.listCandidates(ExamActor.from(jwt), id, page, size);
	}

	@DeleteMapping("/{id}/candidates/{userId}")
	@PreAuthorize(MANAGER_ROLES)
	@Operation(summary = "Remove a candidate from a draft exam")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Candidate assignment removed"),
			@ApiResponse(responseCode = "400", description = "Exam is not draft"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Role or institution access is denied"),
			@ApiResponse(responseCode = "404", description = "Exam or candidate assignment not found")
	})
	public ResponseEntity<Void> removeCandidate(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@PathVariable UUID userId
	) {
		examService.removeCandidate(ExamActor.from(jwt), id, userId);
		return ResponseEntity.noContent().build();
	}
}
