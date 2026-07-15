# alphaTrade Platform Overview

## What is alphaTrade
alphaTrade is a demo real-time stock & crypto trading platform. All money on the platform is mock/virtual: deposits and withdrawals always succeed through a simulated payment gateway, and no real funds or real payment providers are involved. Market prices are simulated with a random-walk (Geometric Brownian Motion) price feed over 9 seeded instruments (stocks, crypto, ETFs). It is built for learning and testing, not for real trading.

## Architecture at a glance
alphaTrade is built as Java 21 / Spring Boot microservices behind a single API Gateway (port 8080), with a React + TypeScript frontend. Each service owns its own Postgres database and services communicate over REST and Kafka events. The services are: auth-service (login/registration, JWT), market-service (instruments, live quotes, WebSocket price stream), order-service (placing/cancelling orders, matching engine), portfolio-service (holdings and P&L), wallet-service (balances and ledger), payment-service (mock deposits/withdrawals), notification-service (in-app notifications), and ai-assistant-service (this chatbot, powered by Groq LLM + Qdrant vector search).

## Live prices
market-service publishes a simulated price tick for every instrument every few seconds. The frontend receives live prices over a WebSocket connection (/ws/prices), so quotes on the Market, Trade, and Portfolio pages update in real time. The latest price of each symbol is also cached in Redis for fast order pricing.

## Security model
Users authenticate with email + password. Login returns a short-lived access token (JWT) and a refresh token. Every API call goes through the API Gateway, which validates the JWT. The frontend refreshes the session automatically when the access token expires. Logging out revokes all refresh tokens for the account.

## The AI assistant
The chat assistant ("Alpha") answers questions about how to use alphaTrade — funding your wallet, placing orders, understanding your portfolio — and about how the platform is built. It retrieves relevant documentation from a Qdrant vector database and generates answers with a Groq-hosted LLM. It never gives financial advice and cannot place orders or move money on your behalf.
