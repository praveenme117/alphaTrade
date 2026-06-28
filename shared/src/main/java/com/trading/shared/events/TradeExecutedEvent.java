package com.trading.shared.events;

import com.trading.shared.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published when a trade is executed (order filled).
 * Topic: trade.executions
 * Consumers: portfolio-service (update holdings), wallet-service (debit funds)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeExecutedEvent {

    private UUID tradeId;
    private UUID orderId;
    private UUID userId;
    private UUID instrumentId;
    private String symbol;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalValue;     // quantity * price
    private BigDecimal fee;            // trading fee (mock: 0.03%)
    private Instant executedAt;
}
