-- ============================================================
-- V1: holdings + trades tables
-- portfolio_db — portfolio-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE holdings (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL,
    instrument_id      UUID         NOT NULL,
    symbol             VARCHAR(20)  NOT NULL,
    quantity           NUMERIC(20, 8) NOT NULL DEFAULT 0,
    average_buy_price  NUMERIC(20, 8) NOT NULL,
    total_invested     NUMERIC(20, 8) NOT NULL DEFAULT 0,
    realized_pnl       NUMERIC(20, 8) NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_holding_user_instrument UNIQUE(user_id, instrument_id)
);

CREATE TABLE trades (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id        UUID         NOT NULL UNIQUE,  -- from TradeExecutedEvent
    user_id         UUID         NOT NULL,
    order_id        UUID         NOT NULL,
    instrument_id   UUID         NOT NULL,
    symbol          VARCHAR(20)  NOT NULL,
    side            VARCHAR(10)  NOT NULL,
    quantity        NUMERIC(20, 8) NOT NULL,
    price           NUMERIC(20, 8) NOT NULL,
    total_value     NUMERIC(20, 8) NOT NULL,
    fee             NUMERIC(20, 8) NOT NULL DEFAULT 0,
    executed_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_holdings_user_id       ON holdings(user_id);
CREATE INDEX idx_holdings_user_symbol   ON holdings(user_id, symbol);
CREATE INDEX idx_trades_user_id         ON trades(user_id);
CREATE INDEX idx_trades_user_symbol     ON trades(user_id, symbol);
CREATE INDEX idx_trades_order_id        ON trades(order_id);
