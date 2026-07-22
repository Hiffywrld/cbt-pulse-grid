package com.cbtpulsegrid.backend.attempt;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ExpiredAttemptWorker {

	private static final Logger log = LoggerFactory.getLogger(ExpiredAttemptWorker.class);
	private static final int BATCH_SIZE = 50;

	private final AttemptService attemptService;

	ExpiredAttemptWorker(AttemptService attemptService) {
		this.attemptService = attemptService;
	}

	@Scheduled(fixedDelayString = "${app.attempts.auto-submit-delay:PT30S}")
	void autoSubmitExpiredAttempts() {
		for (UUID attemptId : attemptService.findExpiredAttemptIds(BATCH_SIZE)) {
			try {
				attemptService.autoSubmitExpired(attemptId);
			}
			catch (RuntimeException exception) {
				log.error("Failed to auto-submit expired attempt {}", attemptId, exception);
			}
		}
	}
}
