package com.trading.market.dto;

import com.trading.shared.enums.InstrumentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class InstrumentDto {
    private UUID id;
    private String symbol;
    private String name;
    private InstrumentType type;
    private String currency;
    private String exchange;
    private String sector;
    private BigDecimal tickSize;
    private BigDecimal lotSize;
    private BigDecimal lastPrice;
    private boolean active;
}
