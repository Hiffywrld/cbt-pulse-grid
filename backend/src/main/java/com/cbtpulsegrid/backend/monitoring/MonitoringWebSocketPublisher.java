package com.cbtpulsegrid.backend.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class MonitoringWebSocketPublisher {

	private static final Logger log = LoggerFactory.getLogger(MonitoringWebSocketPublisher.class);

	private final SimpMessagingTemplate messagingTemplate;

	MonitoringWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void publish(MonitoringUpdateEvent event) {
		try {
			messagingTemplate.convertAndSend(
					"/topic/exams/" + event.examId() + "/monitoring",
					event.update()
			);
		}
		catch (RuntimeException exception) {
			log.error(
					"Failed to publish monitoring update for exam {} attempt {}",
					event.examId(),
					event.update().attemptId(),
					exception
			);
		}
	}
}
