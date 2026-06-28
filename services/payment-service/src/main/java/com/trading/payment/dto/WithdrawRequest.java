package com.trading.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {
    @NotNull
    @DecimalMin("1.0")
    private BigDecimal amount;

    @NotBlank
    private String currency = "INR";

    /** Bank account / UPI reference for withdrawal destination */
    @NotBlank
    private String bankReference;

    @NotBlank
    private String idempotencyKey;
}
