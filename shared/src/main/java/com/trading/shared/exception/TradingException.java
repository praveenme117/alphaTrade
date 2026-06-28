package com.trading.shared.exception;

import lombok.Getter;

/**
 * Base runtime exception for all trading platform errors.
 */
@Getter
public class TradingException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public TradingException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public TradingException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
