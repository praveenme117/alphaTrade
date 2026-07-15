package com.trading.assistant.client;

import com.trading.assistant.config.AssistantProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** OpenAI-compatible chat-completions client for Groq. */
@Slf4j
@Component
public class GroqClient {

    private final RestClient http;
    private final AssistantProperties.Groq props;

    public GroqClient(AssistantProperties properties) {
        this.props = properties.groq();
        this.http = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.apiKey())
                .build();
    }

    public boolean isConfigured() {
        return props.apiKey() != null && !props.apiKey().isBlank();
    }

    /**
     * @param messages ordered list of {role, content} maps (system/user/assistant)
     * @return the assistant reply text
     */
    public String chat(List<Map<String, String>> messages) {
        JsonNode response = http.post()
                .uri("/chat/completions")
                .body(Map.of(
                        "model", props.model(),
                        "messages", messages,
                        "temperature", props.temperature(),
                        "max_tokens", props.maxTokens()))
                .retrieve()
                .body(JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("Empty response from Groq");
        }
        return response.path("choices").path(0).path("message").path("content").asText();
    }

    public String model() {
        return props.model();
    }
}
