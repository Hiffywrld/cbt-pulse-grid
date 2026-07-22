package com.cbtpulsegrid.backend.monitoring;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Server-owned Phase 7A risk weights. Connectivity events deliberately carry
 * zero risk because temporary disconnection is expected in offline-first LANs.
 */
final class MonitoringRiskPolicy {

	static final int MAX_RISK_SCORE = 100;

	private static final Map<MonitoringEventType, Integer> WEIGHTS;

	static {
		EnumMap<MonitoringEventType, Integer> weights = new EnumMap<>(MonitoringEventType.class);
		weights.put(MonitoringEventType.TAB_HIDDEN, 10);
		weights.put(MonitoringEventType.WINDOW_BLUR, 5);
		weights.put(MonitoringEventType.FULLSCREEN_EXIT, 15);
		weights.put(MonitoringEventType.COPY_ATTEMPT, 20);
		weights.put(MonitoringEventType.PASTE_ATTEMPT, 20);
		weights.put(MonitoringEventType.DEVTOOLS_SUSPECTED, 25);
		weights.put(MonitoringEventType.NETWORK_DISCONNECTED, 0);
		weights.put(MonitoringEventType.NETWORK_RECONNECTED, 0);
		weights.put(MonitoringEventType.HEARTBEAT_MISSED, 0);
		WEIGHTS = Collections.unmodifiableMap(weights);
	}

	private MonitoringRiskPolicy() {
	}

	static int weight(MonitoringEventType eventType) {
		Integer weight = WEIGHTS.get(eventType);
		if (weight == null) {
			throw new IllegalArgumentException("Unsupported monitoring event type");
		}
		return weight;
	}

	static Map<MonitoringEventType, Integer> weights() {
		return WEIGHTS;
	}
}
