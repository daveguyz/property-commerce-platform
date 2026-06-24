CREATE TABLE IF NOT EXISTS conversation_history (
    id VARCHAR(36) PRIMARY KEY,
    guest_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36),
    user_message TEXT,
    ai_response TEXT,
    extracted_intent TEXT,
    returned_property_ids TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS guest_preferences (
    id VARCHAR(36) PRIMARY KEY,
    guest_id VARCHAR(36) NOT NULL UNIQUE,
    preferred_cities TEXT,
    preferred_bedrooms INTEGER,
    preferred_min_guests INTEGER,
    preferred_max_price DECIMAL(12,2),
    prefers_pet_friendly BOOLEAN,
    prefers_parking BOOLEAN,
    prefers_pool BOOLEAN,
    prefers_beach_proximity BOOLEAN,
    preferred_amenities TEXT,
    search_history TEXT,
    total_searches INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_convo_guest ON conversation_history(guest_id);
CREATE INDEX IF NOT EXISTS idx_convo_session ON conversation_history(session_id);
CREATE INDEX IF NOT EXISTS idx_pref_guest ON guest_preferences(guest_id);
