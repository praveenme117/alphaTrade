package com.trading.order.engine;

import com.trading.order.client.WalletFeignClient;
import com.trading.order.client.dto.LockFundsRequest;
import com.trading.order.entity.Order;
import com.trading.order.repository.OrderRepository;
import com.trading.shared.enums.OrderSide;
import com.trading.shared.enums.OrderStatus;
import com.trading.shared.enums.OrderType;
import com.trading.shared.events.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Mock Matching Engine.
 *
 * Rules:
 *   MARKET order → fills instantly at provided marketPrice (from MockPriceFeed Redis cache)
 *   LIMIT  order → fills immediately if price is favourable (mock: always fills in 100-500ms)
 *   Fee    = 0.03% of total trade value (like Zerodha)
 *
 * After fill:
 *   1. Updates order status to FILLED
 *   2. Publishes TradeExecutedEvent → Kafka → trade.executions
 *   3. Publishes OrderStatusUpdatedEvent → Kafka → orders.status.updates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockMatchingEngine {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.0003"); // 0.03%
    private static final String TOPIC_TRADE  = "trade.executions";
    private static final String TOPIC_STATUS = "orders.status.updates";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WalletFeignClient walletClient;

    /**
     * Attempt to match and fill an order.
     * Runs asynchronously so the HTTP response returns immediately after order is OPEN.
     *
     * @param order       the newly placed order (status = OPEN)
     * @param marketPrice current market price from Redis cache
     */
    @Async("matchingEngineExecutor")
    @Transactional
    public void matchAsync(Order order, BigDecimal marketPrice) {
        try {
            // Simulate network/matching latency: 100–500ms
            long delay = 100L + (long)(Math.random() * 400);
            Thread.sleep(delay);

            BigDecimal fillPrice = determineFillPrice(order, marketPrice);

            if (fillPrice == null) {
                // Limit order not yet fillable (in real system it stays in book)
                // For mock: fill it at limit price immediately
                fillPrice = order.getPrice();
            }

            fill(order, order.getQuantity(), fillPrice);
            log.info("Order {} FILLED: {} {} {} @ {}", order.getId(),
                    order.getSide(), order.getQuantity(), order.getSymbol(), fillPrice);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Matching interrupted for order {}", order.getId());
        } catch (Exception e) {
            log.error("Matching error for order {}: {}", order.getId(), e.getMessage());
            rejectOrder(order, "Matching engine error: " + e.getMessage());
        }
    }

    private BigDecimal determineFillPrice(Order order, BigDecimal marketPrice) {
        if (order.getOrderType() == OrderType.MARKET) {
            return marketPrice;
        }
        if (order.getOrderType() == OrderType.LIMIT) {
            // Mock: always fill limit orders — assume the market moves to fill
            return order.getPrice();
        }
        return marketPrice;
    }

    private void fill(Order order, BigDecimal quantity, BigDecimal fillPrice) {
        BigDecimal totalValue = quantity.multiply(fillPrice).setScale(8, RoundingMode.HALF_UP);
        BigDecimal fee        = totalValue.multiply(FEE_RATE).setScale(8, RoundingMode.HALF_UP);

        order.setFilledQuantity(quantity);
        order.setAveragePrice(fillPrice);
        order.setFee(fee);
        order.setStatus(OrderStatus.FILLED);
        orderRepository.save(order);

        // Publish trade execution event
        TradeExecutedEvent tradeEvent = TradeExecutedEvent.builder()
                .tradeId(UUID.randomUUID())
                .orderId(order.getId())
                .userId(order.getUserId())
                .instrumentId(order.getInstrumentId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .quantity(quantity)
                .price(fillPrice)
                .totalValue(totalValue)
                .fee(fee)
                .executedAt(Instant.now())
                .build();

        kafkaTemplate.send(TOPIC_TRADE, order.getSymbol(), tradeEvent);
        log.debug("Published TradeExecutedEvent for order {}", order.getId());
    }

    private void rejectOrder(Order order, String reason) {
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectReason(reason);
        orderRepository.save(order);

        // Unlock funds on rejection
        String currency = isCrypto(order.getSymbol()) ? "USDT" : "INR";
        BigDecimal lockAmount = order.getPrice() != null
                ? order.getQuantity().multiply(order.getPrice())
                : BigDecimal.ZERO;
        if (lockAmount.compareTo(BigDecimal.ZERO) > 0 && order.getSide() == OrderSide.BUY) {
            try {
                walletClient.unlockFunds(new LockFundsRequest(
                        order.getUserId(), currency, lockAmount, order.getId()));
            } catch (Exception e) {
                log.error("Failed to unlock funds for rejected order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    private boolean isCrypto(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "BTC", "ETH", "SOL", "USDT" -> true;
            default -> false;
        };
    }
}
