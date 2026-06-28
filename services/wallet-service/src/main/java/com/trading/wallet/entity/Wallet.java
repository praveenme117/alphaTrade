package com.trading.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * User wallet — one per user, per currency.
 * INR wallet for stocks, USDT wallet for crypto.
 *
 * availableBalance = balance - lockedBalance
 * locked funds are held during order placement, released on fill/cancel.
 */
@Entity
@Table(name = "wallets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "currency"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    /** Total balance including locked funds */
    @Column(nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /** Funds reserved for open orders — cannot be withdrawn */
    @Column(name = "locked_balance", nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Version
    private Long version;   // Optimistic locking — prevents concurrent balance corruption

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Balance available for new orders */
    @Transient
    public BigDecimal getAvailableBalance() {
        return balance.subtract(lockedBalance);
    }
}
