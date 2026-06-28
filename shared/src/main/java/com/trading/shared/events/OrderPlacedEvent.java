package com.trading.shared.events;

import com.trading.shared.enums.OrderSide;
import com.trading.shared.enums.OrderType;
import com.trading.shared.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published when a new order is placed.
 * Topic: orders.placed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private UUID orderId;
    private UUID userId;
    private UUID instrumentId;
    private String symbol;
    private OrderType orderType;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal price;          // null for MARKET orders
    private BigDecimal stopPrice;      // null unless STOP_LOSS / STOP_LIMIT
    private ProductType productType;
    private Instant placedAt;
}
