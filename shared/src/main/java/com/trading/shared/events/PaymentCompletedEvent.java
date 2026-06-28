package com.trading.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published when a payment is completed.
 * Topic: payments.completed
 * Consumers: wallet-service (credit balance), notification-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    private UUID paymentOrderId;
    private UUID userId;
    private String gateway;            // MOCK / STRIPE / RAZORPAY
    private String gatewayPaymentId;   // gateway reference
    private BigDecimal amount;
    private String currency;           // INR / USDT
    private String type;               // DEPOSIT / WITHDRAWAL
    private Instant completedAt;
}
