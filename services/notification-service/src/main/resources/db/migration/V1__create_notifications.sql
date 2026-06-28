-- ============================================================
-- V1: notifications table
-- notification_db — notification-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE notifications (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    type        VARCHAR(40)  NOT NULL,
    title       VARCHAR(100) NOT NULL,
    message     VARCHAR(500) NOT NULL,
    metadata    VARCHAR(200),
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id   ON notifications(user_id);
CREATE INDEX idx_notifications_unread    ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created   ON notifications(user_id, created_at DESC);
