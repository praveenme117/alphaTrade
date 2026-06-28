package com.trading.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Strategy interface — all payment gateways implement this.
 *
 * Implementations:
 *   MockPaymentGateway   — always succeeds (used for dev/test)
 *   StripeGateway        — Stripe API stub (no real calls)
 *   RazorpayGateway      — Razorpay API stub (no real calls)
 *
 * Real implementations would call the respective SDK / REST API.
 * All are MOCKED for this project (no real money, no compliance).
 */
public interface PaymentGatewayPort {

    /** Unique gateway name — used to select the implementation */
    String gatewayName();

    /**
     * Initiate a deposit (funds coming IN).
     * Returns a gateway transaction reference on success.
     */
    GatewayResult deposit(UUID userId, BigDecimal amount, String currency,
                          String idempotencyKey, String description);

    /**
     * Initiate a withdrawal (funds going OUT).
     * Returns a gateway transaction reference on success.
     */
    GatewayResult withdraw(UUID userId, BigDecimal amount, String currency,
                           String bankReference, String idempotencyKey);

    /**
     * Result from a gateway call — success or failure with reason.
     */
    record GatewayResult(
            boolean success,
            String gatewayReference,   // gateway's own txn ID (e.g., pi_xxx for Stripe)
            String response,           // raw response / error message
            String errorCode           // null on success
    ) {
        public static GatewayResult success(String ref, String response) {
            return new GatewayResult(true, ref, response, null);
        }
        public static GatewayResult failure(String errorCode, String reason) {
            return new GatewayResult(false, null, reason, errorCode);
        }
    }
}
