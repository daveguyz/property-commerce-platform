CREATE TABLE IF NOT EXISTS bookings (
    id VARCHAR(36) PRIMARY KEY,
    property_id VARCHAR(36) NOT NULL,
    guest_id VARCHAR(36) NOT NULL,
    host_id VARCHAR(36) NOT NULL,
    shopify_order_id VARCHAR(255),
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guest_count INTEGER NOT NULL,
    children_count INTEGER DEFAULT 0,
    infant_count INTEGER DEFAULT 0,
    pet_count INTEGER DEFAULT 0,
    base_amount DECIMAL(12,2),
    cleaning_fee DECIMAL(12,2),
    service_fee DECIMAL(12,2),
    taxes DECIMAL(12,2),
    total_amount DECIMAL(12,2),
    host_payout DECIMAL(12,2),
    platform_fee DECIMAL(12,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'NAD',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_intent_id VARCHAR(255),
    access_code VARCHAR(20),
    special_requests TEXT,
    cancellation_reason TEXT,
    cancelled_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    checked_in_at TIMESTAMP,
    checked_out_at TIMESTAMP,
    CONSTRAINT check_dates CHECK (check_out > check_in),
    CONSTRAINT check_guests CHECK (guest_count > 0)
);

CREATE TABLE IF NOT EXISTS booking_negotiations (
    id VARCHAR(36) PRIMARY KEY,
    property_id VARCHAR(36) NOT NULL,
    guest_id VARCHAR(36) NOT NULL,
    host_id VARCHAR(36) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guest_count INTEGER NOT NULL,
    original_price DECIMAL(12,2),
    offered_price DECIMAL(12,2) NOT NULL,
    counter_price DECIMAL(12,2),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    guest_message TEXT,
    host_response TEXT,
    ai_suggestion TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trip_bookings (
    id VARCHAR(36) PRIMARY KEY,
    guest_id VARCHAR(36) NOT NULL,
    trip_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_amount DECIMAL(12,2),
    currency VARCHAR(10) DEFAULT 'NAD',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trip_booking_ids (
    trip_id VARCHAR(36) NOT NULL REFERENCES trip_bookings(id) ON DELETE CASCADE,
    booking_id VARCHAR(36) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_booking_guest ON bookings(guest_id);
CREATE INDEX IF NOT EXISTS idx_booking_host ON bookings(host_id);
CREATE INDEX IF NOT EXISTS idx_booking_property ON bookings(property_id);
CREATE INDEX IF NOT EXISTS idx_booking_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_booking_dates ON bookings(check_in, check_out);
CREATE INDEX IF NOT EXISTS idx_neg_property ON booking_negotiations(property_id);
CREATE INDEX IF NOT EXISTS idx_neg_guest ON booking_negotiations(guest_id);
CREATE INDEX IF NOT EXISTS idx_neg_host ON booking_negotiations(host_id);
