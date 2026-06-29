CREATE TABLE IF NOT EXISTS notification_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    booking_id VARCHAR(36),
    type VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(255),
    subject TEXT,
    body TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notif_user ON notification_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_booking ON notification_logs(booking_id);
CREATE INDEX IF NOT EXISTS idx_notif_status ON notification_logs(status);
