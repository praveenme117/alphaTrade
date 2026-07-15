# alphaTrade — Full Project Guide

A trading-platform built as **Java 21 / Spring Boot 3.4.4 microservices** behind a **Spring Cloud Gateway**, with a **React 19 + TypeScript + Vite** frontend. This guide explains every microservice, how to run the whole stack, how the frontend talks to the backend, and gives you every API endpoint so you can test the system from Postman.

> An existing `ARCHITECTURE.md` at the repo root also documents the backend in depth. This guide is meant to be the practical, test-it-yourself companion — it adds the frontend wiring and a Postman-ready endpoint list, and calls out a few endpoints that exist in code but weren't in the older doc.

---

## 1. High-Level Architecture

```
                        ┌─────────────────────────┐
   Browser / Postman ───▶   API Gateway  :8080     │  (Spring Cloud Gateway, reactive)
                        │  - JWT validation        │
                        │  - Rate limiting (Redis) │
                        │  - CORS, routing, retry  │
                        └───────────┬─────────────┘
                                    │  X-User-Id / X-User-Role / X-User-Email injected
        ┌───────────────┬──────────┼───────────┬───────────────┬──────────────────┐
        ▼               ▼          ▼           ▼               ▼                  ▼
  auth-service    market-service  order-service portfolio-service wallet-service  payment-service
     :8081           :8082          :8083          :8084             :8085          :8086
        │               │              │              │                 │              │
   auth_db          market_db      order_db      portfolio_db      wallet_db      payment_db
  (postgres)        (postgres)    (postgres)      (postgres)      (postgres)      (postgres)

                notification-service :8087 ── notification_db (postgres)
                ai-assistant-service :8088 ── Groq LLM + Qdrant Cloud (no DB, stateless)

  Kafka (topics: market.price.updates, order.filled, payment.completed)
  Redis (price cache + gateway rate limiting)
```

Each service owns its own Postgres database (database-per-service). Services never call each other's databases directly — they talk over REST (OpenFeign, order→wallet) or asynchronously over Kafka events. There is **no nginx** — the Spring Cloud Gateway service is the only reverse proxy / API gateway.

---

## 2. Microservices Reference

