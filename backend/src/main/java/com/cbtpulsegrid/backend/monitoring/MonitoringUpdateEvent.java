package com.cbtpulsegrid.backend.monitoring;

import java.util.UUID;

import com.cbtpulsegrid.backend.monitoring.api.LiveMonitoringUpdate;

record MonitoringUpdateEvent(UUID examId, LiveMonitoringUpdate update) {
}
