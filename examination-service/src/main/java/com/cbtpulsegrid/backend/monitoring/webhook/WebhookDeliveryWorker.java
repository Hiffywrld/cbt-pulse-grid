package com.cbtpulsegrid.backend.monitoring.webhook;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.webhooks.enabled", havingValue = "true")
class WebhookDeliveryWorker {

	private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryWorker.class);

	private final UUID leaseOwner = UUID.randomUUID();
	private final WebhookDeliveryCoordinator coordinator;
	private final WebhookHttpSender sender;

	WebhookDeliveryWorker(WebhookDeliveryCoordinator coordinator, WebhookHttpSender sender) {
		this.coordinator = coordinator;
		this.sender = sender;
	}

	@Scheduled(fixedDelayString = "${app.webhooks.scan-interval:5s}")
	void deliverDueWebhooks() {
		for (UUID deliveryId : coordinator.claimDue(leaseOwner)) {
			try {
				var context = coordinator.prepare(deliveryId, leaseOwner);
				if (context.isEmpty()) {
					coordinator.releasePaused(deliveryId, leaseOwner);
					continue;
				}
				int responseStatus = sender.send(context.get());
				coordinator.completeHttp(deliveryId, leaseOwner, responseStatus);
			}
			catch (WebhookTransportException exception) {
				coordinator.completeTransportFailure(
						deliveryId,
						leaseOwner,
						exception.isTimeout()
				);
			}
            catch (IllegalArgumentException exception) {
                coordinator.completePermanentFailure(
                        deliveryId,
                        leaseOwner,
                        "Webhook destination is not allowed"
                );
            }

			catch (RuntimeException exception) {
				log.error("Unexpected webhook delivery failure for delivery {}", deliveryId, exception);
			}
		}
	}
}
