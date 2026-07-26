package com.cbtpulsegrid.backend.attempt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ExpiredAttemptWorker {

	private static final Logger log = LoggerFactory.getLogger(ExpiredAttemptWorker.class);
	private final AttemptService attemptService;
	private final ExpiredAttemptProperties properties;

	ExpiredAttemptWorker(AttemptService attemptService, ExpiredAttemptProperties properties) {
		this.attemptService = attemptService;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${app.attempts.expiry.scan-interval:10s}")
	void autoSubmitExpiredAttempts() {
		try {
			int processed = attemptService.autoSubmitExpiredBatch(properties.batchSize());
			if (processed > 0) {
				log.info("Automatically submitted {} expired attempts", processed);
			}
		}
		catch (RuntimeException exception) {
			log.error("Failed to process the expired-attempt batch", exception);
		}
	}
}
