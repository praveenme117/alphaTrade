package com.trading.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant")
public record AssistantProperties(Groq groq, Qdrant qdrant, Rag rag) {

    public record Groq(String baseUrl, String apiKey, String model,
                       double temperature, int maxTokens) {}

    public record Qdrant(String url, String apiKey, String collection,
                         String embeddingModel, int vectorSize) {}

    public record Rag(int topK, double minScore, boolean ingestOnStartup) {}
}
