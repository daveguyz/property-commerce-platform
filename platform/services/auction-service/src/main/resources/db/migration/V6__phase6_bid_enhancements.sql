-- Phase 6: Bid credential audit trail + max bidders cap

-- credential_id on bids was added in V5 — ensure it exists
ALTER TABLE bids ADD COLUMN IF NOT EXISTS credential_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_bid_credential ON bids(credential_id);

-- Max bidders cap on lots (null = unlimited, for exclusive/invite-only auctions)
ALTER TABLE auction_lots ADD COLUMN IF NOT EXISTS max_bidders_allowed INTEGER;

COMMENT ON COLUMN bids.credential_id IS
    'FK to bidding_credentials. Null for pre-Phase-5 bids and proxy auto-bids.';
COMMENT ON COLUMN auction_lots.max_bidders_allowed IS
    'Cap on distinct credentialed bidders. NULL = unlimited.';
