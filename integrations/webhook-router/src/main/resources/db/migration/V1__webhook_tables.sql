-- Phase C: Outbound webhook delivery infrastructure

CREATE TABLE IF NOT EXISTS webhook_endpoints (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    tenant_id       VARCHAR(36)     NOT NULL,
    url             TEXT            NOT NULL,
    signing_secret  VARCHAR(64)     NOT NULL,  -- HMAC-SHA256 key
    event_filter    TEXT,                       -- comma-separated event types; NULL = all
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    failure_count   INTEGER         NOT NULL DEFAULT 0,
    last_delivery_at TIMESTAMP,
    disabled_at     TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_we_tenant ON webhook_endpoints(tenant_id);
CREATE INDEX IF NOT EXISTS idx_we_active ON webhook_endpoints(active);

CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    endpoint_id     VARCHAR(36)     NOT NULL REFERENCES webhook_endpoints(id),
    tenant_id       VARCHAR(36)     NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    event_id        VARCHAR(36),
    payload         TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    http_status     INTEGER,
    response_body   TEXT,
    attempt_count   INTEGER         NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMP,
    delivered_at    TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wd_endpoint ON webhook_deliveries(endpoint_id);
CREATE INDEX IF NOT EXISTS idx_wd_event    ON webhook_deliveries(event_type);
CREATE INDEX IF NOT EXISTS idx_wd_status   ON webhook_deliveries(status);
-- Partial index for efficient retry query
CREATE INDEX IF NOT EXISTS idx_wd_retry    ON webhook_deliveries(next_retry_at)
    WHERE status = 'FAILED' AND attempt_count < 4;
