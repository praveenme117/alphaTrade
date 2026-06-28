package com.trading.payment.gateway.impl;

import com.trading.payment.gateway.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stripe Gateway Stub — production-ready interface, no real API calls.
 *
 * To enable real Stripe:
 *   1. Add stripe-java SDK to pom.xml
 *   2. Set STRIPE_API_KEY env var
 *   3. Uncomment Stripe.apiKey = ... and PaymentIntent.create(...) calls
 *   4. Switch active gateway via payment.gateway=STRIPE in application.yml
 */
@Slf4j
@Component("STRIPE")
public class StripeGatewayStub implements PaymentGatewayPort {

    @Override
    public String gatewayName() { return "STRIPE"; }

    @Override
    public GatewayResult deposit(UUID userId, BigDecimal amount, String currency,
                                 String idempotencyKey, String description) {
        log.warn("[STRIPE STUB] Real Stripe integration not configured — use MOCK gateway");
        // Real implementation:
        // Stripe.apiKey = stripeApiKey;
        // PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        //     .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
        //     .setCurrency(currency.toLowerCase())
        //     .setDescription(description)
        //     .build();
        // PaymentIntent intent = PaymentIntent.create(params, RequestOptions.builder()
        //     .setIdempotencyKey(idempotencyKey).build());
        // return GatewayResult.success(intent.getId(), intent.toJson());
        return GatewayResult.failure("STUB_NOT_CONFIGURED",
                "Stripe not configured. Set payment.gateway=MOCK or configure Stripe API key.");
    }

    @Override
    public GatewayResult withdraw(UUID userId, BigDecimal amount, String currency,
                                  String bankReference, String idempotencyKey) {
        log.warn("[STRIPE STUB] Stripe payouts not configured");
        return GatewayResult.failure("STUB_NOT_CONFIGURED",
                "Stripe payouts not configured. Use MOCK gateway or configure Stripe.");
    }
}
