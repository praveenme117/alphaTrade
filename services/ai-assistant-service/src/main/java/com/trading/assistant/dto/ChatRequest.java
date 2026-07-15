package com.trading.assistant.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * @param message the new user message
 * @param history prior turns of this conversation (client-held; the service is stateless)
 */
public record ChatRequest(@NotBlank String message, List<ChatMessage> history) {

    public record ChatMessage(String role, String content) {}
}
