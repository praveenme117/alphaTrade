-- ============================================================
-- V1: orders table
-- order_db — order-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE orders (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    instrument_id   UUID         NOT NULL,
    symbol          VARCHAR(20)  NOT NULL,
    order_type      VARCHAR(20)  NOT NULL,
    side            VARCHAR(10)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    product_type    VARCHAR(20)  NOT NULL DEFAULT 'CNC',
    quantity        NUMERIC(20, 8) NOT NULL,
    price           NUMERIC(20, 8),
    stop_price      NUMERIC(20, 8),
    filled_quantity NUMERIC(20, 8) NOT NULL DEFAULT 0,
    average_price   NUMERIC(20, 8),
    fee             NUMERIC(20, 8) NOT NULL DEFAULT 0,
    reject_reason   VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_symbol  ON orders(symbol);
CREATE INDEX idx_orders_status  ON orders(status);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
