CREATE TABLE IF NOT EXISTS page_views (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36),
    user_id VARCHAR(36),
    ip_address VARCHAR(50),
    page_type VARCHAR(50),
    page_id VARCHAR(36),
    page_title VARCHAR(500),
    referrer VARCHAR(1000),
    user_agent TEXT,
    country VARCHAR(100),
    city VARCHAR(100),
    device_type VARCHAR(50),
    browser VARCHAR(100),
    os VARCHAR(100),
    time_on_page_seconds INTEGER,
    viewed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS search_events (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36),
    user_id VARCHAR(36),
    query TEXT,
    city VARCHAR(100),
    region VARCHAR(100),
    results_count INTEGER,
    ai_assisted BOOLEAN DEFAULT FALSE,
    result_clicked_index INTEGER,
    result_clicked_id VARCHAR(36),
    searched_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS revenue_metrics (
    id VARCHAR(36) PRIMARY KEY,
    metric_date DATE NOT NULL,
    property_id VARCHAR(36),
    host_id VARCHAR(36),
    granularity VARCHAR(20) NOT NULL,
    gross_revenue DECIMAL(14,2) DEFAULT 0,
    platform_revenue DECIMAL(14,2) DEFAULT 0,
    host_revenue DECIMAL(14,2) DEFAULT 0,
    booking_count INTEGER DEFAULT 0,
    cancelled_count INTEGER DEFAULT 0,
    occupancy_rate DECIMAL(5,4),
    average_nightly_rate DECIMAL(12,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS host_analytics (
    id VARCHAR(36) PRIMARY KEY,
    host_id VARCHAR(36) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    granularity VARCHAR(20) NOT NULL,
    total_revenue DECIMAL(14,2),
    average_booking_value DECIMAL(12,2),
    total_bookings INTEGER,
    confirmed_bookings INTEGER,
    cancelled_bookings INTEGER,
    occupancy_rate DECIMAL(5,4),
    average_rating DECIMAL(3,2),
    response_rate DECIMAL(5,4),
    total_reviews INTEGER,
    profile_views INTEGER,
    search_impressions INTEGER,
    booking_inquiries INTEGER,
    conversion_rate DECIMAL(5,4),
    computed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS site_metrics (
    id VARCHAR(36) PRIMARY KEY,
    metric_date DATE NOT NULL,
    metric_type VARCHAR(20) NOT NULL,
    total_page_views BIGINT,
    unique_sessions BIGINT,
    unique_users BIGINT,
    bounce_rate DECIMAL(5,4),
    avg_session_duration_seconds DECIMAL(10,2),
    total_searches BIGINT,
    searches_with_results BIGINT,
    total_bookings_started BIGINT,
    total_bookings_completed BIGINT,
    booking_conversion_rate DECIMAL(5,4),
    active_listings BIGINT,
    new_listings BIGINT,
    new_users BIGINT,
    returning_users BIGINT,
    p95_response_time_ms DECIMAL(10,2),
    p99_response_time_ms DECIMAL(10,2),
    error_rate DECIMAL(5,4),
    computed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(metric_date, metric_type)
);

CREATE INDEX IF NOT EXISTS idx_pv_session ON page_views(session_id);
CREATE INDEX IF NOT EXISTS idx_pv_time ON page_views(viewed_at);
CREATE INDEX IF NOT EXISTS idx_pv_page ON page_views(page_type);
CREATE INDEX IF NOT EXISTS idx_rm_date ON revenue_metrics(metric_date);
CREATE INDEX IF NOT EXISTS idx_rm_property ON revenue_metrics(property_id);
CREATE INDEX IF NOT EXISTS idx_ha_host_date ON host_analytics(host_id, period_start);
CREATE INDEX IF NOT EXISTS idx_sm_date ON site_metrics(metric_date);
