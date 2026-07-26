package com.cbtpulsegrid.backend.monitoring.webhook;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.URI;
import java.time.Clock;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
class WebhookHttpSender {

	static final String EVENT_ID_HEADER = "X-CBT-Pulse-Event-Id";
	static final String EVENT_TYPE_HEADER = "X-CBT-Pulse-Event-Type";
	static final String TIMESTAMP_HEADER = "X-CBT-Pulse-Timestamp";
	static final String SIGNATURE_HEADER = "X-CBT-Pulse-Signature";

	private final HttpClient httpClient;
	private final WebhookUrlValidator urlValidator;
	private final WebhookSigner signer;
	private final WebhookProperties properties;
	private final Clock clock;

	WebhookHttpSender(
			HttpClient httpClient,
			WebhookUrlValidator urlValidator,
			WebhookSigner signer,
			WebhookProperties properties,
			Clock clock
	) {
		this.httpClient = httpClient;
		this.urlValidator = urlValidator;
		this.signer = signer;
		this.properties = properties;
		this.clock = clock;
	}

	int send(WebhookDeliveryContext context) {
		URI destination = urlValidator.validate(context.destinationUrl());
		byte[] body = context.payloadBody();
		String timestamp = Long.toString(clock.instant().getEpochSecond());
		String signature = signer.sign(
				context.subscriptionId(),
				context.secretVersion(),
				timestamp,
				body
		);
		HttpRequest request = HttpRequest.newBuilder(destination)
				.timeout(properties.responseTimeout())
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.header(EVENT_ID_HEADER, context.eventId().toString())
				.header(EVENT_TYPE_HEADER, context.eventType().name())
				.header(TIMESTAMP_HEADER, timestamp)
				.header(SIGNATURE_HEADER, signature)
				.POST(HttpRequest.BodyPublishers.ofByteArray(body))
				.build();
		try {
			return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
		}
		catch (HttpTimeoutException exception) {
			throw new WebhookTransportException(true, exception);
		}
		catch (IOException exception) {
			throw new WebhookTransportException(false, exception);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new WebhookTransportException(false, exception);
		}
	}
}
