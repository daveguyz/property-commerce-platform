-- Phase 7: Purchase agreement lifecycle for auction winners

CREATE TABLE IF NOT EXISTS purchase_agreements (
    id                          VARCHAR(36)     NOT NULL PRIMARY KEY,
    lot_id                      VARCHAR(36)     NOT NULL UNIQUE,
    property_id                 VARCHAR(36)     NOT NULL,
    lot_title                   VARCHAR(500),
    winner_id                   VARCHAR(36)     NOT NULL,
    winner_email                VARCHAR(255),
    seller_id                   VARCHAR(36)     NOT NULL,
    seller_email                VARCHAR(255),
    winning_amount              DECIMAL(14,2)   NOT NULL,
    deposit_amount              DECIMAL(14,2),
    balance_due                 DECIMAL(14,2),
    currency                    VARCHAR(10)     NOT NULL DEFAULT 'USD',
    status                      VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',

    -- E-signature tokens (SHA-256 hashes only — plaintext emailed, never stored)
    buyer_signing_token_hash    VARCHAR(64),
    seller_signing_token_hash   VARCHAR(64),
    signing_tokens_expire_at    TIMESTAMP,

    -- Signature records
    buyer_signature_hash        VARCHAR(64),
    buyer_signed_at             TIMESTAMP,
    seller_signature_hash       VARCHAR(64),
    seller_signed_at            TIMESTAMP,
    fully_executed_at           TIMESTAMP,

    -- Payment deadline
    payment_deadline            TIMESTAMP       NOT NULL,
    payment_deadline_days       INTEGER         NOT NULL DEFAULT 10,
    payment_confirmed_at        TIMESTAMP,

    -- Default handling
    defaulted_at                TIMESTAMP,
    next_bidder_offered         BOOLEAN         NOT NULL DEFAULT FALSE,
    next_bidder_id              VARCHAR(36),

    -- Conveyancing
    conveyancing_initiated_at   TIMESTAMP,
    conveyancer_ref             VARCHAR(100),

    created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pa_lot    ON purchase_agreements(lot_id);
CREATE INDEX IF NOT EXISTS idx_pa_winner ON purchase_agreements(winner_id);
CREATE INDEX IF NOT EXISTS idx_pa_status ON purchase_agreements(status);
CREATE INDEX IF NOT EXISTS idx_pa_deadline ON purchase_agreements(payment_deadline)
    WHERE status = 'FULLY_EXECUTED';

COMMENT ON TABLE purchase_agreements IS
    'One record per auction lot — tracks the full e-signature and payment lifecycle.';
COMMENT ON COLUMN purchase_agreements.buyer_signing_token_hash IS
    'SHA-256 of the UUID token emailed to the buyer. Nulled after use.';
COMMENT ON COLUMN purchase_agreements.conveyancer_ref IS
    'External conveyancer reference (CONV-{shortId}). Stub until live API wired.';
