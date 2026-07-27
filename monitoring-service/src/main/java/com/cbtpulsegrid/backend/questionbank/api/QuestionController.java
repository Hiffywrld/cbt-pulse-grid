package com.cbtpulsegrid.backend.questionbank.api;

import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionDifficulty;
import com.cbtpulsegrid.backend.questionbank.QuestionService;
import com.cbtpulsegrid.backend.questionbank.QuestionStatus;
import com.cbtpulsegrid.backend.questionbank.QuestionType;
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
@RequestMapping("/api/v1/questions")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'EXAMINER')")
@Tag(name = "Question Bank", description = "Staff-only question authoring and lifecycle operations")
@SecurityRequirement(name = "bearerAuth")
public class QuestionController {

	private final QuestionService questionService;

	public QuestionController(QuestionService questionService) {
		this.questionService = questionService;
	}

	@PostMapping({"", "/"})
	@Operation(summary = "Create a draft question with options")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Draft question created"),
			@ApiResponse(responseCode = "400", description = "Question structure or request is invalid"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied"),
			@ApiResponse(responseCode = "404", description = "Subject not found")
	})
	public ResponseEntity<StaffQuestionResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateQuestionRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(questionService.create(QuestionBankActor.from(jwt), request));
	}

	@GetMapping({"", "/"})
	@Operation(summary = "List and filter questions")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Question page returned"),
			@ApiResponse(responseCode = "400", description = "Invalid pagination or filter value"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied"),
			@ApiResponse(responseCode = "404", description = "Filtered subject not found")
	})
	public QuestionBankPageResponse<StaffQuestionResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) UUID subjectId,
			@RequestParam(required = false) QuestionType type,
			@RequestParam(required = false) QuestionDifficulty difficulty,
			@RequestParam(required = false) QuestionStatus status,
			@RequestParam(required = false) @Size(max = 500) String search,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return questionService.list(
				QuestionBankActor.from(jwt),
				subjectId,
				type,
				difficulty,
				status,
				search,
				page,
				size
		);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a staff question with correct-answer flags")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Staff question returned"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied"),
			@ApiResponse(responseCode = "404", description = "Question not found")
	})
	public StaffQuestionResponse get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id
	) {
		return questionService.get(QuestionBankActor.from(jwt), id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Replace question content and options")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Question and options updated atomically"),
			@ApiResponse(responseCode = "400", description = "Question structure or request is invalid"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied"),
			@ApiResponse(responseCode = "404", description = "Question or subject not found")
	})
	public StaffQuestionResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody UpdateQuestionRequest request
	) {
		return questionService.update(QuestionBankActor.from(jwt), id, request);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Change a question status")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Question status changed"),
			@ApiResponse(responseCode = "400", description = "Question is not valid for publication"),
			@ApiResponse(responseCode = "401", description = "Authentication is required"),
			@ApiResponse(responseCode = "403", description = "Institution access or role is denied"),
			@ApiResponse(responseCode = "404", description = "Question or subject not found")
	})
	public StaffQuestionResponse changeStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody ChangeQuestionStatusRequest request
	) {
		return questionService.changeStatus(QuestionBankActor.from(jwt), id, request.status());
	}
}
