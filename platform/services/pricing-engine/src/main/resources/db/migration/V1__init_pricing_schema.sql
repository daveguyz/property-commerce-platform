CREATE TABLE IF NOT EXISTS price_history (
    id VARCHAR(36) PRIMARY KEY,
    property_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    base_rate DECIMAL(12,2),
    dynamic_rate DECIMAL(12,2),
    floor_rate DECIMAL(12,2),
    occupancy_multiplier DECIMAL(6,4),
    event_multiplier DECIMAL(6,4),
    season_multiplier DECIMAL(6,4),
    demand_multiplier DECIMAL(6,4),
    pricing_factors TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(property_id, date)
);

CREATE TABLE IF NOT EXISTS pricing_rules (
    id VARCHAR(36) PRIMARY KEY,
    property_id VARCHAR(36),
    rule_type VARCHAR(50) NOT NULL,
    name VARCHAR(255),
    start_date DATE,
    end_date DATE,
    day_of_week INTEGER,
    multiplier DECIMAL(5,3),
    fixed_price DECIMAL(12,2),
    enabled BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_price_property_date ON price_history(property_id, date);
CREATE INDEX IF NOT EXISTS idx_rule_property ON pricing_rules(property_id);