All backend services are Maven modules under `services/<name>/`, Java 21, Spring Boot 3.4.4, packaged as a single fat JAR and run in `eclipse-temurin:21-jre-alpine` containers. Each module follows the same internal layout: `config/`, `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `resources/application.yml`, `resources/db/migration/V*.sql` (Flyway).

| Service | Port | Database | Purpose | Notable tech |
|---|---|---|---|---|
| **api-gateway** | 8080 | — | Single entry point; JWT validation, routing to all services, rate limiting, CORS, aggregated Swagger UI | Spring Cloud Gateway (WebFlux, reactive), Redis rate limiter |
| **auth-service** | 8081 | `auth_db` | User registration/login, JWT issuing & refresh, session/token revocation | Spring Security, JJWT (HMAC-SHA384), BCrypt |
| **market-service** | 8082 | `market_db` | Instrument catalog, quotes, live mock price feed (GBM random walk) | Kafka producer, Redis cache writer, WebSocket (`/ws/prices`) |
| **order-service** | 8083 | `order_db` | Place/cancel/list orders, mock matching engine | Kafka producer, Redis reader, OpenFeign → wallet-service |
| **portfolio-service** | 8084 | `portfolio_db` | Holdings & portfolio summary, built from filled orders | Kafka consumer (`OrderFilledEvent`) |
| **wallet-service** | 8085 | `wallet_db` | Balances per currency, fund lock/unlock, ledger | Kafka consumer, internal lock/unlock API for order-service |
| **payment-service** | 8086 | `payment_db` | Mock deposits/withdrawals, payment history, gateway webhook | Kafka producer, mock gateway (`MOCK`) |
| **notification-service** | 8087 | `notification_db` | User notifications (order filled, payment completed) | Kafka consumer |
| **ai-assistant-service** | 8088 | — (stateless) | AI chatbot ("Alpha") answering platform questions via RAG | Groq LLM (chat), Qdrant Cloud (vector search + server-side embeddings) |

### What each service does, in detail

**auth-service** — owns user identity. Registration hashes passwords with BCrypt and stores users in `auth_db`. Login issues a short-lived access JWT + a longer-lived refresh token (stored server-side so it can be revoked). `/me` returns the caller's profile from the `X-User-Id` header the gateway injects. `/logout` revokes all refresh tokens for that user.

**market-service** — owns the instrument catalog (seeded with 9 instruments on first migration) and simulates live prices with a mock Geometric Brownian Motion feed, publishing `PriceUpdatedEvent` to Kafka every tick and caching the latest price per symbol in Redis (`price:{SYMBOL}`, 5s TTL). Also exposes a WebSocket endpoint for the frontend to stream live prices.

**order-service** — accepts new orders (MARKET/LIMIT/STOP_LOSS, BUY/SELL, CASH/INTRADAY/DELIVERY), calls wallet-service synchronously (Feign) to lock funds before accepting the order, then runs an async mock matching engine that fills the order and publishes `OrderFilledEvent` to Kafka. Reads live prices from Redis rather than hitting market-service over HTTP for latency reasons.

**portfolio-service** — pure Kafka consumer of `OrderFilledEvent`; maintains a running holdings table (qty, average price, realized P&L) per user/instrument and exposes read APIs plus a computed summary.

**wallet-service** — owns balances per currency. Exposes lock/unlock endpoints used internally by order-service (not meant to be called by end users/Postman with a normal JWT — they're part of the internal service-to-service contract) and a public ledger/balance API for the frontend. Consumes `OrderFilledEvent` (to settle funds) and `PaymentCompletedEvent` (to credit deposits).

**payment-service** — mock payment gateway. Deposits/withdrawals always succeed (no real payment provider is wired in — Stripe/Razorpay keys in `.env.example` are placeholders, unused). On success it publishes `PaymentCompletedEvent` to Kafka so wallet-service can credit the balance and notification-service can notify the user. Also exposes a public webhook endpoint stub.

**notification-service** — pure Kafka consumer of `OrderFilledEvent` and `PaymentCompletedEvent`; creates in-app notifications, exposes read/unread-count/mark-read APIs for the frontend bell icon.

**ai-assistant-service** — the platform chatbot, surfaced in the frontend as a floating chat widget on every authenticated page. It is a Retrieval-Augmented Generation (RAG) service: a markdown knowledge base bundled in `src/main/resources/knowledge/` is chunked and stored in a **Qdrant Cloud** collection (`alphatrade_knowledge`); embeddings are computed server-side by Qdrant Cloud Inference (`all-minilm-l6-v2`), so no embedding model runs locally. Each chat request retrieves the most relevant doc sections and generates an answer with a **Groq**-hosted LLM (default `llama-3.3-70b-versatile`). The service has no database and no Kafka — conversation history is kept client-side and sent with each request. Knowledge is (re-)ingested automatically on startup (idempotent) or manually via `POST /assistant/reindex`. Requires `GROQ_API_KEY`, `QDRANT_CLUSTER_ENDPOINT` and `QDRANT_API_KEY` in the root `.env`; without them the service still starts but chat returns 503.

**api-gateway** — the only service exposed to the outside world in a real deployment. It:
- Validates JWTs (`JwtAuthFilter`) using the same `JWT_SECRET` as auth-service, and on success strips the `Authorization` header and injects trusted `X-User-Id`, `X-User-Role`, `X-User-Email` headers before forwarding downstream (downstream services trust these headers as-is — they do not re-validate the JWT).
- Applies Redis-backed rate limiting on every route.
- Retries idempotent GETs on 502/503/504.
- Allows all origins via CORS (dev-friendly, not hardened for prod).
- Aggregates all services' OpenAPI docs at `/docs/{service}/v3/api-docs` and serves a combined Swagger UI at `http://localhost:8080/swagger-ui.html`.

---

## 3. How to Run Everything

### Prerequisites
- Java 21, Maven
- Docker + Docker Compose
- Node 18+ (for frontend)

### Option A — Full stack with Docker (recommended)

