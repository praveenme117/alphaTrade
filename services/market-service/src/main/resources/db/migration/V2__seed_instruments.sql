-- ============================================================
-- V2: Seed instruments — 3 Stocks + 3 Crypto
-- Realistic mock seed prices (INR for stocks, USDT for crypto)
-- ============================================================

INSERT INTO instruments (symbol, name, type, last_price, open_price, high_price, low_price, close_price, volume, currency, tick_size, lot_size, exchange, sector)
VALUES
    -- ─── Stocks (NSE) ────────────────────────────────────────
    ('RELIANCE',  'Reliance Industries Ltd',      'STOCK',  2850.00,  2820.00, 2900.00, 2800.00, 2820.00,  850000, 'INR',  0.05,   1, 'NSE', 'Energy'),
    ('TCS',       'Tata Consultancy Services Ltd', 'STOCK',  3900.00,  3870.00, 3940.00, 3850.00, 3870.00,  420000, 'INR',  0.05,   1, 'NSE', 'Information Technology'),
    ('INFY',      'Infosys Ltd',                  'STOCK',  1720.00,  1700.00, 1745.00, 1690.00, 1700.00,  670000, 'INR',  0.05,   1, 'NSE', 'Information Technology'),
    ('HDFCBANK',  'HDFC Bank Ltd',                'STOCK',  1650.00,  1630.00, 1675.00, 1620.00, 1630.00,  980000, 'INR',  0.05,   1, 'NSE', 'Banking'),
    ('WIPRO',     'Wipro Ltd',                    'STOCK',   540.00,   535.00,  548.00,  530.00,  535.00, 1200000, 'INR',  0.05,   1, 'NSE', 'Information Technology'),

    -- ─── Crypto ──────────────────────────────────────────────
    ('BTC',       'Bitcoin',                      'CRYPTO', 68000.00, 67500.00, 69200.00, 67000.00, 67500.00, 12500, 'USDT', 0.01, 0.001, 'CRYPTO', 'Cryptocurrency'),
    ('ETH',       'Ethereum',                     'CRYPTO',  3800.00,  3750.00,  3880.00,  3720.00,  3750.00, 45000, 'USDT', 0.01, 0.01,  'CRYPTO', 'Cryptocurrency'),
    ('SOL',       'Solana',                       'CRYPTO',   185.00,   182.00,   189.00,   180.00,   182.00,120000, 'USDT', 0.01, 0.1,   'CRYPTO', 'Cryptocurrency'),
    ('USDT',      'Tether USD',                   'CRYPTO',     1.00,     1.00,     1.01,     0.99,     1.00, 500000, 'USDT', 0.0001, 1, 'CRYPTO', 'Stablecoin');
