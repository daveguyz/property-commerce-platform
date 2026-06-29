CREATE TABLE IF NOT EXISTS properties (
    id VARCHAR(36) PRIMARY KEY,
    shopify_product_id VARCHAR(255) UNIQUE,
    host_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    street_address VARCHAR(255),
    city VARCHAR(100),
    region VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    neighborhood VARCHAR(255),
    distance_to_beach VARCHAR(100),
    distance_to_city VARCHAR(100),
    base_rate_per_night DECIMAL(10, 2),
    floor_rate_per_night DECIMAL(10, 2),
    weekly_discount DECIMAL(5, 2),
    monthly_discount DECIMAL(5, 2),
    cleaning_fee DECIMAL(10, 2),
    security_deposit DECIMAL(10, 2),
    currency VARCHAR(10) DEFAULT 'NAD',
    dynamic_pricing_enabled BOOLEAN DEFAULT FALSE,
    current_dynamic_rate DECIMAL(10, 2),
    max_guests INTEGER NOT NULL,
    bedrooms INTEGER NOT NULL,
    bathrooms INTEGER NOT NULL,
    pet_friendly BOOLEAN DEFAULT FALSE,
    has_parking BOOLEAN DEFAULT FALSE,
    has_pool BOOLEAN DEFAULT FALSE,
    has_wifi BOOLEAN DEFAULT FALSE,
    has_kitchen BOOLEAN DEFAULT FALSE,
    has_air_conditioning BOOLEAN DEFAULT FALSE,
    has_heating BOOLEAN DEFAULT FALSE,
    has_washer BOOLEAN DEFAULT FALSE,
    has_dryer BOOLEAN DEFAULT FALSE,
    has_tv BOOLEAN DEFAULT FALSE,
    has_workspace BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    cancellation_policy VARCHAR(50) DEFAULT 'MODERATE',
    min_nights INTEGER DEFAULT 1,
    max_nights INTEGER DEFAULT 365,
    trust_score DECIMAL(5, 2),
    average_rating DECIMAL(3, 2),
    total_reviews INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS amenities (
    id VARCHAR(36) PRIMARY KEY,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    icon VARCHAR(100),
    detail VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS property_images (
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL
);

CREATE TABLE IF NOT EXISTS availability_blocks (
    id VARCHAR(36) PRIMARY KEY,
    property_id VARCHAR(36) NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    block_type VARCHAR(50) NOT NULL,
    booking_id VARCHAR(36),
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT check_dates CHECK (end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_prop_host ON properties(host_id);
CREATE INDEX IF NOT EXISTS idx_prop_status ON properties(status);
CREATE INDEX IF NOT EXISTS idx_prop_city ON properties(city);
CREATE INDEX IF NOT EXISTS idx_prop_coords ON properties(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_avail_property_dates ON availability_blocks(property_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_avail_booking ON availability_blocks(booking_id);
