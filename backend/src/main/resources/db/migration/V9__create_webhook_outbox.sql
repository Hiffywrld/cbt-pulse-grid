CREATE TABLE webhook_subscriptions (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    destination_url VARCHAR(2048) NOT NULL,
    status VARCHAR(20) NOT NULL,
    all_event_types BOOLEAN NOT NULL,
    secret_version INTEGER NOT NULL DEFAULT 1,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_webhook_subscriptions_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_webhook_subscriptions_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_webhook_subscriptions_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT chk_webhook_subscriptions_status
        CHECK (status IN ('ACTIVE', 'PAUSED')),
    CONSTRAINT chk_webhook_subscriptions_secret_version
        CHECK (secret_version > 0)
);

CREATE UNIQUE INDEX uq_webhook_subscriptions_institution_name
    ON webhook_subscriptions (institution_id, LOWER(name));

CREATE TABLE webhook_subscription_event_types (
    subscription_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    PRIMARY KEY (subscription_id, event_type),
    CONSTRAINT fk_webhook_subscription_event_types_subscription
        FOREIGN KEY (subscription_id) REFERENCES webhook_subscriptions (id) ON DELETE CASCADE,
    CONSTRAINT chk_webhook_subscription_event_types_type CHECK (event_type IN (
        'TAB_HIDDEN',
        'WINDOW_BLUR',
        'FULLSCREEN_EXIT',
        'COPY_ATTEMPT',
        'PASTE_ATTEMPT',
        'DEVTOOLS_SUSPECTED',
        'NETWORK_DISCONNECTED',
        'NETWORK_RECONNECTED',
        'HEARTBEAT_MISSED'
    ))
);

CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    subscription_id UUID NOT NULL,
    monitoring_event_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    secret_version INTEGER NOT NULL,
    payload_body BYTEA NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    lease_owner UUID,
    lease_expires_at TIMESTAMPTZ,
    response_status INTEGER,
    failure_reason VARCHAR(500),
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_webhook_deliveries_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_webhook_deliveries_subscription
        FOREIGN KEY (subscription_id) REFERENCES webhook_subscriptions (id),
    CONSTRAINT fk_webhook_deliveries_monitoring_event
        FOREIGN KEY (monitoring_event_id) REFERENCES monitoring_events (id),
    CONSTRAINT uq_webhook_deliveries_subscription_event
        UNIQUE (subscription_id, monitoring_event_id),
    CONSTRAINT chk_webhook_deliveries_event_type CHECK (event_type IN (
        'TAB_HIDDEN',
        'WINDOW_BLUR',
        'FULLSCREEN_EXIT',
        'COPY_ATTEMPT',
        'PASTE_ATTEMPT',
        'DEVTOOLS_SUSPECTED',
        'NETWORK_DISCONNECTED',
        'NETWORK_RECONNECTED',
        'HEARTBEAT_MISSED'
    )),
    CONSTRAINT chk_webhook_deliveries_status CHECK (status IN (
        'PENDING',
        'IN_FLIGHT',
        'SUCCEEDED',
        'FAILED',
        'DEAD_LETTER'
    )),
    CONSTRAINT chk_webhook_deliveries_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT chk_webhook_deliveries_secret_version CHECK (secret_version > 0),
    CONSTRAINT chk_webhook_deliveries_response_status CHECK (
        response_status IS NULL OR response_status BETWEEN 100 AND 599
    ),
    CONSTRAINT chk_webhook_deliveries_lease CHECK (
        (status = 'IN_FLIGHT' AND lease_owner IS NOT NULL AND lease_expires_at IS NOT NULL)
        OR (status <> 'IN_FLIGHT' AND lease_owner IS NULL AND lease_expires_at IS NULL)
    )
);

CREATE INDEX idx_webhook_subscriptions_tenant_status
    ON webhook_subscriptions (institution_id, status, created_at DESC);
CREATE INDEX idx_webhook_subscription_event_types_lookup
    ON webhook_subscription_event_types (event_type, subscription_id);
CREATE INDEX idx_webhook_deliveries_due
    ON webhook_deliveries (next_attempt_at, id)
    WHERE status IN ('PENDING', 'IN_FLIGHT');
CREATE INDEX idx_webhook_deliveries_tenant_history
    ON webhook_deliveries (institution_id, created_at DESC, id);
CREATE INDEX idx_webhook_deliveries_subscription
    ON webhook_deliveries (subscription_id, created_at DESC);
