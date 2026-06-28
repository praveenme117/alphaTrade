package com.trading.order.service;

import com.trading.order.client.WalletFeignClient;
import com.trading.order.client.dto.LockFundsRequest;
import com.trading.order.engine.MockMatchingEngine;
import com.trading.order.entity.Order;
import com.trading.order.repository.OrderRepository;
import com.trading.shared.enums.OrderSide;
import com.trading.shared.enums.OrderStatus;
import com.trading.shared.enums.OrderType;
import com.trading.shared.exception.ResourceNotFoundException;
import com.trading.shared.exception.TradingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String REDIS_PRICE_PREFIX = "price:";

    private final OrderRepository orderRepository;
    private final MockMatchingEngine matchingEngine;
    private final WalletFeignClient walletClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Place a new order.
     * Flow:
     *   1. Validate instrument and quantity
     *   2. Fetch current market price from Redis cache
     *   3. Lock funds in wallet (sync via Feign)
     *   4. Create Order with status OPEN
     *   5. Hand off to MockMatchingEngine (async)
     */
    @Transactional
    public Order placeOrder(UUID userId, PlaceOrderRequest req) {
        // Fetch live price from Redis (market-service writes this)
        BigDecimal marketPrice = fetchMarketPrice(req.symbol());
        if (marketPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw new TradingException("No price data for " + req.symbol(), "NO_PRICE_DATA", 404);
        }

        // Determine fund lock amount
        BigDecimal lockPrice = req.orderType() == OrderType.LIMIT && req.price() != null
                ? req.price() : marketPrice;
        BigDecimal lockAmount = req.quantity().multiply(lockPrice);
        String currency = isCrypto(req.symbol()) ? "USDT" : "INR";

        // Create order (OPEN state)
        Order order = orderRepository.save(Order.builder()
                .userId(userId)
                .instrumentId(req.instrumentId())
                .symbol(req.symbol().toUpperCase())
                .orderType(req.orderType())
                .side(req.side())
                .status(OrderStatus.OPEN)
                .productType(req.productType())
                .quantity(req.quantity())
                .price(req.orderType() == OrderType.MARKET ? null : req.price())
                .stopPrice(req.stopPrice())
                .build());

        // Lock funds for BUY orders
        if (req.side() == OrderSide.BUY) {
            try {
                walletClient.lockFunds(new LockFundsRequest(userId, currency, lockAmount, order.getId()));
            } catch (Exception e) {
                order.setStatus(OrderStatus.REJECTED);
                order.setRejectReason("Insufficient funds: " + e.getMessage());
                orderRepository.save(order);
                log.warn("Order {} rejected — wallet lock failed: {}", order.getId(), e.getMessage());
                throw new TradingException("Insufficient funds to place order", "INSUFFICIENT_FUNDS", 422);
            }
        }

        log.info("Order {} OPEN: {} {} {} @ {} (lockAmount={})",
                order.getId(), req.side(), req.quantity(), req.symbol(), lockPrice, lockAmount);

        // Async matching — returns immediately
        matchingEngine.matchAsync(order, marketPrice);
        return order;
    }

    @Transactional
    public Order cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new TradingException("Only OPEN orders can be cancelled",
                    "ORDER_NOT_CANCELLABLE", 409);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Unlock funds for BUY orders
        if (order.getSide() == OrderSide.BUY && order.getPrice() != null) {
            String currency = isCrypto(order.getSymbol()) ? "USDT" : "INR";
            BigDecimal lockAmount = order.getQuantity().multiply(order.getPrice());
            try {
                walletClient.unlockFunds(new LockFundsRequest(userId, currency, lockAmount, orderId));
            } catch (Exception e) {
                log.error("Failed to unlock funds for cancelled order {}: {}", orderId, e.getMessage());
            }
        }

        log.info("Order {} CANCELLED by user {}", orderId, userId);
        return order;
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID userId, UUID orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
    }

    @Transactional(readOnly = true)
    public List<Order> getOpenOrders(UUID userId) {
        return orderRepository.findByUserIdAndStatus(userId, OrderStatus.OPEN);
    }

    private BigDecimal fetchMarketPrice(String symbol) {
        Object cached = redisTemplate.opsForValue().get(REDIS_PRICE_PREFIX + symbol.toUpperCase());
        if (cached instanceof com.trading.shared.events.PriceUpdatedEvent event) {
            return event.getLtp();
        }
        return BigDecimal.ZERO;
    }

    private boolean isCrypto(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "BTC", "ETH", "SOL", "USDT" -> true;
            default -> false;
        };
    }

    public record PlaceOrderRequest(
            UUID instrumentId,
            String symbol,
            OrderType orderType,
            OrderSide side,
            com.trading.shared.enums.ProductType productType,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal stopPrice
    ) {}
}
