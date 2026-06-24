CREATE TABLE IF NOT EXISTS user_profiles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    shopify_customer_id VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    profile_image_url VARCHAR(1000),
    nationality VARCHAR(100),
    date_of_birth DATE,
    role VARCHAR(50) NOT NULL DEFAULT 'GUEST',
    id_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    background_check_completed BOOLEAN DEFAULT FALSE,
    id_document_type VARCHAR(50),
    id_document_ref VARCHAR(255),
    trust_score DECIMAL(5,2) DEFAULT 0,
    average_rating DECIMAL(3,2),
    total_bookings INTEGER DEFAULT 0,
    total_reviews INTEGER DEFAULT 0,
    cancelled_bookings INTEGER DEFAULT 0,
    response_rate DECIMAL(4,3),
    stripe_customer_id VARCHAR(255),
    stripe_account_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reviews (
    id VARCHAR(36) PRIMARY KEY,
    booking_id VARCHAR(36) NOT NULL,
    property_id VARCHAR(36) NOT NULL,
    guest_id VARCHAR(36) NOT NULL,
    host_id VARCHAR(36) NOT NULL,
    overall_rating INTEGER NOT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    cleanliness_rating INTEGER CHECK (cleanliness_rating BETWEEN 1 AND 5),
    accuracy_rating INTEGER CHECK (accuracy_rating BETWEEN 1 AND 5),
    check_in_rating INTEGER CHECK (check_in_rating BETWEEN 1 AND 5),
    communication_rating INTEGER CHECK (communication_rating BETWEEN 1 AND 5),
    location_rating INTEGER CHECK (location_rating BETWEEN 1 AND 5),
    value_rating INTEGER CHECK (value_rating BETWEEN 1 AND 5),
    comment TEXT NOT NULL,
    host_response TEXT,
    host_response_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS verification_events (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider VARCHAR(100),
    reference VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_profile_user ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_review_property ON reviews(property_id);
CREATE INDEX IF NOT EXISTS idx_review_guest ON reviews(guest_id);
CREATE INDEX IF NOT EXISTS idx_review_booking ON reviews(booking_id);
