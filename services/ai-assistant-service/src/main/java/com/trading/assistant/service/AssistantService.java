package com.trading.assistant.service;

import com.trading.assistant.client.GroqClient;
import com.trading.assistant.client.QdrantClient;
import com.trading.assistant.config.AssistantProperties;
import com.trading.assistant.dto.ChatRequest;
import com.trading.assistant.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private static final int MAX_HISTORY_TURNS = 12;

    private static final String SYSTEM_PROMPT = """
            You are "Alpha", the in-app assistant of alphaTrade — a stock & crypto trading platform.
            Help users understand and use the platform: registering, funding the wallet, placing \
            MARKET/LIMIT/STOP_LOSS orders, reading their portfolio, notifications, and how the \
            platform works internally.

            Rules:
            - Answer from the CONTEXT sections below when they are relevant; they are authoritative \
            documentation of this platform.
            - If the context does not cover the question, say so briefly and answer from general \
            knowledge only if it is safe to do so.
            - Never give financial or investment advice (which asset to buy/sell); explain mechanics only.
            - This is a demo platform with mock money and mock payments — remind users of that when \
            they ask about real funds.
            - Be concise. Use short paragraphs or bullet lists. Plain text or light markdown.
            """;

    private final GroqClient groq;
    private final QdrantClient qdrant;
    private final AssistantProperties properties;

    public ChatResponse chat(ChatRequest request, String userEmail) {
        if (!groq.isConfigured()) {
            throw new IllegalStateException("Assistant is not configured (missing GROQ_API_KEY)");
        }

        List<QdrantClient.SearchHit> hits = retrieveContext(request.message());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemMessage(hits, userEmail)));
        if (request.history() != null) {
            request.history().stream()
                    .filter(m -> m.role() != null && ("user".equals(m.role()) || "assistant".equals(m.role())))
                    .skip(Math.max(0, request.history().size() - MAX_HISTORY_TURNS))
                    .forEach(m -> messages.add(Map.of("role", m.role(), "content", m.content())));
        }
        messages.add(Map.of("role", "user", "content", request.message()));

        String reply = groq.chat(messages);

        List<ChatResponse.Source> sources = hits.stream()
                .map(h -> new ChatResponse.Source(h.title(), h.source(), Math.round(h.score() * 100) / 100.0))
                .toList();
        return new ChatResponse(reply, groq.model(), sources);
    }

    private List<QdrantClient.SearchHit> retrieveContext(String query) {
        if (!qdrant.isConfigured()) {
            return List.of();
        }
        try {
            return qdrant.search(query, properties.rag().topK(), properties.rag().minScore());
        } catch (Exception e) {
            // Retrieval is best-effort: fall back to a docs-free answer instead of failing the chat
            log.warn("Qdrant retrieval failed, answering without RAG context: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildSystemMessage(List<QdrantClient.SearchHit> hits, String userEmail) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);
        if (userEmail != null && !userEmail.isBlank()) {
            sb.append("\nThe signed-in user's email is ").append(userEmail).append('.');
        }
        if (!hits.isEmpty()) {
            sb.append("\n\nCONTEXT — alphaTrade documentation excerpts:\n");
            for (QdrantClient.SearchHit hit : hits) {
                sb.append("\n--- [").append(hit.title()).append("] (").append(hit.source()).append(")\n")
                  .append(hit.text()).append('\n');
            }
        }
        return sb.toString();
    }
}
