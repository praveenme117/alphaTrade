-- ============================================================
-- V1: payment_orders table
-- payment_db — payment-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE payment_orders (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL,
    direction         VARCHAR(20)  NOT NULL,      -- DEPOSIT, WITHDRAWAL
    amount            NUMERIC(20, 8) NOT NULL,
    currency          VARCHAR(10)  NOT NULL DEFAULT 'INR',
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    gateway           VARCHAR(20)  NOT NULL DEFAULT 'MOCK',
    gateway_reference VARCHAR(100),
    gateway_response  VARCHAR(500),
    idempotency_key   VARCHAR(100) NOT NULL UNIQUE,
    bank_reference    VARCHAR(100),
    processed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_user_id     ON payment_orders(user_id);
CREATE INDEX idx_payment_status      ON payment_orders(status);
CREATE INDEX idx_payment_idempotency ON payment_orders(idempotency_key);
CREATE INDEX idx_payment_user_created ON payment_orders(user_id, created_at DESC);
