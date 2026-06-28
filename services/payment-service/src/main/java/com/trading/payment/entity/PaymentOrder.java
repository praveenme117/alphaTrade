package com.trading.payment.entity;

import com.trading.shared.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single payment intent — deposit or withdrawal.
 *
 * Lifecycle:
 *   PENDING → (gateway processing) → COMPLETED | FAILED
 *   COMPLETED → (admin action only) → REFUNDED
 *
 * Idempotency: one PaymentOrder per idempotencyKey — safe to retry.
 */
@Entity
@Table(name = "payment_orders",
    indexes = {
        @Index(name = "idx_payment_user_id",    columnList = "user_id"),
        @Index(name = "idx_payment_status",     columnList = "status"),
        @Index(name = "idx_payment_idempotency",columnList = "idempotency_key", unique = true)
    })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** DEPOSIT or WITHDRAWAL */
    @Column(nullable = false, length = 20)
    private String direction;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    /** Which gateway processed this (MOCK, STRIPE, RAZORPAY) */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String gateway = "MOCK";

    /** Gateway's own transaction reference */
    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    /** Gateway raw response or error detail */
    @Column(name = "gateway_response", length = 500)
    private String gatewayResponse;

    /** Client-supplied idempotency key to prevent duplicate charges */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /** Optional bank/UPI reference for withdrawals */
    @Column(name = "bank_reference", length = 100)
    private String bankReference;

    @Column(name = "processed_at")
    private Instant processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