```bash
# 1. Copy .env.example to .env and fill in GROQ_API_KEY / QDRANT_* (needed by the AI assistant)
cp .env.example .env

# 2. Build all backend JARs first (Docker images copy pre-built jars, no in-container Maven build)
mvn clean package -DskipTests

# 3. Start infra + all 9 microservices
docker compose --profile full up -d --build
```

This starts 21 containers: 7 Postgres instances (one per service), Redis, Zookeeper, Kafka, Kafka UI, Redis Insight, and the 9 Spring Boot services.

> **Gotcha:** `docker compose up -d` (no `--profile full`) only starts the infrastructure (databases, Kafka, Redis) — the microservices are gated behind the `full` profile and will NOT start without it.

Tear down:
```bash
docker compose --profile full down       # stop everything
docker compose --profile full down -v    # stop everything + wipe volumes/data
```

Useful infra UIs once running:
- Kafka UI: http://localhost:9000
- Redis Insight: http://localhost:5540

### Option B — Run a single service locally (e.g. while developing)

Start just the infra it needs via `docker compose up -d`, then run the service's `*Application.java` from your IDE, or:

```bash
cd services/order-service
mvn spring-boot:run
```

Each service's `application.yml` defaults to `localhost` ports for its DB/Kafka/Redis, so this works without any extra env vars as long as `docker compose up -d` (infra) is running.

### Frontend

```bash
cd services/frontend
cp .env.example .env     # adjust VITE_API_BASE_URL / VITE_WS_URL if needed
npm install
npm run dev              # http://localhost:3000
```

Other scripts: `npm run build` (`tsc -b && vite build`), `npm run preview`, `npm run lint` (oxlint).

### Verifying it's up
- Gateway health: `GET http://localhost:8080/actuator/health`
- Swagger UI (all services aggregated): `http://localhost:8080/swagger-ui.html`
- `psql -h localhost -p <port> -U trading -d <db_name>` to inspect any DB directly (see port table in §7).

---

## 4. How Frontend and Backend Are Connected

- Frontend lives at `services/frontend/`, framework React 19 + TypeScript + Vite. State via `zustand`, server-state/caching via `@tanstack/react-query`, HTTP via `axios`.
- `.env` variables (`services/frontend/.env.example`):
  ```
  VITE_API_BASE_URL=http://localhost:8080   # API Gateway
  VITE_WS_URL=ws://localhost:8080
  VITE_DEMO_EMAIL=
  VITE_DEMO_PASSWORD=
  ```
- `vite.config.ts` reads these and sets up a dev-server proxy: any request to `/api/**` is forwarded to `VITE_API_BASE_URL`, and `/ws/**` is proxied (websocket-aware) to `VITE_WS_URL`. This means in dev, the browser only ever talks to `localhost:3000`, and Vite forwards to the gateway on `8080` — avoiding CORS entirely in local dev.
- `src/api/client.ts` — a single axios instance, `baseURL: '/api/v1'`. On every request it attaches `Authorization: Bearer <accessToken>` from the zustand `authStore`. It also implements automatic session refresh: on a `401`, it calls `POST /api/v1/auth/refresh`, queues any other requests that failed concurrently, retries them with the new token, and logs the user out if the refresh itself fails.
- `src/api/*.ts` — one file per backend domain (`auth.ts`, `market.ts`, `orders.ts`, `portfolio.ts`, `wallet.ts`, `payments.ts`, `notifications.ts`), each just a thin wrapper around the shared axios client hitting the paths in §5 below.
- `src/hooks/useMarketWebSocket.ts` opens `ws(s)://<current host>/ws/prices?token=<accessToken>` (same-origin, so it flows through the same Vite/gateway proxy) and pushes live price ticks into `marketStore`; it auto-reconnects every 3s if the socket drops.
- Route protection: `components/shared/ProtectedRoute.tsx` checks the zustand `authStore` and redirects to `/login` if there's no valid access token; protected pages are wrapped in `components/layout/Layout.tsx` (nav/shell).

**Page → API call map:**

