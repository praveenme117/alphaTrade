package com.trading.payment.gateway.impl;

import com.trading.payment.gateway.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Razorpay Gateway Stub — production-ready interface, no real API calls.
 *
 * To enable real Razorpay:
 *   1. Add razorpay-java SDK to pom.xml
 *   2. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET env vars
 *   3. Uncomment RazorpayClient and Order/Payout creation calls
 *   4. Switch via payment.gateway=RAZORPAY in application.yml
 */
@Slf4j
@Component("RAZORPAY")
public class RazorpayGatewayStub implements PaymentGatewayPort {

    @Override
    public String gatewayName() { return "RAZORPAY"; }

    @Override
    public GatewayResult deposit(UUID userId, BigDecimal amount, String currency,
                                 String idempotencyKey, String description) {
        log.warn("[RAZORPAY STUB] Real Razorpay integration not configured — use MOCK gateway");
        // Real implementation:
        // RazorpayClient client = new RazorpayClient(keyId, keySecret);
        // JSONObject orderRequest = new JSONObject();
        // orderRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
        // orderRequest.put("currency", currency);
        // orderRequest.put("receipt", idempotencyKey);
        // Order order = client.orders.create(orderRequest);
        // return GatewayResult.success(order.get("id"), order.toString());
        return GatewayResult.failure("STUB_NOT_CONFIGURED",
                "Razorpay not configured. Set payment.gateway=MOCK or configure Razorpay keys.");
    }

    @Override
    public GatewayResult withdraw(UUID userId, BigDecimal amount, String currency,
                                  String bankReference, String idempotencyKey) {
        log.warn("[RAZORPAY STUB] Razorpay payouts not configured");
        return GatewayResult.failure("STUB_NOT_CONFIGURED",
                "Razorpay payouts not configured. Use MOCK gateway.");
    }
}
