-- Phase D: Multi-tenancy — the tenants registry

CREATE TABLE IF NOT EXISTS tenants (
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    slug                VARCHAR(60)  NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    contact_email       VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    quota_tier          VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    logo_url            TEXT,
    primary_color       VARCHAR(9)   NOT NULL DEFAULT '#6366f1',
    primary_text_color  VARCHAR(9)   NOT NULL DEFAULT '#ffffff',
    border_radius       VARCHAR(10)  NOT NULL DEFAULT '8px',
    default_currency    VARCHAR(3)   NOT NULL DEFAULT 'USD',
    default_locale      VARCHAR(5)   NOT NULL DEFAULT 'en',
    allowed_currencies  VARCHAR(255) NOT NULL DEFAULT 'USD',
    feature_auctions    BOOLEAN      NOT NULL DEFAULT TRUE,
    feature_bookings    BOOLEAN      NOT NULL DEFAULT TRUE,
    feature_messaging   BOOLEAN      NOT NULL DEFAULT TRUE,
    feature_ai          BOOLEAN      NOT NULL DEFAULT FALSE,
    feature_livestream  BOOLEAN      NOT NULL DEFAULT FALSE,
    api_calls_per_day   INTEGER      NOT NULL DEFAULT 10000,
    bids_per_minute     INTEGER      NOT NULL DEFAULT 60,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);

-- Default single-tenant row so existing deployments keep working (Phase D
-- migration path: all pre-tenancy data belongs to 'default')
INSERT INTO tenants (id, slug, name, contact_email)
VALUES ('default', 'default', 'Default Tenant', 'admin@propertycommerce.io')
ON CONFLICT (id) DO NOTHING;
