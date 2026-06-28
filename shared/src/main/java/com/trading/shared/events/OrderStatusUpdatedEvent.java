package com.trading.shared.events;

import com.trading.shared.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published when order status changes.
 * Topic: orders.status.updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {

    private UUID orderId;
    private UUID userId;
    private String symbol;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private BigDecimal filledQuantity;
    private BigDecimal averagePrice;
    private String reason;            // reason for REJECTED status
    private Instant updatedAt;
}
