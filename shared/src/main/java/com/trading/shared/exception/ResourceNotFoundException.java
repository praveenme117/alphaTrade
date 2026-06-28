package com.trading.shared.exception;

public class ResourceNotFoundException extends TradingException {

    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found: " + identifier, "RESOURCE_NOT_FOUND", 404);
    }
}
