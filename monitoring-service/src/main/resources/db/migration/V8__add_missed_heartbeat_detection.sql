ALTER TABLE monitoring_states
    ADD COLUMN heartbeat_outage_active BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN heartbeat_missed_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_monitoring_states_heartbeat_outage CHECK (
        (heartbeat_outage_active = FALSE AND heartbeat_missed_at IS NULL)
        OR (heartbeat_outage_active = TRUE AND heartbeat_missed_at IS NOT NULL)
    );

ALTER TABLE monitoring_events
    DROP CONSTRAINT chk_monitoring_events_type,
    ADD CONSTRAINT chk_monitoring_events_type CHECK (event_type IN (
        'TAB_HIDDEN',
        'WINDOW_BLUR',
        'FULLSCREEN_EXIT',
        'COPY_ATTEMPT',
        'PASTE_ATTEMPT',
        'DEVTOOLS_SUSPECTED',
        'NETWORK_DISCONNECTED',
        'NETWORK_RECONNECTED',
        'HEARTBEAT_MISSED'
    ));

CREATE INDEX idx_monitoring_states_heartbeat_timeout
    ON monitoring_states (last_heartbeat_received_at, id)
    WHERE last_heartbeat_received_at IS NOT NULL
      AND heartbeat_outage_active = FALSE;
