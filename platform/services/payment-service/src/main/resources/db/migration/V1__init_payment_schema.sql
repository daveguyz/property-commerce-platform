CREATE TABLE IF NOT EXISTS payment_transactions (
    id VARCHAR(36) PRIMARY KEY,
    booking_id VARCHAR(36) NOT NULL,
    guest_id VARCHAR(36) NOT NULL,
    host_id VARCHAR(36) NOT NULL,
    payment_intent_id VARCHAR(255) UNIQUE,
    charge_id VARCHAR(255),
    transfer_id VARCHAR(255),
    host_stripe_account_id VARCHAR(255),
    amount DECIMAL(12,2) NOT NULL,
    host_payout DECIMAL(12,2),
    platform_fee DECIMAL(12,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'NAD',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    stripe_metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS host_payout_accounts (
    id VARCHAR(36) PRIMARY KEY,
    host_id VARCHAR(36) NOT NULL UNIQUE,
    stripe_account_id VARCHAR(255) UNIQUE,
    stripe_account_type VARCHAR(50),
    details_submitted BOOLEAN DEFAULT FALSE,
    charges_enabled BOOLEAN DEFAULT FALSE,
    payouts_enabled BOOLEAN DEFAULT FALSE,
    country VARCHAR(10),
    currency VARCHAR(10),
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_booking ON payment_transactions(booking_id);
CREATE INDEX IF NOT EXISTS idx_payment_intent ON payment_transactions(payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_payment_host ON payment_transactions(host_id);
