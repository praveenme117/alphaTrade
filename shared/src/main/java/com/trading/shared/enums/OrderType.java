package com.trading.shared.enums;

public enum OrderType {
    MARKET,     // fill immediately at best available price
    LIMIT,      // fill only at specified price or better
    STOP_LOSS,  // trigger market order when price hits stop
    STOP_LIMIT  // trigger limit order when price hits stop
}
