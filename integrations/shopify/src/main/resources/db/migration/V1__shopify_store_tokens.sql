CREATE TABLE IF NOT EXISTS shopify_store_tokens (
    id                      VARCHAR(36)     PRIMARY KEY,
    shop_domain             VARCHAR(255)    NOT NULL,
    access_token            VARCHAR(500)    NOT NULL,
    plan_name               VARCHAR(100),
    owner_email             VARCHAR(255),
    provisioned_theme_id    BIGINT,
    theme_provisioned       BOOLEAN         NOT NULL DEFAULT FALSE,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    installed_at            TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    uninstalled_at          TIMESTAMP
);

CREATE UNIQUE INDEX idx_store_domain ON shopify_store_tokens(shop_domain);
CREATE INDEX idx_store_active ON shopify_store_tokens(active);
