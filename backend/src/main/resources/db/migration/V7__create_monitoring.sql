CREATE TABLE monitoring_states (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    exam_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    last_heartbeat_id UUID,
    last_client_sequence BIGINT,
    last_client_timestamp TIMESTAMPTZ,
    last_heartbeat_received_at TIMESTAMPTZ,
    last_connectivity_occurred_at TIMESTAMPTZ,
    focused BOOLEAN,
    fullscreen BOOLEAN,
    online BOOLEAN,
    event_count BIGINT NOT NULL DEFAULT 0,
    risk_score INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_monitoring_states_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_monitoring_states_exam
        FOREIGN KEY (exam_id) REFERENCES exams (id),
    CONSTRAINT fk_monitoring_states_attempt
        FOREIGN KEY (attempt_id) REFERENCES exam_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_monitoring_states_candidate
        FOREIGN KEY (candidate_id) REFERENCES users (id),
    CONSTRAINT uq_monitoring_states_attempt UNIQUE (attempt_id),
    CONSTRAINT chk_monitoring_states_sequence
        CHECK (last_client_sequence IS NULL OR last_client_sequence >= 0),
    CONSTRAINT chk_monitoring_states_event_count CHECK (event_count >= 0),
    CONSTRAINT chk_monitoring_states_risk_score CHECK (risk_score BETWEEN 0 AND 100)
);

CREATE TABLE monitoring_heartbeat_receipts (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    heartbeat_id UUID NOT NULL,
    client_sequence BIGINT NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_monitoring_heartbeat_receipts_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_monitoring_heartbeat_receipts_attempt
        FOREIGN KEY (attempt_id) REFERENCES exam_attempts (id) ON DELETE CASCADE,
    CONSTRAINT uq_monitoring_heartbeat_receipts_attempt_heartbeat
        UNIQUE (attempt_id, heartbeat_id),
    CONSTRAINT chk_monitoring_heartbeat_receipts_sequence CHECK (client_sequence >= 0)
);

CREATE TABLE monitoring_events (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    exam_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    client_event_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    risk_weight INTEGER NOT NULL,
    risk_points_applied INTEGER NOT NULL,
    CONSTRAINT fk_monitoring_events_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_monitoring_events_exam
        FOREIGN KEY (exam_id) REFERENCES exams (id),
    CONSTRAINT fk_monitoring_events_attempt
        FOREIGN KEY (attempt_id) REFERENCES exam_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_monitoring_events_candidate
        FOREIGN KEY (candidate_id) REFERENCES users (id),
    CONSTRAINT uq_monitoring_events_attempt_client_event
        UNIQUE (attempt_id, client_event_id),
    CONSTRAINT chk_monitoring_events_type CHECK (event_type IN (
        'TAB_HIDDEN',
        'WINDOW_BLUR',
        'FULLSCREEN_EXIT',
        'COPY_ATTEMPT',
        'PASTE_ATTEMPT',
        'DEVTOOLS_SUSPECTED',
        'NETWORK_DISCONNECTED',
        'NETWORK_RECONNECTED'
    )),
    CONSTRAINT chk_monitoring_events_metadata_object
        CHECK (jsonb_typeof(metadata) = 'object'),
    CONSTRAINT chk_monitoring_events_risk_weight CHECK (risk_weight BETWEEN 0 AND 100),
    CONSTRAINT chk_monitoring_events_risk_applied
        CHECK (risk_points_applied BETWEEN 0 AND risk_weight)
);

CREATE TABLE monitoring_sync_batches (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    sync_id UUID NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_monitoring_sync_batches_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_monitoring_sync_batches_attempt
        FOREIGN KEY (attempt_id) REFERENCES exam_attempts (id) ON DELETE CASCADE,
    CONSTRAINT uq_monitoring_sync_batches_attempt_sync UNIQUE (attempt_id, sync_id)
);

CREATE INDEX idx_monitoring_states_dashboard
    ON monitoring_states (institution_id, exam_id, risk_score DESC, last_heartbeat_received_at DESC);
CREATE INDEX idx_monitoring_states_candidate
    ON monitoring_states (institution_id, candidate_id);
CREATE INDEX idx_monitoring_heartbeat_receipts_attempt
    ON monitoring_heartbeat_receipts (attempt_id, received_at DESC);
CREATE INDEX idx_monitoring_events_dashboard
    ON monitoring_events (institution_id, exam_id, received_at DESC);
CREATE INDEX idx_monitoring_events_attempt_history
    ON monitoring_events (institution_id, attempt_id, occurred_at DESC, id);
CREATE INDEX idx_monitoring_sync_batches_attempt
    ON monitoring_sync_batches (attempt_id, received_at DESC);
