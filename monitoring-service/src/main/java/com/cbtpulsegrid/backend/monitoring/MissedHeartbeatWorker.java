package com.cbtpulsegrid.backend.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class MissedHeartbeatWorker {

	private static final Logger log = LoggerFactory.getLogger(MissedHeartbeatWorker.class);

	private final MissedHeartbeatService missedHeartbeatService;

	MissedHeartbeatWorker(MissedHeartbeatService missedHeartbeatService) {
		this.missedHeartbeatService = missedHeartbeatService;
	}

	@Scheduled(fixedDelayString = "${app.monitoring.missed-heartbeat.scan-interval:10s}")
	void detectMissedHeartbeats() {
		try {
			missedHeartbeatService.detectMissedHeartbeats();
		}
		catch (RuntimeException exception) {
			log.error("Failed to scan for missed monitoring heartbeats", exception);
		}
	}
}
