package com.trading.assistant.dto;

import java.util.List;

public record ChatResponse(String reply, String model, List<Source> sources) {

    public record Source(String title, String source, double score) {}
}
