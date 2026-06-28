package com.trading.market.service;

import com.trading.shared.events.PriceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Consumes price update events from Kafka.
 * Dual responsibility:
 *   1. Cache latest price in Redis (TTL: 5 minutes)
 *   2. Broadcast to WebSocket STOMP subscribers at /topic/prices/{symbol}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceUpdateConsumer {

    private static final String REDIS_KEY_PREFIX = "price:";
    private static final Duration REDIS_TTL      = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
        topics     = MockPriceFeedService.TOPIC_PRICE_UPDATES,
        groupId    = "market-service-price-cache",
        concurrency = "3"
    )
    public void onPriceUpdated(PriceUpdatedEvent event) {
        String redisKey = REDIS_KEY_PREFIX + event.getSymbol();

        // 1. Cache in Redis
        redisTemplate.opsForValue().set(redisKey, event, REDIS_TTL);

        // 2. Push to WebSocket subscribers: /topic/prices/BTC, /topic/prices/RELIANCE …
        messagingTemplate.convertAndSend("/topic/prices/" + event.getSymbol(), event);

        log.debug("Price update: {} = {} (change {}%)",
                event.getSymbol(), event.getLtp(), event.getChangePct());
    }
}