| Page | Calls |
|---|---|
| `pages/auth/LoginPage`, `RegisterPage` | `POST /auth/login`, `POST /auth/register` |
| `DashboardPage` (and app shell) | `GET /auth/me`, `GET /notifications/unread-count` |
| `MarketPage` | `GET /market/instruments`, `GET /market/instruments/symbol/{symbol}`, `GET /market/instruments/search?q=`, `GET /market/quotes/{symbol}` |
| `TradePage` | `POST /orders`, plus market quotes for pricing |
| `OrdersPage` | `GET /orders`, `GET /orders/open`, `DELETE /orders/{id}` |
| `PortfolioPage` | `GET /portfolio/holdings`, `GET /portfolio/summary` |
| `WalletPage` | `GET /wallet`, `GET /wallet/{currency}`, `GET /wallet/ledger`, `POST /payments/deposit`, `POST /payments/withdraw` |
| `NotificationsPage` | `GET /notifications`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` |
| `AssistantWidget` (floating chat, all pages) | `POST /assistant/chat` |

---

## 5. Full API Reference (for Postman)

Base URL for everything below (through the gateway): `http://localhost:8080/api/v1`

All responses are wrapped: `{ "success": bool, "message": string, "data": <payload>, "timestamp": "..." }`.

**Auth model:** register/login first, then set a Postman **Bearer Token** (or an `Authorization: Bearer <accessToken>` header) using the `accessToken` returned from login, for every "Protected" endpoint below. The gateway validates the JWT and forwards trusted `X-User-Id`/`X-User-Role`/`X-User-Email` headers to the service — you never need to set those yourself.

### 5.1 Auth Service — `/api/v1/auth`

| Method | Path | Auth | Body | Notes |
|---|---|---|---|---|
| POST | `/auth/register` | Public | `{ "fullName", "email", "password", "phone"? }` | 201, returns user + tokens |
| POST | `/auth/login` | Public | `{ "email", "password" }` | returns `accessToken` + `refreshToken` |
| POST | `/auth/refresh` | Public | `{ "refreshToken" }` | rotates tokens |
| POST | `/auth/logout` | Protected | — | revokes all refresh tokens for the user |
| GET | `/auth/me` | Protected | — | current user profile |

### 5.2 Market Service — `/api/v1/market`

| Method | Path | Auth | Params | Notes |
|---|---|---|---|---|
| GET | `/market/instruments` | Protected | `?type=STOCK\|CRYPTO\|ETF` optional | list instruments |
| GET | `/market/instruments/{id}` | Protected | UUID path | instrument by id |
| GET | `/market/instruments/symbol/{symbol}` | Protected | symbol path | instrument by symbol |
| GET | `/market/instruments/search` | Protected | `?q=` required | search by partial symbol/name |
| GET | `/market/quotes/{symbol}` | Protected | symbol path | live quote for one symbol |
| GET | `/market/quotes` | Protected | `?symbols=AAPL,TSLA` | bulk quotes |
| WS | `/ws/prices` | via gateway | `?token=<accessToken>` | live price stream (not a Postman REST call — use a WS client) |

### 5.3 Order Service — `/api/v1/orders`

| Method | Path | Auth | Body/Params | Notes |
|---|---|---|---|---|
| POST | `/orders` | Protected | `{ "instrumentId", "symbol", "orderType": "MARKET\|LIMIT\|STOP_LOSS", "side": "BUY\|SELL", "productType": "CASH\|INTRADAY\|DELIVERY", "quantity", "price"?, "stopPrice"? }` | locks funds via wallet-service, then async-fills |
| GET | `/orders` | Protected | `?page=0&size=20` | paginated order history |
| GET | `/orders/{id}` | Protected | UUID path | single order |
| GET | `/orders/open` | Protected | — | open orders only |
| DELETE | `/orders/{id}` | Protected | UUID path | cancel order, unlocks funds |

