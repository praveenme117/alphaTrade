package com.trading.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable ledger entry for every wallet transaction.
 * Types: CREDIT, DEBIT, LOCK, UNLOCK, FEE
 */
@Entity
@Table(name = "wallet_ledger")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WalletLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, length = 20)
    private String type;          // CREDIT, DEBIT, LOCK, UNLOCK, FEE

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 20, scale = 8)
    private BigDecimal balanceAfter;

    @Column(length = 100)
    private String description;   // Human-readable: "Order fill BTC", "Deposit via MOCK"

    @Column(name = "reference_id")
    private UUID referenceId;     // orderId or paymentOrderId

    @Column(name = "reference_type", length = 30)
    private String referenceType; // ORDER, PAYMENT, MANUAL

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
