package com.cbtpulsegrid.backend.institution.api;

import java.util.UUID;

import com.cbtpulsegrid.backend.institution.InstitutionService;
import com.cbtpulsegrid.backend.institution.InstitutionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/institutions")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class InstitutionController {

	private final InstitutionService institutionService;

	public InstitutionController(InstitutionService institutionService) {
		this.institutionService = institutionService;
	}

	@PostMapping({"", "/"})
	public ResponseEntity<InstitutionResponse> create(
			@Valid @RequestBody CreateInstitutionRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(institutionService.create(request));
	}

	@GetMapping({"", "/"})
	public PageResponse<InstitutionResponse> list(
			@RequestParam(required = false) @Size(max = 160) String search,
			@RequestParam(required = false) InstitutionStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return institutionService.list(search, status, page, size);
	}

	@GetMapping("/{id}")
	public InstitutionResponse get(@PathVariable UUID id) {
		return institutionService.get(id);
	}

	@PutMapping("/{id}")
	public InstitutionResponse updateName(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateInstitutionRequest request
	) {
		return institutionService.updateName(id, request.name());
	}

	@PatchMapping("/{id}/status")
	public InstitutionResponse changeStatus(
			@PathVariable UUID id,
			@Valid @RequestBody ChangeInstitutionStatusRequest request
	) {
		return institutionService.changeStatus(id, request.status());
	}
}
