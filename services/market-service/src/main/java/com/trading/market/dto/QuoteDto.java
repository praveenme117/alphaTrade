package com.trading.market.dto;

import com.trading.shared.enums.InstrumentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class QuoteDto {
    private UUID instrumentId;
    private String symbol;
    private String name;
    private BigDecimal ltp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal change;
    private BigDecimal changePct;
    private BigDecimal volume;
    private InstrumentType type;
    private String currency;
    private String exchange;
    private Instant timestamp;
}
