package com.trading.assistant.controller;

import com.trading.assistant.dto.ChatRequest;
import com.trading.assistant.dto.ChatResponse;
import com.trading.assistant.service.AssistantService;
import com.trading.assistant.service.KnowledgeIngestionService;
import com.trading.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "RAG chatbot over platform docs (Qdrant retrieval + Groq LLM)")
public class AssistantController {

    private final AssistantService assistantService;
    private final KnowledgeIngestionService ingestionService;

    @Operation(summary = "Chat with the AI assistant")
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assistantService.chat(request, userEmail)));
    }

    @Operation(summary = "Re-ingest the bundled knowledge base into Qdrant")
    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<Integer>> reindex() {
        int chunks = ingestionService.ingest();
        return ResponseEntity.ok(ApiResponse.ok(chunks, "Knowledge base re-indexed"));
    }
}
