package com.trading.market.service;

import com.trading.market.dto.InstrumentDto;
import com.trading.market.dto.QuoteDto;
import com.trading.market.entity.Instrument;
import com.trading.market.repository.InstrumentRepository;
import com.trading.shared.enums.InstrumentType;
import com.trading.shared.events.PriceUpdatedEvent;
import com.trading.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private static final String REDIS_KEY_PREFIX = "price:";

    private final InstrumentRepository instrumentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MockPriceFeedService priceFeedService;

    // ─── Instruments ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InstrumentDto> getAllInstruments() {
        return instrumentRepository.findAllByActiveTrue()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstrumentDto> getInstrumentsByType(InstrumentType type) {
        return instrumentRepository.findByTypeAndActiveTrue(type)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstrumentDto getInstrumentById(UUID id) {
        return instrumentRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument", id.toString()));
    }

    @Transactional(readOnly = true)
    public InstrumentDto getInstrumentBySymbol(String symbol) {
        return instrumentRepository.findBySymbol(symbol.toUpperCase())
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument", symbol));
    }

    @Transactional(readOnly = true)
    public List<InstrumentDto> search(String query) {
        return instrumentRepository.searchBySymbolOrName(query)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ─── Quotes (live price from Redis) ─────────────────────────

    @Transactional(readOnly = true)
    public QuoteDto getQuote(String symbol) {
        Instrument instrument = instrumentRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Instrument", symbol));

        // Try Redis first (real-time price)
        PriceUpdatedEvent cached = (PriceUpdatedEvent) redisTemplate.opsForValue()
                .get(REDIS_KEY_PREFIX + symbol.toUpperCase());

        if (cached != null) {
            return QuoteDto.builder()
                    .instrumentId(instrument.getId())
                    .symbol(cached.getSymbol())
                    .name(instrument.getName())
                    .ltp(cached.getLtp())
                    .open(cached.getOpen())
                    .high(cached.getHigh())
                    .low(cached.getLow())
                    .close(cached.getClose())
                    .change(cached.getChange())
                    .changePct(cached.getChangePct())
                    .volume(cached.getVolume())
                    .type(instrument.getType())
                    .currency(instrument.getCurrency())
                    .exchange(instrument.getExchange())
                    .timestamp(cached.getTimestamp())
                    .build();
        }

        // Fallback to in-memory price feed
        BigDecimal livePrice = priceFeedService.getCurrentPrice(symbol.toUpperCase());
        BigDecimal close = instrument.getClosePrice() != null ? instrument.getClosePrice() : livePrice;
        BigDecimal change = livePrice.subtract(close);

        return QuoteDto.builder()
                .instrumentId(instrument.getId())
                .symbol(instrument.getSymbol())
                .name(instrument.getName())
                .ltp(livePrice)
                .open(instrument.getOpenPrice())
                .high(instrument.getHighPrice())
                .low(instrument.getLowPrice())
                .close(close)
                .change(change)
                .changePct(BigDecimal.ZERO)
                .volume(instrument.getVolume())
                .type(instrument.getType())
                .currency(instrument.getCurrency())
                .exchange(instrument.getExchange())
                .timestamp(java.time.Instant.now())
                .build();
    }

    public List<QuoteDto> getQuotes(List<String> symbols) {
        return symbols.stream()
                .map(this::getQuote)
                .toList();
    }

    // ─── Mapping ─────────────────────────────────────────────────

    private InstrumentDto toDto(Instrument i) {
        return InstrumentDto.builder()
                .id(i.getId())
                .symbol(i.getSymbol())
                .name(i.getName())
                .type(i.getType())
                .currency(i.getCurrency())
                .exchange(i.getExchange())
                .sector(i.getSector())
                .tickSize(i.getTickSize())
                .lotSize(i.getLotSize())
                .lastPrice(i.getLastPrice())
                .active(i.isActive())
                .build();
    }
}
