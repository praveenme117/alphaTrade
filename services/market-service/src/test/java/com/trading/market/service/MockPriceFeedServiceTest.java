package com.trading.market.service;

import com.trading.market.entity.Instrument;
import com.trading.market.repository.InstrumentRepository;
import com.trading.shared.enums.InstrumentType;
import com.trading.shared.events.PriceUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MockPriceFeedService Unit Tests")
class MockPriceFeedServiceTest {

    @Mock private InstrumentRepository instrumentRepository;
    @Mock private KafkaTemplate<String, PriceUpdatedEvent> kafkaTemplate;

    @InjectMocks private MockPriceFeedService service;

    private Instrument btc;

    @BeforeEach
    void setUp() {
        btc = Instrument.builder()
                .id(UUID.randomUUID())
                .symbol("BTC")
                .name("Bitcoin")
                .type(InstrumentType.CRYPTO)
                .lastPrice(BigDecimal.valueOf(68000))
                .closePrice(BigDecimal.valueOf(67500))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("initializePrices: loads prices for all active instruments")
    void initializePrices_LoadsAllActiveInstruments() {
        when(instrumentRepository.findAllByActiveTrue()).thenReturn(List.of(btc));

        service.initializePrices();

        assertThat(service.getCurrentPrice("BTC"))
                .isEqualByComparingTo(BigDecimal.valueOf(68000));
    }

    @Test
    @DisplayName("initializePrices: seeds at lastPrice when available")
    void initializePrices_SeedsFromLastPrice() {
        when(instrumentRepository.findAllByActiveTrue()).thenReturn(List.of(btc));
        service.initializePrices();
        assertThat(service.getCurrentPrice("BTC"))
                .isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("initializePrices: seeds at 100 when no price data available")
    void initializePrices_SeedsAt100WhenNoPriceData() {
        Instrument noPrice = Instrument.builder()
                .id(UUID.randomUUID()).symbol("NEW").name("New Instrument")
                .type(InstrumentType.STOCK).active(true).build();
        when(instrumentRepository.findAllByActiveTrue()).thenReturn(List.of(noPrice));

        service.initializePrices();

        assertThat(service.getCurrentPrice("NEW"))
                .isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("tick: publishes PriceUpdatedEvent to Kafka for each active instrument")
    void tick_PublishesPriceUpdateToKafka() {
        when(instrumentRepository.findAllByActiveTrue()).thenReturn(List.of(btc));
        when(instrumentRepository.findBySymbol("BTC")).thenReturn(Optional.of(btc));
        service.initializePrices();

        service.tick();

        ArgumentCaptor<PriceUpdatedEvent> captor = ArgumentCaptor.forClass(PriceUpdatedEvent.class);
        verify(kafkaTemplate, atLeastOnce())
                .send(eq(MockPriceFeedService.TOPIC_PRICE_UPDATES), eq("BTC"), captor.capture());

        PriceUpdatedEvent event = captor.getValue();
        assertThat(event.getSymbol()).isEqualTo("BTC");
        assertThat(event.getLtp()).isGreaterThan(BigDecimal.ZERO);
        assertThat(event.getInstrumentId()).isEqualTo(btc.getId());
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("tick: high price is always >= LTP")
    void tick_HighIsAlwaysGreaterOrEqualToLtp() {
        when(instrumentRepository.findAllByActiveTrue()).thenReturn(List.of(btc));
        when(instrumentRepository.findBySymbol("BTC")).thenReturn(Optional.of(btc));
        service.initializePrices();

        ArgumentCaptor<PriceUpdatedEvent> captor = ArgumentCaptor.forClass(PriceUpdatedEvent.class);

        // Run 10 ticks
        for (int i = 0; i < 10; i++) service.tick();

        verify(kafkaTemplate, atLeast(10))
                .send(anyString(), anyString(), captor.capture());

        captor.getAllValues().forEach(e ->
                assertThat(e.getHigh()).isGreaterThanOrEqualTo(e.getLtp()));
    }

    @Test
    @DisplayName("tick: low price is always <= LTP")
    void tick_LowIsAlwaysLessOrEqualToLtp() {
        when(instrumentRepository.findAllByActiveTrue()).thenReturn(List.of(btc));
        when(instrumentRepository.findBySymbol("BTC")).thenReturn(Optional.of(btc));
        service.initializePrices();

        ArgumentCaptor<PriceUpdatedEvent> captor = ArgumentCaptor.forClass(PriceUpdatedEvent.class);
        for (int i = 0; i < 10; i++) service.tick();

        verify(kafkaTemplate, atLeast(10))
                .send(anyString(), anyString(), captor.capture());

        captor.getAllValues().forEach(e ->
                assertThat(e.getLow()).isLessThanOrEqualTo(e.getLtp()));
    }

    @RepeatedTest(20)
    @DisplayName("computeNextPrice: price never goes below 1.0")
    void tick_PriceNeverGoesNegative() {
        // Start with very low price to stress test floor
        Instrument cheapStock = Instrument.builder()
                .id(UUID.randomUUID()).symbol("CHEAP").name("Cheap Stock")
                .type(InstrumentType.STOCK).lastPrice(BigDecimal.valueOf(1.5))
                .closePrice(BigDecimal.valueOf(1.5)).active(true).build();

        when(instrumentRepository.findAllByActiveTrue()).thenReturn(List.of(cheapStock));
        when(instrumentRepository.findBySymbol("CHEAP")).thenReturn(Optional.of(cheapStock));
        service.initializePrices();
        service.tick();

        assertThat(service.getCurrentPrice("CHEAP")).isGreaterThanOrEqualTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("getCurrentPrice: returns ZERO for unknown symbol")
    void getCurrentPrice_ReturnsZeroForUnknownSymbol() {
        assertThat(service.getCurrentPrice("UNKNOWN")).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
