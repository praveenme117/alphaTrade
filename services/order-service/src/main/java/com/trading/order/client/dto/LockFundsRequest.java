package com.trading.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LockFundsRequest {
    private UUID userId;
    private String currency;
    private BigDecimal amount;
    private UUID orderId;
}
