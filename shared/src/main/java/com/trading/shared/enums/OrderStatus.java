package com.trading.shared.enums;

public enum OrderStatus {
    PENDING,    // just received, validating
    OPEN,       // validated, sitting in order book
    PARTIAL,    // partially filled
    FILLED,     // fully filled
    CANCELLED,  // user cancelled
    REJECTED    // system rejected (e.g. insufficient funds)
}
