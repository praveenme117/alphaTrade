-- ============================================================
-- V1: Create users and refresh_tokens tables
-- auth_db — auth-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    kyc_status    VARCHAR(20)  NOT NULL DEFAULT 'VERIFIED',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(512) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash) WHERE revoked = FALSE;

-- Seed: create a default admin user (password: Admin@1234)
INSERT INTO users (email, password_hash, full_name, role, kyc_status)
VALUES (
    'admin@trading.dev',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewYpR/StG3iBkdK2',  -- BCrypt of Admin@1234
    'Platform Admin',
    'ROLE_ADMIN',
    'VERIFIED'
);
