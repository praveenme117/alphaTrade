package com.trading.assistant.client;

import com.trading.assistant.config.AssistantProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thin REST client for Qdrant Cloud. Embeddings are generated server-side by
 * Qdrant Cloud Inference (the "text" + "model" document objects below), so this
 * service never computes vectors itself.
 */
@Slf4j
@Component
public class QdrantClient {

    private final RestClient http;
    private final AssistantProperties.Qdrant props;

    public QdrantClient(AssistantProperties properties) {
        this.props = properties.qdrant();
        this.http = RestClient.builder()
                .baseUrl(props.url())
                .defaultHeader("api-key", props.apiKey())
                .build();
    }

    public boolean isConfigured() {
        return props.url() != null && !props.url().isBlank()
                && props.apiKey() != null && !props.apiKey().isBlank();
    }

    /** Create the collection if it does not exist yet. */
    public void ensureCollection() {
        JsonNode exists = http.get()
                .uri("/collections/{c}/exists", props.collection())
                .retrieve()
                .body(JsonNode.class);
        if (exists != null && exists.path("result").path("exists").asBoolean(false)) {
            return;
        }
        http.put()
                .uri("/collections/{c}", props.collection())
                .body(Map.of("vectors", Map.of("size", props.vectorSize(), "distance", "Cosine")))
                .retrieve()
                .body(JsonNode.class);
        log.info("Created Qdrant collection '{}'", props.collection());
    }

    /** Upsert chunks; Qdrant embeds the text server-side. */
    public void upsertChunks(List<KnowledgeChunk> chunks) {
        List<Map<String, Object>> points = chunks.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.id(),
                        "vector", Map.of("text", c.text(), "model", props.embeddingModel()),
                        "payload", Map.of("source", c.source(), "title", c.title(), "text", c.text())))
                .toList();
        http.put()
                .uri("/collections/{c}/points?wait=true", props.collection())
                .body(Map.of("points", points))
                .retrieve()
                .body(JsonNode.class);
    }

    /** Semantic search; the query is embedded server-side with the same model. */
    public List<SearchHit> search(String query, int limit, double minScore) {
        JsonNode response = http.post()
                .uri("/collections/{c}/points/query", props.collection())
                .body(Map.of(
                        "query", Map.of("text", query, "model", props.embeddingModel()),
                        "limit", limit,
                        "score_threshold", minScore,
                        "with_payload", true))
                .retrieve()
                .body(JsonNode.class);

        List<SearchHit> hits = new ArrayList<>();
        if (response != null) {
            for (JsonNode point : response.path("result").path("points")) {
                JsonNode payload = point.path("payload");
                hits.add(new SearchHit(
                        payload.path("title").asText(""),
                        payload.path("source").asText(""),
                        payload.path("text").asText(""),
                        point.path("score").asDouble(0)));
            }
        }
        return hits;
    }

    public long countPoints() {
        JsonNode response = http.post()
                .uri("/collections/{c}/points/count", props.collection())
                .body(Map.of("exact", true))
                .retrieve()
                .body(JsonNode.class);
        return response == null ? 0 : response.path("result").path("count").asLong(0);
    }

    public record KnowledgeChunk(String id, String source, String title, String text) {}

    public record SearchHit(String title, String source, String text, double score) {}
}
