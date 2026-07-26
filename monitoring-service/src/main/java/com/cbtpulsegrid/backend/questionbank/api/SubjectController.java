package com.cbtpulsegrid.backend.questionbank.api;

import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.SubjectService;
import com.cbtpulsegrid.backend.questionbank.SubjectStatus;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/subjects")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'EXAMINER')")
@Tag(name = "Subjects", description = "Institutional subject administration")
@SecurityRequirement(name = "bearerAuth")
public class SubjectController {

	private final SubjectService subjectService;

	public SubjectController(SubjectService subjectService) {
		this.subjectService = subjectService;
	}

	@PostMapping({"", "/"})
	@PreAuthorize("hasRole('INSTITUTION_ADMIN')")
	@Operation(summary = "Create a subject")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Subject created"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied"),
			@ApiResponse(responseCode = "409", description = "Subject code already exists")
	})
	public ResponseEntity<SubjectResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateSubjectRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(subjectService.create(QuestionBankActor.from(jwt), request));
	}

	@GetMapping({"", "/"})
	@Operation(summary = "List and filter subjects")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Subject page returned"),
			@ApiResponse(responseCode = "400", description = "Invalid pagination or filter value"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied")
	})
	public QuestionBankPageResponse<SubjectResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) @Size(max = 150) String search,
			@RequestParam(required = false) SubjectStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return subjectService.list(QuestionBankActor.from(jwt), search, status, page, size);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a subject")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Subject returned"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied"),
			@ApiResponse(responseCode = "404", description = "Subject not found")
	})
	public SubjectResponse get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id
	) {
		return subjectService.get(QuestionBankActor.from(jwt), id);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('INSTITUTION_ADMIN')")
	@Operation(summary = "Update a subject")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Subject updated"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied"),
			@ApiResponse(responseCode = "404", description = "Subject not found"),
			@ApiResponse(responseCode = "409", description = "Subject code already exists")
	})
	public SubjectResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody UpdateSubjectRequest request
	) {
		return subjectService.update(QuestionBankActor.from(jwt), id, request);
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('INSTITUTION_ADMIN')")
	@Operation(summary = "Change a subject status")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Subject status changed"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied"),
			@ApiResponse(responseCode = "404", description = "Subject not found")
	})
	public SubjectResponse changeStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody ChangeSubjectStatusRequest request
	) {
		return subjectService.changeStatus(QuestionBankActor.from(jwt), id, request.status());
	}
}
