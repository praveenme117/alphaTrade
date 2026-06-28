package com.trading.payment.gateway.impl;

import com.trading.payment.gateway.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mock Payment Gateway — simulates Stripe/Razorpay behaviour.
 *
 * Rules:
 *   - Always succeeds for amounts between ₹1 and ₹10,00,000
 *   - Simulates 50–200ms network latency
 *   - Returns a mock gateway reference like MOCK-TXN-{uuid}
 *   - Fails for amounts < 1 (invalid) or > 10,00,000 (limit exceeded)
 *
 * This is the default gateway (no real API keys needed).
 * Swap to StripeGateway or RazorpayGateway via spring.profiles.active=stripe
 */
@Slf4j
@Component("MOCK")
public class MockPaymentGateway implements PaymentGatewayPort {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000"); // ₹10 lakh

    @Override
    public String gatewayName() {
        return "MOCK";
    }

    @Override
    public GatewayResult deposit(UUID userId, BigDecimal amount, String currency,
                                 String idempotencyKey, String description) {
        log.info("[MOCK] Deposit request: user={} amount={} {} idempotencyKey={}",
                userId, amount, currency, idempotencyKey);

        // Simulate network latency
        simulateLatency();

        if (!isValidAmount(amount)) {
            return GatewayResult.failure("INVALID_AMOUNT",
                    String.format("Amount must be between %s and %s", MIN_AMOUNT, MAX_AMOUNT));
        }

        String ref = "MOCK-DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String response = String.format(
                "{\"gateway\":\"MOCK\",\"type\":\"DEPOSIT\",\"ref\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"ts\":\"%s\"}",
                ref, amount, currency, Instant.now());

        log.info("[MOCK] Deposit SUCCESS: ref={}", ref);
        return GatewayResult.success(ref, response);
    }

    @Override
    public GatewayResult withdraw(UUID userId, BigDecimal amount, String currency,
                                  String bankReference, String idempotencyKey) {
        log.info("[MOCK] Withdrawal request: user={} amount={} {} bankRef={}",
                userId, amount, currency, bankReference);

        simulateLatency();

        if (!isValidAmount(amount)) {
            return GatewayResult.failure("INVALID_AMOUNT",
                    String.format("Amount must be between %s and %s", MIN_AMOUNT, MAX_AMOUNT));
        }

        String ref = "MOCK-WDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String response = String.format(
                "{\"gateway\":\"MOCK\",\"type\":\"WITHDRAWAL\",\"ref\":\"%s\",\"amount\":%s,\"bankRef\":\"%s\",\"ts\":\"%s\"}",
                ref, amount, bankReference, Instant.now());

        log.info("[MOCK] Withdrawal SUCCESS: ref={}", ref);
        return GatewayResult.success(ref, response);
    }

    private boolean isValidAmount(BigDecimal amount) {
        return amount != null
                && amount.compareTo(MIN_AMOUNT) >= 0
                && amount.compareTo(MAX_AMOUNT) <= 0;
    }

    private void simulateLatency() {
        try {
            long ms = 50L + (long)(Math.random() * 150);
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