### 5.4 Portfolio Service — `/api/v1/portfolio`

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/portfolio/holdings` | Protected | all holdings with qty > 0 |
| GET | `/portfolio/holdings/{instrumentId}` | Protected | single holding |
| GET | `/portfolio/summary` | Protected | `{ holdingsCount, totalInvested, totalRealizedPnl }` |

### 5.5 Wallet Service — `/api/v1/wallet`

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/wallet` | Protected | all balances for the user |
| GET | `/wallet/{currency}` | Protected | balance for one currency (e.g. `INR`) |
| GET | `/wallet/ledger` | Protected | `?page=0&size=50` — paginated transaction ledger |
| POST | `/wallet/lock` | **Internal only** | not exposed via gateway to end users — called service-to-service by order-service (Feign). Do not test via Postman with a normal user JWT. |
| POST | `/wallet/unlock` | **Internal only** | same as above |

### 5.6 Payment Service — `/api/v1/payments`

| Method | Path | Auth | Body | Notes |
|---|---|---|---|---|
| POST | `/payments/deposit` | Protected | `{ "amount", "currency" }` | mock deposit, always succeeds, credits wallet via Kafka |
| POST | `/payments/withdraw` | Protected | `{ "amount", "currency" }` | mock withdrawal |
| GET | `/payments/history` | Protected | `?page=0&size=20` | paginated payment history |
| GET | `/payments/{id}` | Protected | UUID path | single payment |
| POST | `/payments/webhook/{gateway}` | Public | raw string body, optional `X-Signature` header | mock gateway webhook stub, logs only |

### 5.7 Notification Service — `/api/v1/notifications`

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/notifications` | Protected | `?page=0&size=20` |
| GET | `/notifications/unread-count` | Protected | unread count |
| PATCH | `/notifications/{id}/read` | Protected | mark one as read |
| PATCH | `/notifications/read-all` | Protected | mark all as read |

### 5.8 AI Assistant Service — `/api/v1/assistant`

| Method | Path | Auth | Body | Notes |
|---|---|---|---|---|
| POST | `/assistant/chat` | Protected | `{ "message": "How do I place a limit order?", "history": [{ "role": "user\|assistant", "content": "..." }] }` | RAG answer; returns `{ reply, model, sources[] }`. `history` is optional (service is stateless — pass prior turns back each call) |
| POST | `/assistant/reindex` | Protected | — | re-ingests the bundled knowledge base into Qdrant, returns chunk count |

> Requires `GROQ_API_KEY` + `QDRANT_CLUSTER_ENDPOINT` + `QDRANT_API_KEY` in the root `.env`. Without them, chat returns 503 `ASSISTANT_UNAVAILABLE`. If only Qdrant is missing, the bot still answers, just without doc retrieval.

### 5.9 Suggested Postman Testing Flow

1. `POST /auth/register` → grab `accessToken`/`refreshToken`, or `POST /auth/login` if the user already exists.
2. Set `accessToken` as a Postman collection variable, use `Bearer {{accessToken}}` as the collection-level auth.
3. `POST /payments/deposit` to fund the wallet (mock, always succeeds).
4. `GET /wallet` to confirm the balance credited.
5. `GET /market/instruments` → pick an `instrumentId`/`symbol`.
6. `POST /orders` to place a trade.
7. `GET /orders` / `GET /orders/open` to see it, `GET /portfolio/holdings` once it fills.
8. `GET /notifications` to see the order-filled / payment-completed notifications generated via Kafka.
9. When `accessToken` expires, `POST /auth/refresh` with `refreshToken` to get a new one.

Tip: import the combined OpenAPI spec into Postman directly from `http://localhost:8080/swagger-ui.html` (or the underlying `/docs/{service}/v3/api-docs` aggregation) instead of hand-building requests.

---

## 6. Messaging & Caching

**Kafka topics** (`kafka:29092` inside Docker network, `localhost:9092` from host):

| Topic | Producer | Consumers |
|---|---|---|
| `market.price.updates` | market-service | market-service (own Redis cache writer), order-service, portfolio-service |
| `order.filled` | order-service | portfolio-service, wallet-service, notification-service |
| `payment.completed` | payment-service | wallet-service, notification-service |

**Redis** (`localhost:6379`): live price cache `price:{SYMBOL}` (JSON, 5s TTL, written by market-service, read by order-service to avoid a synchronous HTTP hop to market-service) + API gateway rate-limit counters.

