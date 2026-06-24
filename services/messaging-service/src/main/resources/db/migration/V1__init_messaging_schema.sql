CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(36) PRIMARY KEY,
    booking_id VARCHAR(36),
    property_id VARCHAR(36),
    participant_one_id VARCHAR(36) NOT NULL,
    participant_two_id VARCHAR(36) NOT NULL,
    type VARCHAR(50),
    last_message_preview TEXT,
    last_message_at TIMESTAMP,
    unread_count_one INTEGER DEFAULT 0,
    unread_count_two INTEGER DEFAULT 0,
    archived_by_one BOOLEAN DEFAULT FALSE,
    archived_by_two BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    deleted_by_sender BOOLEAN DEFAULT FALSE,
    deleted_by_recipient BOOLEAN DEFAULT FALSE,
    metadata TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS message_attachments (
    message_id VARCHAR(36) NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    attachment_url VARCHAR(1000) NOT NULL
);

CREATE TABLE IF NOT EXISTS support_tickets (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    booking_id VARCHAR(36),
    property_id VARCHAR(36),
    subject VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    category VARCHAR(50),
    assigned_admin_id VARCHAR(36),
    conversation_id VARCHAR(36) REFERENCES conversations(id),
    resolved_at TIMESTAMP,
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conv_booking ON conversations(booking_id);
CREATE INDEX IF NOT EXISTS idx_conv_participants ON conversations(participant_one_id, participant_two_id);
CREATE INDEX IF NOT EXISTS idx_msg_convo ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_msg_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_msg_time ON messages(sent_at);
CREATE INDEX IF NOT EXISTS idx_ticket_user ON support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_ticket_status ON support_tickets(status);
