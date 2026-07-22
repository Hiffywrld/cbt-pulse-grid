package com.cbtpulsegrid.backend.monitoring.webhook.api;

import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.api.MonitoringActor;
import com.cbtpulsegrid.backend.monitoring.api.MonitoringPageResponse;
import com.cbtpulsegrid.backend.monitoring.webhook.WebhookDeliveryStatus;
import com.cbtpulsegrid.backend.monitoring.webhook.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/webhooks")
@PreAuthorize("hasRole('INSTITUTION_ADMIN')")
@ConditionalOnProperty(name = "app.webhooks.enabled", havingValue = "true")
@Tag(name = "Webhooks", description = "Tenant-secured anti-cheat webhook subscriptions and delivery history")
@SecurityRequirement(name = "bearerAuth")
public class WebhookController {

	private final WebhookService webhookService;

	public WebhookController(WebhookService webhookService) {
		this.webhookService = webhookService;
	}

	@PostMapping("/subscriptions")
	@Operation(summary = "Create an active monitoring webhook subscription")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Subscription created; secret returned once"),
		@ApiResponse(responseCode = "400", description = "Subscription or destination is invalid"),
		@ApiResponse(responseCode = "401", description = "Authentication is required"),
		@ApiResponse(responseCode = "403", description = "Institution administration access is denied"),
		@ApiResponse(responseCode = "409", description = "Subscription name already exists")
	})
	public ResponseEntity<WebhookSubscriptionSecretResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateWebhookSubscriptionRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(webhookService.create(MonitoringActor.from(jwt), request));
	}

	@GetMapping("/subscriptions")
	@Operation(summary = "List institution webhook subscriptions without secrets")
	public MonitoringPageResponse<WebhookSubscriptionResponse> listSubscriptions(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return webhookService.list(MonitoringActor.from(jwt), page, size);
	}

	@GetMapping("/subscriptions/{id}")
	@Operation(summary = "Get one institution webhook subscription without its secret")
	public WebhookSubscriptionResponse getSubscription(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id
	) {
		return webhookService.get(MonitoringActor.from(jwt), id);
	}

	@PatchMapping("/subscriptions/{id}/status")
	@Operation(summary = "Pause or reactivate a webhook subscription")
	public WebhookSubscriptionResponse changeStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id,
			@Valid @RequestBody ChangeWebhookSubscriptionStatusRequest request
	) {
		return webhookService.changeStatus(MonitoringActor.from(jwt), id, request);
	}

	@PostMapping("/subscriptions/{id}/rotate-secret")
	@Operation(summary = "Rotate and return a new subscription signing secret once")
	public WebhookSubscriptionSecretResponse rotateSecret(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id
	) {
		return webhookService.rotateSecret(MonitoringActor.from(jwt), id);
	}

	@GetMapping("/deliveries")
	@Operation(summary = "List institution webhook delivery status")
	public MonitoringPageResponse<WebhookDeliveryResponse> deliveries(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) WebhookDeliveryStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return webhookService.deliveries(MonitoringActor.from(jwt), status, page, size);
	}

	@PostMapping("/deliveries/{id}/retry")
	@Operation(summary = "Manually retry an owned failed or dead-letter delivery")
	public WebhookDeliveryResponse retry(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id
	) {
		return webhookService.retry(MonitoringActor.from(jwt), id);
	}
}
