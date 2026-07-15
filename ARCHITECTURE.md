# alphaTrade — Microservices Trading Platform

> **Real-Time Stock & Crypto Trading Platform built on Java 21 + Spring Boot 3.4 + Kafka + Docker**

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [High-Level Architecture](#3-high-level-architecture)
4. [Project File Structure](#4-project-file-structure)
5. [Service-by-Service Breakdown](#5-service-by-service-breakdown)
6. [Shared Library](#6-shared-library)
7. [Database Design](#7-database-design)
8. [Message Bus — Kafka Topics](#8-message-bus--kafka-topics)
9. [Caching — Redis](#9-caching--redis)
10. [Security — JWT Auth Flow](#10-security--jwt-auth-flow)
11. [Complete API Reference](#11-complete-api-reference)
12. [Docker Infrastructure](#12-docker-infrastructure)
13. [Building and Running](#13-building-and-running)
14. [Unit Testing](#14-unit-testing)
15. [Implementation Plan](#15-implementation-plan)
16. [Common Issues Fixed](#16-common-issues-fixed)

---

## 1. Project Overview

**alphaTrade** is a full-stack trading platform backend simulating real-world stock & crypto trading using an event-driven microservices architecture.

### What it does:
- **User registration & authentication** with JWT (access + refresh tokens)
- **Real-time market data** with mock price feed (Geometric Brownian Motion) emitting prices every second via Kafka
- **Order placement** with a mock matching engine that fills orders asynchronously
- **Portfolio tracking** — holding positions, P&L, average buy price
- **Wallet & fund management** — balance, fund locking for orders, transaction ledger
- **Payment simulation** — mock deposit/withdrawal flows
- **In-app notifications** — trade execution & payment event notifications
- **AI assistant chatbot** — RAG-powered help (Groq LLM + Qdrant vector search) available on every page

### Tech Choice Philosophy:
- Every service is **fully independent** — its own DB, schema, and lifecycle
- Kafka decouples real-time price feeds from consumers
- Redis caches live prices to avoid querying across services
- API Gateway provides a single entry point with JWT validation before forwarding

---

## 2. Technology Stack

| Layer             | Technology                       | Version      |
|-------------------|----------------------------------|--------------|
| Language          | Java                             | 21 (LTS)     |
| Framework         | Spring Boot                      | 3.4.4        |
| Service Mesh      | Spring Cloud Gateway (WebFlux)   | 2024.0.1     |
| Service Clients   | Spring Cloud OpenFeign           | 2024.0.1     |
| Security          | Spring Security + JJWT           | 0.12.6       |
| ORM               | Spring Data JPA + Hibernate      | 6.x          |
| Database          | PostgreSQL                       | 16           |
| Migrations        | Flyway                           | 10.x         |
| Message Broker    | Apache Kafka (Confluent)         | 7.6.1        |
| Cache             | Redis                            | 7            |
| API Docs          | SpringDoc OpenAPI (Swagger UI)   | 2.8.6        |
| AI — LLM          | Groq (OpenAI-compatible API)     | llama-3.3-70b|
| AI — Vector DB    | Qdrant Cloud (+ Cloud Inference) | managed      |
| Build Tool        | Maven (Multi-module)             | 3.9.x        |
| Containerization  | Docker + Docker Compose          | Latest       |
| Testing           | JUnit 5 + Mockito + AssertJ      | via Spring   |
| Code Generation   | Lombok + MapStruct               | 1.18 / 1.6.3 |

---

## 3. High-Level Architecture

```
                    ┌────────────────────────────────────┐
                    │           API Gateway              │
                    │         (port 8080)                │
                    │  Spring Cloud Gateway (WebFlux)    │
                    │  • JWT validation (JwtAuthFilter)  │
                    │  • Route dispatch                  │
                    │  • Rate limiting (Redis)           │
                    │  • Swagger UI aggregator           │
                    └──────────────┬─────────────────────┘
                                   │  HTTP (Docker network)
       ┌───────────────────────────┼──────────────────────────┐
       │                           │                          │
┌──────▼──────┐        ┌───────────▼──────┐        ┌─────────▼────────┐
│auth-service │        │ market-service   │        │  order-service   │
│ port 8081   │        │  port 8082       │        │   port 8083      │
│             │        │                  │        │                  │
│ auth_db     │        │ market_db        │        │ order_db         │
│ PostgreSQL  │        │ Kafka Producer   │        │ Redis Consumer   │
└─────────────┘        │ Redis Writer     │        │ Feign→Wallet     │
                       │ WebSocket        │        │ MockMatchEngine  │
                       └──────────────────┘        └──────────────────┘

┌──────────────────┐  ┌────────────────────┐  ┌───────────────────┐
│ portfolio-service│  │  wallet-service    │  │ payment-service   │
│   port 8084      │  │   port 8085        │  │   port 8086       │
│                  │  │                    │  │                   │
│ portfolio_db     │  │ wallet_db          │  │ payment_db        │
│ Kafka Consumer   │  │ Kafka Consumer     │  │ Kafka Producer    │
└──────────────────┘  └────────────────────┘  └───────────────────┘

┌──────────────────┐  ┌────────────────────────────────────┐
│notification-svc  │  │ ai-assistant-service               │
│   port 8087      │  │   port 8088                        │
│                  │  │                                    │
│ notification_db  │  │ no DB (stateless)                  │
│ Kafka Consumer   │  │ Groq LLM + Qdrant Cloud (RAG chat) │
└──────────────────┘  └────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────┐
    │                Shared Infrastructure                │
    │                                                     │
    │  Redis :6379          Kafka :9092                   │
    │  Redis Insight :5540  Kafka UI :9000                │
    └─────────────────────────────────────────────────────┘
```

---

## 4. Project File Structure

```
alphaTrade/
├── pom.xml                      ← Parent Maven POM (multi-module)
├── docker-compose.yml           ← Full stack (21 containers)
├── .env.example                 ← Environment variable template
├── ARCHITECTURE.md              ← This document
│
├── shared/                      ← Shared internal library (JAR)
│   └── src/main/java/com/trading/shared/
│       ├── enums/               ← OrderSide, OrderStatus, OrderType, ProductType
│       ├── events/              ← PriceUpdatedEvent, OrderFilledEvent
│       ├── exception/           ← TradingException, ResourceNotFoundException
│       └── response/            ← ApiResponse<T> wrapper
│
└── services/
    ├── api-gateway/             ← Spring Cloud Gateway (WebFlux)
    ├── auth-service/            ← User registration, JWT, refresh tokens
    ├── market-service/          ← Live price feed, instruments, quotes
    ├── order-service/           ← Order placement, mock matching engine
    ├── portfolio-service/       ← Holdings, P&L tracking
    ├── wallet-service/          ← Balances, fund locking, ledger
    ├── payment-service/         ← Mock deposits, withdrawals
    ├── notification-service/    ← In-app notifications
    └── ai-assistant-service/    ← RAG chatbot (Groq LLM + Qdrant vector DB)
```

### Anatomy of a Single Microservice

Every microservice follows the same internal package layout:

```
services/<name>-service/
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/trading/<name>/
    │   │   ├── <Name>ServiceApplication.java   ← @SpringBootApplication
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java         ← per-service Spring Security
    │   │   │   ├── KafkaConfig.java            ← Kafka consumers/producers
    │   │   │   └── RedisConfig.java            ← RedisTemplate<String,Object>
    │   │   ├── controller/                     ← REST endpoints
    │   │   ├── service/                        ← Business logic
    │   │   ├── repository/                     ← Spring Data JPA interfaces
    │   │   ├── entity/                         ← JPA @Entity classes
    │   │   └── dto/                            ← Request/Response DTOs
    │   └── resources/
    │       ├── application.yml                 ← Spring config
    │       ├── application-docker.yml          ← Docker-profile overrides
    │       └── db/migration/
    │           └── V1__create_<table>.sql      ← Flyway schema migration
    └── test/
        └── java/com/trading/<name>/
            └── service/<Name>ServiceTest.java  ← JUnit 5 + Mockito
```

---

## 5. Service-by-Service Breakdown

### 5.1 API Gateway — Port 8080

**Role:** Single entry point. Validates JWT tokens and routes to downstream services.

**Stack:** Spring Cloud Gateway (reactive/WebFlux — non-blocking)

**Key Files:**
- `GatewayRoutesConfig.java` — Declares all route rules
- `JwtAuthFilter.java` — Gateway-level JWT validation (WebFlux GatewayFilter)
- `SecurityConfig.java` — Disables default Spring Security form login
- `RateLimiterConfig.java` — Redis-backed rate limiting

**Route Table:**

| Route               | Path Pattern                  | Auth     | Target Service      |
|---------------------|-------------------------------|----------|---------------------|
| `auth-register`     | POST /api/v1/auth/register    | Public   | auth-service:8081   |
| `auth-login`        | POST /api/v1/auth/login       | Public   | auth-service:8081   |
| `auth-refresh`      | POST /api/v1/auth/refresh     | Public   | auth-service:8081   |
| `auth-protected`    | /api/v1/auth/**               | JWT      | auth-service:8081   |
| `market-service`    | /api/v1/market/**             | JWT      | market-service:8082 |
| `order-service`     | /api/v1/orders/**             | JWT      | order-service:8083  |
| `portfolio-service` | /api/v1/portfolio/**          | JWT      | portfolio-svc:8084  |
| `wallet-service`    | /api/v1/wallet/**             | JWT      | wallet-service:8085 |
| `payment-protected` | /api/v1/payments/deposit|withdraw | JWT  | payment-svc:8086    |
| `payment-webhooks`  | /api/v1/payments/webhook/**   | Public   | payment-svc:8086    |
| `websocket`         | /ws/**                        | —        | market-service:8082 |
| `docs-*`            | /docs/{svc}/v3/api-docs       | —        | per-service         |

**Swagger UI (aggregated):** `http://localhost:8080/swagger-ui.html`

---

### 5.2 Auth Service — Port 8081

**Role:** User registration, login, JWT generation, token refresh, logout.

**Database:** `auth_db` (PostgreSQL port 5433)

**Schema (Flyway V1):**
```sql
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
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(512) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

**JWT Strategy:**
- **Access Token** — 15 minutes, HMAC-SHA384 signed, contains `userId`, `email`, `role`
- **Refresh Token** — 7 days, stored as BCrypt hash in DB (not plaintext)
- On login: revoke all previous refresh tokens, issue a new one
- On logout: revoke all refresh tokens for that user

**Default Seed Admin:**
```
email:    admin@trading.dev
password: Admin@1234
role:     ROLE_ADMIN
```

---

### 5.3 Market Service — Port 8082

**Role:** Instrument catalog + real-time mock price feed.

**Database:** `market_db` (PostgreSQL port 5434)

**Seeded Instruments (9 total):**

| Symbol   | Name                    | Type   | Seed Price  | Currency |
|----------|-------------------------|--------|-------------|----------|
| RELIANCE | Reliance Industries Ltd | STOCK  | Rs 2,850.00 | INR      |
| TCS      | Tata Consultancy Svc    | STOCK  | Rs 3,900.00 | INR      |
| INFY     | Infosys Ltd             | STOCK  | Rs 1,720.00 | INR      |
| HDFCBANK | HDFC Bank Ltd           | STOCK  | Rs 1,650.00 | INR      |
| WIPRO    | Wipro Ltd               | STOCK  | Rs   540.00 | INR      |
| BTC      | Bitcoin                 | CRYPTO | $68,000.00  | USDT     |
| ETH      | Ethereum                | CRYPTO | $ 3,800.00  | USDT     |
| SOL      | Solana                  | CRYPTO | $   185.00  | USDT     |
| USDT     | Tether USD              | CRYPTO | $     1.00  | USDT     |

**Mock Price Feed Algorithm — Geometric Brownian Motion:**
```
nextPrice = lastPrice x (1 + drift + volatility x Z)

drift      = 0.0001   (slight upward bias)
volatility = 0.005    (+/-0.5% per tick)
Z          = random Gaussian N(0,1)
tick rate  = every 1 second
```

On each tick, for all 9 instruments:
1. Compute new price
2. Update OHLC (Open/High/Low/Close) in-memory maps
3. Publish `PriceUpdatedEvent` to Kafka topic `market.price.updates`

**Redis Integration:**
`PriceUpdateConsumer` (Kafka listener) writes latest price to Redis:
```
Key:   price:{SYMBOL}       e.g.  price:BTC
Value: PriceUpdatedEvent    (JSON)
TTL:   5 seconds
```
The `order-service` reads from Redis when placing orders — no direct HTTP call to market-service.

---

### 5.4 Order Service — Port 8083

**Role:** Accepts buy/sell orders, locks funds, asynchronously fills via mock matching engine.

**Database:** `order_db` (PostgreSQL port 5435)

**Schema (Flyway V1):**
```sql
CREATE TABLE orders (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    instrument_id   UUID         NOT NULL,
    symbol          VARCHAR(20)  NOT NULL,
    order_type      VARCHAR(20)  NOT NULL,    -- MARKET | LIMIT | STOP_LOSS
    side            VARCHAR(10)  NOT NULL,    -- BUY | SELL
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    product_type    VARCHAR(20)  NOT NULL DEFAULT 'CNC',
    quantity        NUMERIC(20,8) NOT NULL,
    price           NUMERIC(20,8),            -- null for MARKET orders
    stop_price      NUMERIC(20,8),
    filled_quantity NUMERIC(20,8) NOT NULL DEFAULT 0,
    average_price   NUMERIC(20,8),
    fee             NUMERIC(20,8) NOT NULL DEFAULT 0,
    reject_reason   VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

**Order Placement Flow:**
```
Client  POST /api/v1/orders
  |
  +- 1. Fetch live price from Redis (key: price:{SYMBOL})
  +- 2. Compute lock amount = quantity x price
  +- 3. Save Order (status = OPEN) to order_db
  +- 4. [BUY only] Feign call to wallet-service.lockFunds()
  |       If wallet rejects → Order marked REJECTED
  +- 5. MockMatchingEngine.matchAsync(order, marketPrice)
           Async: fills order → publishes OrderFilledEvent to Kafka
```

**Inter-Service Communication via OpenFeign:**
```java
@FeignClient(name = "wallet-service", url = "${wallet.service.url}")
public interface WalletFeignClient {
    void lockFunds(LockFundsRequest request);
    void unlockFunds(LockFundsRequest request);
}
```

---

### 5.5 Portfolio Service — Port 8084

**Role:** Tracks user holdings and calculates P&L.

**Database:** `portfolio_db` (PostgreSQL port 5436)

**Data Flow:**
Consumes `OrderFilledEvent` from Kafka:
- BUY fills → increase holding quantity, recalculate weighted average buy price
- SELL fills → decrease holding quantity, calculate realized P&L

---

### 5.6 Wallet Service — Port 8085

**Role:** Multi-currency balances, fund locking for open orders, transaction ledger.

**Database:** `wallet_db` (PostgreSQL port 5437)

**Fund Locking Logic:**
- Order placed:    `balance -= lockAmount`, `locked_balance += lockAmount`
- Order cancelled: `locked_balance -= lockAmount`, `balance += lockAmount`
- Order filled:    `locked_balance -= lockAmount`, asset credited to portfolio

---

### 5.7 Payment Service — Port 8086

**Role:** Mock payment gateway for deposits and withdrawals. All transactions auto-succeed in dev mode.

**Database:** `payment_db` (PostgreSQL port 5438)

**Deposit Flow:**
```
Client  POST /api/v1/payments/deposit
  +- 1. Create PaymentOrder (status = PENDING)
  +- 2. Simulate gateway (mock → always COMPLETED)
  +- 3. Update PaymentOrder (status = COMPLETED)
  +- 4. Publish PaymentCompletedEvent to Kafka
           wallet-service consumes → credits balance
```

---

### 5.8 Notification Service — Port 8087

**Role:** Persists and serves in-app notifications.

**Database:** `notification_db` (PostgreSQL port 5439)

**Events Consumed:**
- `OrderFilledEvent` → "Your order for 0.05 BTC filled at $71,234"
- `PaymentCompletedEvent` → "Rs 15,000 deposited to your wallet"

**States:** `UNREAD` → `READ`

---

### 5.9 AI Assistant Service — Port 8088

**Role:** In-app AI chatbot ("Alpha") that answers questions about using the platform (funding, orders, portfolio) and how it works internally, via Retrieval-Augmented Generation.

**Database:** none — the service is stateless; conversation history is held by the client and passed with each request.

**How it works:**

1. A markdown knowledge base bundled in `src/main/resources/knowledge/*.md` is chunked per `##` section and upserted into a **Qdrant Cloud** collection (`alphatrade_knowledge`) on startup. Embeddings are computed server-side by **Qdrant Cloud Inference** (`sentence-transformers/all-minilm-l6-v2`, 384-dim, cosine) — Groq has no embeddings API, so no local embedding model is needed. Point ids are deterministic, so re-ingestion is idempotent.
2. On `POST /api/v1/assistant/chat`, the user's question is semantically searched against the collection (top-4, score ≥ 0.30), the matching doc sections are injected into the system prompt, and the answer is generated by a **Groq**-hosted LLM (default `llama-3.3-70b-versatile`) via its OpenAI-compatible chat-completions API.
3. The response includes the reply plus the retrieved sources (title, file, score). If Qdrant is unreachable, the service degrades gracefully and answers without RAG context.

**Endpoints:** `POST /assistant/chat` (protected via gateway JWT), `POST /assistant/reindex` (re-ingest the knowledge base).

**Config (env):** `GROQ_API_KEY`, `GROQ_MODEL`, `QDRANT_URL` / `QDRANT_CLUSTER_ENDPOINT`, `QDRANT_API_KEY`.

**Frontend:** a floating chat widget (`components/assistant/AssistantWidget.tsx`) rendered on every authenticated page.

---

## 6. Shared Library

The `shared` Maven module is a common internal JAR compiled into every service.

### ApiResponse — Standardized Response Shape

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": { "..." },
  "timestamp": "2026-06-28T19:55:35Z"
}
```

### Shared Enums

| Enum          | Values                                                       |
|---------------|--------------------------------------------------------------|
| `OrderSide`   | `BUY`, `SELL`                                                |
| `OrderStatus` | `OPEN`, `FILLED`, `PARTIALLY_FILLED`, `CANCELLED`, `REJECTED`|
| `OrderType`   | `MARKET`, `LIMIT`, `STOP_LOSS`                               |
| `ProductType` | `CASH`, `INTRADAY`, `DELIVERY`                               |

### Shared Kafka Events

| Event                  | Key Fields                                                                   |
|------------------------|------------------------------------------------------------------------------|
| `PriceUpdatedEvent`    | symbol, ltp, open, high, low, close, change, changePct, volume, timestamp   |
| `OrderFilledEvent`     | orderId, userId, symbol, side, quantity, filledPrice, fee                    |
| `PaymentCompletedEvent`| paymentId, userId, amount, currency                                          |

### Shared Exceptions

- `TradingException` — custom exception with `errorCode` (String) + `httpStatus` (int)
- `ResourceNotFoundException` — 404 with entity name and ID

---

## 7. Database Design

### One Database Per Service

| Service       | DB Name           | Port | Tables                         |
|---------------|-------------------|------|--------------------------------|
| auth-service  | auth_db           | 5433 | users, refresh_tokens          |
| market-service| market_db         | 5434 | instruments                    |
| order-service | order_db          | 5435 | orders                         |
| portfolio-svc | portfolio_db      | 5436 | holdings, portfolio_summary    |
| wallet-service| wallet_db         | 5437 | wallets, wallet_ledger         |
| payment-svc   | payment_db        | 5438 | payment_orders                 |
| notification  | notification_db   | 5439 | notifications                  |

### Schema Management with Flyway

All migrations live in: `services/<name>/src/main/resources/db/migration/`

```
V1__create_users_and_refresh_tokens.sql   (auth-service)
V1__create_instruments.sql                (market-service)
V2__seed_instruments.sql                  (market-service — seed data)
V1__create_orders.sql                     (order-service)
V1__create_portfolio.sql                  (portfolio-service)
V1__create_wallets.sql                    (wallet-service)
V1__create_payment_orders.sql             (payment-service)
V1__create_notifications.sql              (notification-service)
```

**Important:** `spring.jpa.hibernate.ddl-auto: validate` — Hibernate validates schema against entities but Flyway owns all DDL.

### Connect to DB Directly

```bash
# Auth DB
psql -h localhost -p 5433 -U trading -d auth_db

# Order DB
psql -h localhost -p 5435 -U trading -d order_db

# Market DB
psql -h localhost -p 5434 -U trading -d market_db

# Password: trading_secret
```

---

## 8. Message Bus — Kafka Topics

Kafka auto-creates all topics (`KAFKA_AUTO_CREATE_TOPICS_ENABLE: true`).

| Topic                  | Producer       | Consumers                                          |
|------------------------|----------------|----------------------------------------------------|
| market.price.updates   | market-service | market-service (Redis writer), order, portfolio    |
| order.filled           | order-service  | portfolio-service, wallet-service, notification    |
| payment.completed      | payment-service| wallet-service, notification-service               |

**Kafka UI:** `http://localhost:9000`

**Live stream messages:**
```bash
docker exec -it trading-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic market.price.updates \
  --from-beginning
```

---

## 9. Caching — Redis

### Live Price Cache

```
Key format:   price:{SYMBOL}         e.g. price:BTC
Value:        PriceUpdatedEvent      (JSON serialized)
TTL:          5 seconds
Writer:       market-service (PriceUpdateConsumer via Kafka)
Reader:       order-service (when placing orders)
```

This avoids synchronous HTTP calls between order-service and market-service on the hot path.

### Rate Limiting

API Gateway stores per-user request counters in Redis (Spring Cloud Gateway RequestRateLimiter).

**Redis Insight:** `http://localhost:5540`

---

## 10. Security — JWT Auth Flow

### Flow Diagram

```
1. Register / Login
   Client  POST /api/v1/auth/register
   Client  POST /api/v1/auth/login
            Returns { accessToken, refreshToken }

2. Calling Protected Endpoints
   Client  GET /api/v1/orders
           Authorization: Bearer <accessToken>
            |
            +-- API Gateway JwtAuthFilter (WebFlux)
                  Extract Bearer token
                  Parse & validate HMAC-SHA384 signature
                  Check token expiry
                  Inject X-User-Id header
                  Forward to order-service

3. Downstream Service
   Reads X-User-Id from header (injected by gateway)
   All queries filtered by userId (data isolation)

4. Token Refresh
   Client  POST /api/v1/auth/refresh { refreshToken }
            auth-service validates hash against DB
            Issues new accessToken + refreshToken

5. Logout
   Client  POST /api/v1/auth/logout
            auth-service marks all refresh tokens revoked
```

### JWT Payload Structure

```json
{
  "jti": "uuid-for-blacklisting",
  "sub": "user-uuid",
  "email": "alice@example.com",
  "role": "ROLE_USER",
  "type": "ACCESS",
  "iat": 1782676535,
  "exp": 1782677435
}
```

Token validity:
- Access token: **15 minutes**
- Refresh token: **7 days**

---

## 11. Complete API Reference

> **Base URL:** `http://localhost:8080`
> Protected endpoints require: `Authorization: Bearer <accessToken>`

---

### AUTH SERVICE (/api/v1/auth)

#### Register User

```
POST /api/v1/auth/register
Content-Type: application/json

{
  "fullName": "Alice Smith",
  "email": "alice@example.com",
  "password": "SecurePass123",
  "phone": "9876543210"
}
```

Response 201:
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "uuid",
      "email": "alice@example.com",
      "fullName": "Alice Smith",
      "role": "ROLE_USER",
      "kycStatus": "VERIFIED",
      "active": true
    }
  }
}
```

#### Login

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "SecurePass123"
}
```

Response 200: Same shape as register.

#### Refresh Token

```
POST /api/v1/auth/refresh
Content-Type: application/json

{ "refreshToken": "eyJhbGci..." }
```

#### Get Profile (Protected)

```
GET /api/v1/auth/me
Authorization: Bearer <accessToken>
```

#### Logout (Protected)

```
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
```

---

### MARKET SERVICE (/api/v1/market)

#### List All Instruments (Protected)

```
GET /api/v1/market/instruments
GET /api/v1/market/instruments?type=STOCK
GET /api/v1/market/instruments?type=CRYPTO
Authorization: Bearer <accessToken>
```

#### Get Instrument by Symbol (Protected)

```
GET /api/v1/market/instruments/symbol/BTC
Authorization: Bearer <accessToken>
```

#### Search Instruments (Protected)

```
GET /api/v1/market/instruments/search?q=bitcoin
Authorization: Bearer <accessToken>
```

#### Get Live Quote (Protected)

```
GET /api/v1/market/quotes/BTC
Authorization: Bearer <accessToken>
```

Response:
```json
{
  "success": true,
  "data": {
    "symbol": "BTC",
    "ltp": 71234.56,
    "open": 68000.00,
    "high": 72100.00,
    "low": 67800.00,
    "change": 3234.56,
    "changePct": 4.76,
    "volume": 12850
  }
}
```

---

### ORDER SERVICE (/api/v1/orders)

#### Place Market Order (Protected)

```
POST /api/v1/orders
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "instrumentId": "uuid-from-instruments-api",
  "symbol": "BTC",
  "orderType": "MARKET",
  "side": "BUY",
  "productType": "CASH",
  "quantity": 0.05
}
```

#### Place Limit Order (Protected)

```
POST /api/v1/orders
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "instrumentId": "uuid-from-instruments-api",
  "symbol": "RELIANCE",
  "orderType": "LIMIT",
  "side": "BUY",
  "productType": "CASH",
  "quantity": 10,
  "price": 2700.00
}
```

Response 201:
```json
{
  "success": true,
  "data": {
    "id": "order-uuid",
    "symbol": "BTC",
    "orderType": "MARKET",
    "side": "BUY",
    "status": "OPEN",
    "quantity": 0.05,
    "createdAt": "2026-06-28T20:00:00Z"
  }
}
```

#### Get Order History (Protected)

```
GET /api/v1/orders?page=0&size=20
Authorization: Bearer <accessToken>
```

#### Get Open Orders (Protected)

```
GET /api/v1/orders/open
Authorization: Bearer <accessToken>
```

#### Cancel Order (Protected)

```
DELETE /api/v1/orders/{orderId}
Authorization: Bearer <accessToken>
```

---

### PORTFOLIO SERVICE (/api/v1/portfolio)

#### Get Holdings (Protected)

```
GET /api/v1/portfolio/holdings
Authorization: Bearer <accessToken>
```

#### Get Portfolio Summary (Protected)

```
GET /api/v1/portfolio/summary
Authorization: Bearer <accessToken>
```

Response:
```json
{
  "success": true,
  "data": {
    "totalInvested": 15000.00,
    "currentValue": 16200.00,
    "unrealizedPnl": 1200.00,
    "realizedPnl": 300.00
  }
}
```

---

### WALLET SERVICE (/api/v1/wallet)

#### Get All Balances (Protected)

```
GET /api/v1/wallet
Authorization: Bearer <accessToken>
```

#### Get Balance for Currency (Protected)

```
GET /api/v1/wallet/INR
GET /api/v1/wallet/USDT
Authorization: Bearer <accessToken>
```

#### Get Transaction Ledger (Protected)

```
GET /api/v1/wallet/ledger?page=0&size=20
Authorization: Bearer <accessToken>
```

---

### PAYMENT SERVICE (/api/v1/payments)

#### Deposit Funds (Protected)

```
POST /api/v1/payments/deposit
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "amount": 15000.00,
  "currency": "INR"
}
```

#### Withdraw Funds (Protected)

```
POST /api/v1/payments/withdraw
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "amount": 5000.00,
  "currency": "INR"
}
```

#### Payment History (Protected)

```
GET /api/v1/payments/history?page=0&size=20
Authorization: Bearer <accessToken>
```

---

### NOTIFICATION SERVICE (/api/v1/notifications)

#### Get Notifications (Protected)

```
GET /api/v1/notifications?page=0&size=20
Authorization: Bearer <accessToken>
```

#### Get Unread Count (Protected)

```
GET /api/v1/notifications/unread-count
Authorization: Bearer <accessToken>
```

#### Mark Single Notification as Read (Protected)

```
PATCH /api/v1/notifications/{id}/read
Authorization: Bearer <accessToken>
```

#### Mark All as Read (Protected)

```
PATCH /api/v1/notifications/read-all
Authorization: Bearer <accessToken>
```

---

### AI ASSISTANT SERVICE (/api/v1/assistant)

#### Chat with the Assistant (Protected)

```
POST /api/v1/assistant/chat
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "message": "How do I place a limit order?",
  "history": [
    { "role": "user", "content": "..." },
    { "role": "assistant", "content": "..." }
  ]
}
```

Response 200:

```json
{
  "success": true,
  "data": {
    "reply": "To place a LIMIT order...",
    "model": "llama-3.3-70b-versatile",
    "sources": [
      { "title": "How to place an order", "source": "trading-and-orders.md", "score": 0.64 }
    ]
  }
}
```

`history` is optional — the service is stateless and the client passes prior turns back on each call.

#### Re-index the Knowledge Base (Protected)

```
POST /api/v1/assistant/reindex
Authorization: Bearer <accessToken>
```

---

## 12. Docker Infrastructure

### All 21 Containers

| Container                    | Image                        | Port | Purpose               |
|------------------------------|------------------------------|------|-----------------------|
| trading-api-gateway          | alphatrade-api-gateway       | 8080 | API Gateway           |
| trading-auth-service         | alphatrade-auth-service      | 8081 | Auth Service          |
| trading-market-service       | alphatrade-market-service    | 8082 | Market Service        |
| trading-order-service        | alphatrade-order-service     | 8083 | Order Service         |
| trading-portfolio-service    | alphatrade-portfolio-service | 8084 | Portfolio Service     |
| trading-wallet-service       | alphatrade-wallet-service    | 8085 | Wallet Service        |
| trading-payment-service      | alphatrade-payment-service   | 8086 | Payment Service       |
| trading-notification-service | alphatrade-notification-svc  | 8087 | Notification Service  |
| trading-ai-assistant-service | alphatrade-ai-assistant-svc  | 8088 | AI Assistant (chatbot)|
| trading-postgres-auth        | postgres:16-alpine           | 5433 | Auth DB               |
| trading-postgres-market      | postgres:16-alpine           | 5434 | Market DB             |
| trading-postgres-order       | postgres:16-alpine           | 5435 | Order DB              |
| trading-postgres-portfolio   | postgres:16-alpine           | 5436 | Portfolio DB          |
| trading-postgres-wallet      | postgres:16-alpine           | 5437 | Wallet DB             |
| trading-postgres-payment     | postgres:16-alpine           | 5438 | Payment DB            |
| trading-postgres-notification| postgres:16-alpine           | 5439 | Notification DB       |
| trading-redis                | redis:7-alpine               | 6379 | Cache + Rate Limit    |
| trading-redis-insight        | redis/redisinsight           | 5540 | Redis Dashboard       |
| trading-kafka                | confluentinc/cp-kafka:7.6.1  | 9092 | Kafka Broker          |
| trading-zookeeper            | confluentinc/cp-zookeeper    | 2181 | Kafka Coordinator     |
| trading-kafka-ui             | ghcr.io/kafbat/kafka-ui      | 9000 | Kafka Dashboard       |

### Docker Compose Profiles

The compose file uses profiles to separate infrastructure from application services:

```bash
# Infrastructure only (databases, Redis, Kafka, dashboards):
docker compose up -d

# Full stack (all 21 containers):
docker compose --profile full up -d --build

# Stop infrastructure only (microservices remain running!):
docker compose down

# Stop EVERYTHING:
docker compose --profile full down

# Full reset (deletes all data):
docker compose --profile full down -v
```

### Dockerfile Strategy

All Dockerfiles use a single-stage runtime build — JARs are pre-built on the host:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY services/auth-service/target/auth-service-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

This avoids Maven downloading dependencies inside Docker (network timeouts in multi-stage builds).

---

## 13. Building and Running

### Prerequisites

- Java 21 (OpenJDK or Temurin)
- Maven 3.9+
- Docker + Docker Compose v2

### Step 1 — Build all JARs on host

```bash
cd alphaTrade
mvn clean package -DskipTests
```

Build time: ~15 seconds.

### Step 2 — Start infrastructure

```bash
docker compose up -d
```

Wait ~15 seconds for health checks to pass.

### Step 3 — Start microservices

```bash
docker compose --profile full up -d --build
```

Wait 30-60 seconds for Spring Boot to complete Flyway migrations and JPA initialization.

### Step 4 — Verify

```bash
docker ps
curl http://localhost:8080/actuator/health
```

### Step 5 — Test registration

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Alice Smith","email":"alice@example.com","password":"SecurePass123"}'
```

### Developer Tool URLs

| Tool          | URL                                     |
|---------------|-----------------------------------------|
| Swagger UI    | http://localhost:8080/swagger-ui.html   |
| Kafka UI      | http://localhost:9000                   |
| Redis Insight | http://localhost:5540                   |

---

## 14. Unit Testing

### Testing Stack

- **JUnit 5** — test lifecycle, assertions
- **Mockito** — mocking repository/service dependencies
- **AssertJ** — fluent assertions (`assertThat(...)`)
- `@ExtendWith(MockitoExtension.class)` — no Spring context = fast tests

### Running Tests

```bash
# All tests
mvn test

# Single service
mvn test -pl services/auth-service

# Single class
mvn test -pl services/auth-service -Dtest=AuthServiceTest

# With integration tests
mvn verify -P integration-tests
```

### Existing Test Coverage

#### Auth Service — AuthServiceTest (8 tests)

| Test Method                                | Scenario                                     |
|--------------------------------------------|----------------------------------------------|
| register_ShouldCreateUserAndReturnTokens   | Happy path: valid registration returns tokens|
| register_ShouldThrow409WhenEmailExists     | Duplicate email → EMAIL_ALREADY_EXISTS (409) |
| register_ShouldSetKycStatusToVerified      | Mock KYC always returns VERIFIED             |
| login_ShouldReturnTokens_WhenCredentialsValid | Valid login returns tokens + revokes old   |
| login_ShouldThrow_WhenBadCredentials       | Wrong password → BadCredentialsException     |
| logout_ShouldRevokeAllRefreshTokens        | Logout revokes all tokens for user           |
| getProfile_ShouldReturnUserDto             | Returns mapped UserDto from entity           |
| getProfile_ShouldThrow404WhenUserNotFound  | Unknown userId → TradingException (404)      |

#### Market Service — MockPriceFeedServiceTest

Tests for price feed initialization, tick execution, and OHLC tracking.

#### Payment Service — PaymentServiceTest

Tests for deposit and withdrawal flows.

### Template for New Tests

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Unit Tests")
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletLedgerRepository ledgerRepository;

    @InjectMocks private WalletService walletService;

    @Test
    @DisplayName("getBalance: should return INR balance for user")
    void getBalance_ShouldReturnBalance() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency("INR")
                .balance(new BigDecimal("10000"))
                .build();

        when(walletRepository.findByUserIdAndCurrency(userId, "INR"))
                .thenReturn(Optional.of(wallet));

        BigDecimal balance = walletService.getBalance(userId, "INR");

        assertThat(balance).isEqualByComparingTo("10000");
    }
}
```

---

## 15. Implementation Plan

### COMPLETED

**Phase 1 — Foundation**
- [x] Multi-module Maven project structure
- [x] Shared library (enums, events, exceptions, ApiResponse)
- [x] Parent pom.xml with full dependency management
- [x] Docker Compose with all 21 containers
- [x] Docker network renamed to alphatrade-network

**Phase 2 — Auth Service**
- [x] User registration with BCrypt password hashing
- [x] JWT access token (15 min) + refresh token (7 days)
- [x] Refresh token rotation on login/refresh
- [x] Logout = revoke all refresh tokens
- [x] GET /api/v1/auth/me profile endpoint
- [x] Flyway schema (users + refresh_tokens)
- [x] Unit tests (8 test cases)

**Phase 3 — API Gateway**
- [x] Spring Cloud Gateway route configuration
- [x] JWT GatewayJwtAuthFilter (WebFlux reactive)
- [x] Public vs protected route distinction
- [x] Swagger UI aggregator (all services in one UI)
- [x] Rate limiter config (Redis-backed)
- [x] Request logging filter

**Phase 4 — Market Service**
- [x] Instrument catalog (9 instruments seeded via Flyway)
- [x] Mock price feed (Geometric Brownian Motion)
- [x] Prices published to Kafka every 1 second
- [x] Redis price cache writer
- [x] REST endpoints: list, search, symbol lookup, live quote

**Phase 5 — Order Service**
- [x] Order placement (MARKET, LIMIT, STOP_LOSS)
- [x] Live price fetch from Redis
- [x] Fund locking via Feign → wallet-service
- [x] MockMatchingEngine (async fill)
- [x] Order cancellation with fund unlock
- [x] Order history + open orders endpoints

**Phase 6 — Infrastructure Fixes**
- [x] Fixed invalid JWT Base64 fallback in docker-compose
- [x] Fixed missing RedisConfig bean in order-service
- [x] Simplified all Dockerfiles to single-stage
- [x] Removed invalid -XX:+UseVirtualThreads JVM flag
- [x] Added spring-boot:repackage to parent pom.xml

### NEXT STEPS

**Phase 7 — Portfolio Service**
- [ ] Holdings entity with weighted avg buy price tracking
- [ ] P&L calculation (unrealized + realized)
- [ ] Kafka consumer for OrderFilledEvent
- [ ] REST endpoints: holdings, portfolio summary

**Phase 8 — Wallet Service**
- [ ] Multi-currency wallet (INR, USDT, BTC, ETH, SOL)
- [ ] Fund lock/unlock mechanics
- [ ] Transaction ledger API
- [ ] Kafka consumer for PaymentCompletedEvent

**Phase 9 — Payment Service**
- [ ] Full deposit/withdrawal flow
- [ ] Mock payment gateway (auto-success)
- [ ] Publish PaymentCompletedEvent to Kafka

**Phase 10 — Notification Service**
- [ ] Persist notifications from Kafka events
- [ ] REST API: list, unread count, mark read

**Phase 11 — Production Readiness (Future)**
- [ ] Helm charts for Kubernetes
- [ ] Centralized logging (Grafana Loki / ELK)
- [ ] Distributed tracing (OpenTelemetry + Jaeger)
- [ ] Integration tests with Testcontainers
- [ ] GitHub Actions CI pipeline
- [ ] WebSocket live price streaming to frontend
- [ ] Real payment gateway (Razorpay / Stripe)
- [ ] KYC verification flow

---

## 16. Common Issues Fixed

| Issue | Root Cause | Resolution |
|-------|-----------|------------|
| GATEWAY_INTERNAL_ERROR on /register | auth-service crashed at startup due to invalid JWT secret | Changed docker-compose JWT_SECRET fallback to valid Base64 string |
| DecodingException: Illegal base64 character '-' | Docker default JWT secret contained hyphens (not valid Base64) | Used Base64-encoded value as default fallback |
| order-service crash: No qualifying bean RedisTemplate | Spring Boot does not auto-create RedisTemplate with generic types | Created RedisConfig.java in order-service with explicit @Bean |
| auth-service DNS: UnknownHostException auth-service | Microservices use "full" Docker profile; plain docker compose down leaves them dangling | Always use docker compose --profile full down to stop everything |
| JVM crash: Unrecognized VM option UseVirtualThreads | Not a valid JVM flag (virtual threads enabled in app code, not via JVM flags) | Removed from all service Dockerfiles |
| 40KB thin JARs instead of fat JARs | Custom parent POM without spring-boot-starter-parent skips repackage | Added explicit repackage execution to spring-boot-maven-plugin |
| docker compose down leaves microservices running | Microservices are in profiles:["full"] — plain down only stops default profile | Documented: always use --profile full or --profile "*" |

---

*alphaTrade v1.0.0-SNAPSHOT | Generated: 2026-07-04*
