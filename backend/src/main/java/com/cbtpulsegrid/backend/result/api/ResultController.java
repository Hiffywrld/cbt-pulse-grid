package com.cbtpulsegrid.backend.result.api;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.cbtpulsegrid.backend.result.ResultCsvExport;
import com.cbtpulsegrid.backend.result.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/results")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'EXAMINER')")
@Tag(name = "Results", description = "Tenant-secured staff examination result reporting")
@SecurityRequirement(name = "bearerAuth")
public class ResultController {

	private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv;charset=UTF-8");

	private final ResultService resultService;

	public ResultController(ResultService resultService) {
		this.resultService = resultService;
	}

	@GetMapping("/exams/{examId}/summary")
	@Operation(summary = "Get aggregate result statistics for an exam")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Exam result summary returned"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Institution staff access is denied"),
		@ApiResponse(responseCode = "404", description = "Exam not found in the institution")
	})
	public ExamResultSummaryResponse summary(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID examId
	) {
		return resultService.summary(ResultActor.from(jwt), examId);
	}

	@GetMapping("/exams/{examId}/candidates")
	@Operation(summary = "List assigned candidates and their result state")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Candidate result page returned"),
		@ApiResponse(responseCode = "400", description = "Filters or pagination are invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Institution staff access is denied"),
		@ApiResponse(responseCode = "404", description = "Exam not found in the institution")
	})
	public ResultPageResponse<CandidateResultResponse> candidates(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID examId,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) CandidateResultStatus status,
			@RequestParam(required = false) Boolean passed,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return resultService.candidates(
				ResultActor.from(jwt),
				examId,
				search,
				status,
				passed,
				page,
				size
		);
	}

	@GetMapping("/attempts/{attemptId}")
	@Operation(summary = "Get a staff-safe attempt result and submitted-answer review")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Attempt result returned"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Institution staff access is denied"),
		@ApiResponse(responseCode = "404", description = "Attempt not found in the institution")
	})
	public StaffAttemptResultResponse attempt(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID attemptId
	) {
		return resultService.attempt(ResultActor.from(jwt), attemptId);
	}

	@GetMapping(value = "/exams/{examId}/export.csv", produces = "text/csv;charset=UTF-8")
	@Operation(summary = "Export safe exam candidate results as UTF-8 CSV")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "CSV result export returned"),
		@ApiResponse(responseCode = "400", description = "Filters are invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Institution staff access is denied"),
		@ApiResponse(responseCode = "404", description = "Exam not found in the institution")
	})
	public ResponseEntity<byte[]> export(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID examId,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) CandidateResultStatus status,
			@RequestParam(required = false) Boolean passed
	) {
		ResultCsvExport export = resultService.exportCsv(
				ResultActor.from(jwt),
				examId,
				search,
				status,
				passed
		);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(CSV_MEDIA_TYPE);
		headers.setContentDisposition(ContentDisposition.attachment()
				.filename(export.filename(), StandardCharsets.UTF_8)
				.build());
		return ResponseEntity.ok().headers(headers).body(export.content());
	}
}
