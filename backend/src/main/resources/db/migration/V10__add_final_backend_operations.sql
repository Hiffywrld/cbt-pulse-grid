CREATE INDEX idx_exam_attempts_expiry_pending
    ON exam_attempts (expires_at, id)
    WHERE status = 'IN_PROGRESS';

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    institution_id UUID,
    actor_id UUID,
    actor_roles VARCHAR(255) NOT NULL,
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(60) NOT NULL,
    resource_id UUID,
    outcome VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    request_id UUID,
    metadata VARCHAR(2000) NOT NULL DEFAULT '{}',
    CONSTRAINT fk_audit_events_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_audit_events_actor
        FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT chk_audit_events_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_audit_events_actor_roles
        CHECK (LENGTH(TRIM(actor_roles)) > 0),
    CONSTRAINT chk_audit_events_action
        CHECK (LENGTH(TRIM(action)) > 0),
    CONSTRAINT chk_audit_events_resource_type
        CHECK (LENGTH(TRIM(resource_type)) > 0)
);

CREATE INDEX idx_audit_events_tenant_time
    ON audit_events (institution_id, occurred_at DESC, id DESC);
CREATE INDEX idx_audit_events_tenant_action_time
    ON audit_events (institution_id, action, occurred_at DESC);
CREATE INDEX idx_audit_events_tenant_resource_time
    ON audit_events (institution_id, resource_type, occurred_at DESC);
CREATE INDEX idx_audit_events_tenant_actor_time
    ON audit_events (institution_id, actor_id, occurred_at DESC)
    WHERE actor_id IS NOT NULL;

CREATE FUNCTION reject_audit_event_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only';
END;
$$;

CREATE TRIGGER trg_audit_events_append_only
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW
    EXECUTE FUNCTION reject_audit_event_mutation();
