# Getting Started and API Reference

## Getting started (first trade in 5 steps)
1. Register with your name, email and password (or log in if you already have an account).
2. Open the Wallet page and make a deposit — it's mock money and always succeeds.
3. Browse the Market page and pick an instrument (9 are available: stocks, crypto and ETFs).
4. Place a BUY order from the Trade page (MARKET is simplest — no price needed).
5. Watch it fill on the Orders page, then see the position on your Portfolio page.

## Password and account
Accounts are identified by email. Passwords are stored securely (BCrypt-hashed). There is currently no self-service password reset flow in the demo — register a new account if you lose access. Use the logout button to revoke your session on shared machines.

## API overview for developers
All APIs go through the gateway at /api/v1 and return a { success, message, data, timestamp } wrapper. Authenticate with POST /auth/register or /auth/login, then send the accessToken as a Bearer token. Main endpoints:
- Auth: POST /auth/register, /auth/login, /auth/refresh, /auth/logout, GET /auth/me
- Market: GET /market/instruments, /market/instruments/search?q=, /market/quotes/{symbol}, WebSocket /ws/prices
- Orders: POST /orders, GET /orders, GET /orders/open, DELETE /orders/{id}
- Portfolio: GET /portfolio/holdings, GET /portfolio/summary
- Wallet: GET /wallet, GET /wallet/ledger
- Payments: POST /payments/deposit, POST /payments/withdraw, GET /payments/history
- Notifications: GET /notifications, PATCH /notifications/{id}/read
- Assistant: POST /assistant/chat with { "message": "...", "history": [{ "role": "user"|"assistant", "content": "..." }] }
Interactive Swagger docs for every service are aggregated at http://localhost:8080/swagger-ui.html.

## Rate limits and errors
The gateway applies per-user rate limiting (backed by Redis); if you hit it you'll receive HTTP 429 — wait a moment and retry. A 401 means your access token expired: call POST /auth/refresh with your refreshToken. Error responses use the same wrapper with success=false and an errorCode.
