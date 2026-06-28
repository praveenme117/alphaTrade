package com.trading.market.service;

import com.trading.market.entity.Instrument;
import com.trading.market.repository.InstrumentRepository;
import com.trading.shared.events.PriceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Price Feed Service — simulates real-time price movements.
 *
 * Algorithm: Geometric Brownian Motion (simple version)
 *   nextPrice = lastPrice * (1 + drift + volatility * randomFactor)
 *
 * Parameters:
 *   - drift     = 0.0001 (slight upward bias)
 *   - volatility = 0.005  (±0.5% per tick)
 *   - tick interval = 1 second
 *
 * On each tick, publishes PriceUpdatedEvent to Kafka topic: market.price.updates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockPriceFeedService {

    public static final String TOPIC_PRICE_UPDATES = "market.price.updates";

    private static final double DRIFT      = 0.0001;
    private static final double VOLATILITY = 0.005;
    private static final MathContext MC    = new MathContext(10, RoundingMode.HALF_UP);

    private final InstrumentRepository instrumentRepository;
    private final KafkaTemplate<String, PriceUpdatedEvent> kafkaTemplate;

    // In-memory price state (symbol → current price)
    private final Map<String, BigDecimal> currentPrices = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> openPrices    = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> highPrices    = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lowPrices     = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> volumes       = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Initialize price state from DB after application starts.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializePrices() {
        List<Instrument> instruments = instrumentRepository.findAllByActiveTrue();
        for (Instrument inst : instruments) {
            BigDecimal seed = inst.getLastPrice() != null
                    ? inst.getLastPrice()
                    : inst.getClosePrice() != null ? inst.getClosePrice() : BigDecimal.valueOf(100);
            currentPrices.put(inst.getSymbol(), seed);
            openPrices   .put(inst.getSymbol(), seed);
            highPrices   .put(inst.getSymbol(), seed);
            lowPrices    .put(inst.getSymbol(), seed);
            volumes      .put(inst.getSymbol(), BigDecimal.ZERO);
        }
        log.info("MockPriceFeed initialized for {} instruments", instruments.size());
    }

    /**
     * Price tick — fires every 1 second.
     * Updates all instrument prices and publishes to Kafka.
     */
    @Scheduled(fixedRate = 1000)
    public void tick() {
        if (currentPrices.isEmpty()) return;

        currentPrices.forEach((symbol, lastPrice) -> {
            BigDecimal newPrice = computeNextPrice(lastPrice);
            currentPrices.put(symbol, newPrice);

            // Update OHLC
            highPrices.merge(symbol, newPrice, BigDecimal::max);
            lowPrices .merge(symbol, newPrice, BigDecimal::min);

            // Mock volume: random 100-500 units per tick
            BigDecimal tickVolume = BigDecimal.valueOf(100 + random.nextInt(400));
            volumes.merge(symbol, tickVolume, BigDecimal::add);

            // Look up instrument for metadata (UUID + close price)
            instrumentRepository.findBySymbol(symbol).ifPresent(inst -> {
                BigDecimal close  = inst.getClosePrice() != null ? inst.getClosePrice() : openPrices.get(symbol);
                BigDecimal change = newPrice.subtract(close);
                BigDecimal changePct = close.compareTo(BigDecimal.ZERO) != 0
                        ? change.divide(close, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                PriceUpdatedEvent event = PriceUpdatedEvent.builder()
                        .instrumentId(inst.getId())
                        .symbol(symbol)
                        .ltp(newPrice)
                        .open(openPrices.get(symbol))
                        .high(highPrices.get(symbol))
                        .low(lowPrices.get(symbol))
                        .close(close)
                        .change(change.setScale(2, RoundingMode.HALF_UP))
                        .changePct(changePct.setScale(2, RoundingMode.HALF_UP))
                        .volume(volumes.get(symbol))
                        .timestamp(Instant.now())
                        .build();

                kafkaTemplate.send(TOPIC_PRICE_UPDATES, symbol, event);
            });
        });
    }

    /**
     * Geometric Brownian Motion: nextPrice = lastPrice * exp(drift + vol * Z)
     * Simplified: nextPrice = lastPrice * (1 + drift + vol * Z) where Z ~ N(0,1)
     */
    private BigDecimal computeNextPrice(BigDecimal lastPrice) {
        double z        = random.nextGaussian();   // standard normal
        double factor   = 1.0 + DRIFT + VOLATILITY * z;
        double newPriceD = lastPrice.doubleValue() * factor;

        // Floor at 1.0 — prices can't go negative
        newPriceD = Math.max(1.0, newPriceD);

        return BigDecimal.valueOf(newPriceD).round(MC).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the current live price for a symbol (used for order matching).
     */
    public BigDecimal getCurrentPrice(String symbol) {
        return currentPrices.getOrDefault(symbol, BigDecimal.ZERO);
    }

    /**
     * Reset daily OHLC — called at market open (00:00 UTC for mock).
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyOhlc() {
        currentPrices.forEach((symbol, price) -> {
            openPrices.put(symbol, price);
            highPrices.put(symbol, price);
            lowPrices .put(symbol, price);
            volumes   .put(symbol, BigDecimal.ZERO);
        });
        log.info("Daily OHLC reset for {} instruments", currentPrices.size());
    }
}
