-- Phase 1: add auctioneerId column to auction_lots
-- The auctioneer is distinct from the seller (property owner).
-- A seller may manage their own auction (auctioneerId = sellerId) or
-- delegate to a dedicated auctioneer user.

ALTER TABLE auction_lots
    ADD COLUMN IF NOT EXISTS auctioneer_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_lot_auctioneer
    ON auction_lots(auctioneer_id);

-- Comment for DBA context
COMMENT ON COLUMN auction_lots.auctioneer_id IS
    'User ID of the assigned auctioneer. NULL = seller manages their own lot.';
