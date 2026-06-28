-- ============================================================
-- V1: wallets + wallet_ledger tables
-- wallet_db — wallet-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE wallets (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    currency        VARCHAR(10)  NOT NULL DEFAULT 'INR',
    balance         NUMERIC(20, 8) NOT NULL DEFAULT 0,
    locked_balance  NUMERIC(20, 8) NOT NULL DEFAULT 0,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wallet_user_currency UNIQUE(user_id, currency)
);

CREATE TABLE wallet_ledger (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       UUID         NOT NULL REFERENCES wallets(id),
    type            VARCHAR(20)  NOT NULL,    -- CREDIT, DEBIT, LOCK, UNLOCK, FEE
    amount          NUMERIC(20, 8) NOT NULL,
    balance_after   NUMERIC(20, 8) NOT NULL,
    description     VARCHAR(100),
    reference_id    UUID,
    reference_type  VARCHAR(30),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallets_user_id        ON wallets(user_id);
CREATE INDEX idx_wallet_ledger_wallet   ON wallet_ledger(wallet_id);
CREATE INDEX idx_wallet_ledger_user     ON wallet_ledger(wallet_id, created_at DESC);