---

## 7. Databases

One Postgres 16 instance per service, all DDL owned by Flyway (`hibernate.ddl-auto: validate`, Hibernate never mutates schema). Default creds everywhere (dev only): `trading` / `trading_secret`.

| Service | DB name | Host port | Connect |
|---|---|---|---|
| auth-service | `auth_db` | 5433 | `psql -h localhost -p 5433 -U trading -d auth_db` |
| market-service | `market_db` | 5434 | `psql -h localhost -p 5434 -U trading -d market_db` |
| order-service | `order_db` | 5435 | `psql -h localhost -p 5435 -U trading -d order_db` |
| portfolio-service | `portfolio_db` | 5436 | `psql -h localhost -p 5436 -U trading -d portfolio_db` |
| wallet-service | `wallet_db` | 5437 | `psql -h localhost -p 5437 -U trading -d wallet_db` |
| payment-service | `payment_db` | 5438 | `psql -h localhost -p 5438 -U trading -d payment_db` |
| notification-service | `notification_db` | 5439 | `psql -h localhost -p 5439 -U trading -d notification_db` |

---

## 8. Environment Variables

**Root `.env.example`** (used by docker-compose):
```
JWT_SECRET=<base64, 32+ chars — must match between auth-service and api-gateway>
DB_USER=trading
DB_PASSWORD=trading_secret
KAFKA_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
SPRING_PROFILES_ACTIVE=dev
GROQ_API_KEY=<Groq API key — console.groq.com, used by ai-assistant-service>
GROQ_MODEL=llama-3.3-70b-versatile
QDRANT_CLUSTER_ENDPOINT=<Qdrant Cloud cluster URL — cloud.qdrant.io>
QDRANT_API_KEY=<Qdrant Cloud API key>
# STRIPE_API_KEY / RAZORPAY_KEY_ID etc. — placeholders, unused (payment gateway is mocked)
# SENDGRID_API_KEY — placeholder, unused (notifications are in-app only)
```

**Per-service overrides** (set in `docker-compose.yml`, defaulted to `localhost` in each `application.yml` for local/IDE runs):
- All services: `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`
- Kafka-connected services: `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- Redis-connected services (gateway, market, order, portfolio, payment): `REDIS_HOST`, `REDIS_PORT`
- auth-service & api-gateway: `TRADING_JWT_SECRET` (must match)
- api-gateway only: `AUTH_SERVICE_URL`, `MARKET_SERVICE_URL`, `ORDER_SERVICE_URL`, `PORTFOLIO_SERVICE_URL`, `WALLET_SERVICE_URL`, `PAYMENT_SERVICE_URL`
- order-service only: `WALLET_SERVICE_URL`, `MARKET_SERVICE_URL` (Feign target)
- market-service only: `PRICE_FEED_ENABLED` (default `true`)
- payment-service only: `PAYMENT_GATEWAY` (default `MOCK`)
- ai-assistant-service only: `GROQ_API_KEY`, `GROQ_MODEL`, `QDRANT_URL` (compose maps it from `QDRANT_CLUSTER_ENDPOINT`), `QDRANT_API_KEY`, `QDRANT_COLLECTION` (default `alphatrade_knowledge`), `ASSISTANT_INGEST_ON_STARTUP` (default `true`)
- api-gateway also: `AI_ASSISTANT_SERVICE_URL`

**Frontend `services/frontend/.env.example`:**
```
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080
VITE_DEMO_EMAIL=
VITE_DEMO_PASSWORD=
```

---

## 9. Where to Look Next

- `ARCHITECTURE.md` (repo root) — deeper architectural background, diagrams, and design rationale.
- `helm/trading-platform/` — Kubernetes/Helm deployment path (production-oriented, not needed for local docker-compose).
- `shared/` Maven module — common enums (`OrderSide`, `OrderStatus`, `OrderType`, `ProductType`), Kafka event DTOs (`PriceUpdatedEvent`, `OrderFilledEvent`, `PaymentCompletedEvent`), and the `ApiResponse<T>` wrapper used by every controller.
