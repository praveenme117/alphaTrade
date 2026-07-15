# Wallet, Payments, Portfolio and Notifications

## Wallet and balances
Your wallet holds balances per currency (e.g. INR). Each balance has an available amount and a locked amount — funds get locked when you place an order and are settled or released when the order fills or is cancelled. The Wallet page shows balances and a full transaction ledger (every credit, debit, lock and unlock).

## Deposits and withdrawals
Deposits and withdrawals are made from the Wallet page and are processed by a mock payment gateway — they always succeed and no real money moves. After a deposit completes, the amount is credited to your wallet within a few seconds (it flows through a PaymentCompletedEvent on Kafka) and you get a notification. Payment history is available on the Wallet page.

## Portfolio and P&L
The Portfolio page shows your holdings: quantity, average buy price, current value at live prices, and profit & loss. Realized P&L is what you've locked in by selling; unrealized P&L is the paper gain/loss on positions you still hold, computed against live prices. Holdings are built automatically from your filled orders — buying increases quantity and recalculates your average price, selling reduces quantity and realizes P&L.

## Notifications
The bell icon shows in-app notifications: order fills and completed payments. You can mark individual notifications as read or mark all as read on the Notifications page. Notifications are generated automatically from Kafka events — there is no email or SMS in this demo.
