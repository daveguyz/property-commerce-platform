-- ============================================================
-- StaySphere AOS — Auction Service V2: KYC Schema
-- ============================================================

CREATE TABLE IF NOT EXISTS kyc_records (
    id                              VARCHAR(36) PRIMARY KEY,
    user_id                         VARCHAR(36)     NOT NULL,
    user_email                      VARCHAR(255)    NOT NULL,
    stripe_session_id               VARCHAR(100)    NOT NULL,
    stripe_verification_report_id   VARCHAR(100),
    status                          VARCHAR(30)     NOT NULL DEFAULT 'NOT_STARTED',
    verification_url                VARCHAR(1000),
    document_type                   VARCHAR(50),
    risk_score                      VARCHAR(20),
    failure_reason                  TEXT,
    triggering_lot_id               VARCHAR(36),
    ai_fraud_score                  DECIMAL(5,4),
    ai_fraud_notes                  TEXT,
    verified_at                     TIMESTAMP,
    expires_at                      TIMESTAMP,
    created_at                      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kyc_user    ON kyc_records(user_id);
CREATE INDEX idx_kyc_status  ON kyc_records(status);
CREATE UNIQUE INDEX idx_kyc_session ON kyc_records(stripe_session_id);

-- Add KYC-related columns to bids table (fraud score already in V1)
-- Add index for fraud review queue
CREATE INDEX idx_bid_fraud_review ON bids(flagged_for_review) WHERE flagged_for_review = TRUE;
