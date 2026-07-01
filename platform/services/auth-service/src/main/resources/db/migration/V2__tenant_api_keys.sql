-- Phase C: Tenant API keys for server-to-server integrations (WordPress plugin, VS Code ext)

CREATE TABLE IF NOT EXISTS tenant_api_keys (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    tenant_id       VARCHAR(36)     NOT NULL,
    label           VARCHAR(100)    NOT NULL,
    key_hash        VARCHAR(64)     NOT NULL UNIQUE,  -- SHA-256 of plaintext key
    key_prefix      VARCHAR(20)     NOT NULL,          -- "pcp_live_ab12…" safe for display
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    last_used_at    TIMESTAMP,
    revoked_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tak_tenant ON tenant_api_keys(tenant_id);

COMMENT ON TABLE tenant_api_keys IS
    'Tenant API keys for server-to-server integrations. Plaintext key never stored.';
COMMENT ON COLUMN tenant_api_keys.key_hash IS
    'SHA-256 hex of the plaintext pcp_live_* or pcp_test_* key.';
