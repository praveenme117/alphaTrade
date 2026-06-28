package com.trading.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event for live price updates.
 * Topic: market.price.updates
 * Producer: MockPriceFeedService (market-service)
 * Consumers: market-service (Redis cache), market-service (WebSocket broadcast)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdatedEvent {

    private UUID instrumentId;
    private String symbol;
    private BigDecimal ltp;            // Last Traded Price
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;          // previous close
    private BigDecimal change;         // ltp - close
    private BigDecimal changePct;      // (ltp - close) / close * 100
    private BigDecimal volume;
    private Instant timestamp;
}
