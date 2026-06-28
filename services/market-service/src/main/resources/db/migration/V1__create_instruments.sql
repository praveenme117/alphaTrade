-- ============================================================
-- V1: Create instruments table
-- market_db — market-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE instrument_type AS ENUM ('STOCK', 'CRYPTO', 'ETF');

CREATE TABLE instruments (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol      VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    last_price  NUMERIC(20, 8),
    open_price  NUMERIC(20, 8),
    high_price  NUMERIC(20, 8),
    low_price   NUMERIC(20, 8),
    close_price NUMERIC(20, 8),
    volume      NUMERIC(20, 2),
    currency    VARCHAR(10)  NOT NULL DEFAULT 'INR',
    tick_size   NUMERIC(10, 5) NOT NULL DEFAULT 0.05,
    lot_size    NUMERIC(10, 2) NOT NULL DEFAULT 1,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    exchange    VARCHAR(10),
    sector      VARCHAR(50),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_instruments_symbol   ON instruments(symbol);
CREATE INDEX idx_instruments_type     ON instruments(type);
CREATE INDEX idx_instruments_active   ON instruments(is_active);
