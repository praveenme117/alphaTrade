# Trading and Orders on alphaTrade

## Order types
alphaTrade supports three order types:
- MARKET — executes immediately at the current live price. No price input needed.
- LIMIT — you set a price; the order fills at your limit price (the mock matching engine fills it shortly after placement in this demo).
- STOP_LOSS — you set a stop price; used to exit a position when the price moves against you.
Each order also has a side (BUY or SELL) and a product type: CASH, INTRADAY, or DELIVERY.

## How to place an order
1. Fund your wallet first (Wallet page → Deposit; deposits are mock and always succeed).
2. Go to the Market page, pick an instrument (e.g. AAPL, BTC), or open the Trade page directly.
3. Choose BUY or SELL, the order type (MARKET/LIMIT/STOP_LOSS), product type, and quantity. For LIMIT orders enter a price; for STOP_LOSS enter a stop price.
4. Submit. The platform locks the required funds in your wallet, then the matching engine fills the order asynchronously (usually within a few seconds in this demo).
5. Track it on the Orders page; once filled it appears in your Portfolio.

## What happens behind the scenes when an order fills
When you place an order, order-service synchronously asks wallet-service to lock the required funds. The order is accepted and a mock matching engine fills it asynchronously at the live price. On fill, an OrderFilledEvent is published to Kafka: portfolio-service updates your holdings and average price, wallet-service settles the locked funds, and notification-service creates an in-app "order filled" notification.

## Cancelling orders
Open (not yet filled) orders can be cancelled from the Orders page (or DELETE /api/v1/orders/{id}). Cancelling unlocks the funds that were reserved for the order. Filled orders cannot be cancelled.

## Why did my order fail
Common reasons an order is rejected:
- Insufficient wallet balance to lock funds (deposit first on the Wallet page).
- Selling more quantity than you hold.
- Invalid quantity or missing price for LIMIT/STOP_LOSS orders.
The Orders page shows the order status; REJECTED orders include the failure reason.
