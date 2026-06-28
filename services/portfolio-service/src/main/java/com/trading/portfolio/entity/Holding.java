package com.trading.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user's position in a single instrument.
 * Updated on every trade execution event from Kafka.
 *
 * Unrealized P&L = (currentPrice - avgBuyPrice) * quantity
 * Realized P&L   = accumulated from closed positions
 */
@Entity
@Table(name = "holdings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "instrument_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "instrument_id", nullable = false)
    private UUID instrumentId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "average_buy_price", nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal averageBuyPrice = BigDecimal.ZERO;

    @Column(name = "total_invested", nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal totalInvested = BigDecimal.ZERO;

    @Column(name = "realized_pnl", nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
