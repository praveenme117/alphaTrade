package com.trading.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    @NotNull
    @DecimalMin("1.0")
    private BigDecimal amount;

    @NotBlank
    private String currency = "INR";

    /** Client-generated UUID to prevent duplicate charges */
    @NotBlank
    private String idempotencyKey;
}
