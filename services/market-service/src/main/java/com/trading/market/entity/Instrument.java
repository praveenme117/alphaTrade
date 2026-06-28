package com.trading.market.entity;

import com.trading.shared.enums.InstrumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tradeable instrument — stocks (RELIANCE, TCS, INFY) and crypto (BTC, ETH, SOL).
 * Seeded on startup via Flyway V2 migration.
 */
@Entity
@Table(name = "instruments",
       indexes = {
           @Index(name = "idx_instruments_symbol", columnList = "symbol", unique = true),
           @Index(name = "idx_instruments_type",   columnList = "type")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;                        // e.g., RELIANCE, BTC

    @Column(nullable = false, length = 100)
    private String name;                          // e.g., Reliance Industries Ltd

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstrumentType type;                  // STOCK, CRYPTO, ETF

    @Column(precision = 20, scale = 8)
    private BigDecimal lastPrice;                 // latest LTP, updated by price feed

    @Column(precision = 20, scale = 8)
    private BigDecimal openPrice;

    @Column(precision = 20, scale = 8)
    private BigDecimal highPrice;

    @Column(precision = 20, scale = 8)
    private BigDecimal lowPrice;

    @Column(precision = 20, scale = 8)
    private BigDecimal closePrice;                // previous day close

    @Column(precision = 20, scale = 2)
    private BigDecimal volume;

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";              // INR for stocks, USDT for crypto

    @Column(name = "tick_size", precision = 10, scale = 5)
    @Builder.Default
    private BigDecimal tickSize = new BigDecimal("0.05");  // minimum price movement

    @Column(name = "lot_size", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lotSize = BigDecimal.ONE;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(length = 10)
    private String exchange;                      // NSE, BSE, CRYPTO

    @Column(length = 50)
    private String sector;                        // Energy, IT, Crypto etc.

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
