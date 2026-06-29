-- Phase 4: Lot Q&A mechanism
-- Bidders post questions on a lot; auctioneers answer privately or publicly.

CREATE TABLE IF NOT EXISTS lot_questions (
    id                    VARCHAR(36)     NOT NULL PRIMARY KEY,
    lot_id                VARCHAR(36)     NOT NULL,
    bidder_id             VARCHAR(36)     NOT NULL,
    bidder_email          VARCHAR(255),
    bidder_display_name   VARCHAR(20)     NOT NULL,  -- "Bidder #4" pseudonym
    content               TEXT            NOT NULL,
    status                VARCHAR(20)     NOT NULL    DEFAULT 'PENDING',
    visibility            VARCHAR(10)     NOT NULL    DEFAULT 'PRIVATE',
    category              VARCHAR(20)     NOT NULL    DEFAULT 'GENERAL',
    priority              VARCHAR(10)     NOT NULL    DEFAULT 'NORMAL',
    response              TEXT,
    response_by           VARCHAR(36),
    responded_at          TIMESTAMP,
    answered_publicly     BOOLEAN         NOT NULL    DEFAULT FALSE,
    flagged_as_abusive    BOOLEAN         NOT NULL    DEFAULT FALSE,
    flagged_at            TIMESTAMP,
    support_ticket_id     VARCHAR(36),
    submitted_at          TIMESTAMP       NOT NULL    DEFAULT NOW(),
    updated_at            TIMESTAMP       NOT NULL    DEFAULT NOW()
);

-- Auctioneer queue: filter by lot + status, newest first
CREATE INDEX IF NOT EXISTS idx_lq_lot    ON lot_questions(lot_id);
-- Bidder view: my questions on a lot
CREATE INDEX IF NOT EXISTS idx_lq_bidder ON lot_questions(lot_id, bidder_id);
-- Status filter (PENDING queue)
CREATE INDEX IF NOT EXISTS idx_lq_status ON lot_questions(lot_id, status);
-- Chronological ordering
CREATE INDEX IF NOT EXISTS idx_lq_submitted ON lot_questions(submitted_at);

COMMENT ON TABLE lot_questions IS
    'Questions and comments posted by approved bidders on specific auction lots.';
COMMENT ON COLUMN lot_questions.bidder_display_name IS
    'Stable pseudonym ("Bidder #N") assigned at first question per lot.';
COMMENT ON COLUMN lot_questions.answered_publicly IS
    'If true, the answer was broadcast to all room attendees.';
COMMENT ON COLUMN lot_questions.flagged_as_abusive IS
    'Silent moderation: flagged questions are hidden from the bidder without notification.';
