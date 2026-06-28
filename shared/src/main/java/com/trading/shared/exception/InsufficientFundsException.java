package com.trading.shared.exception;

public class InsufficientFundsException extends TradingException {

    public InsufficientFundsException(String message) {
        super(message, "INSUFFICIENT_FUNDS", 402);
    }
}
