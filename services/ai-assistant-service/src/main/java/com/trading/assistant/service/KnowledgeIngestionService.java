package com.trading.assistant.service;

import com.trading.assistant.client.QdrantClient;
import com.trading.assistant.config.AssistantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Chunks the markdown knowledge base bundled under resources/knowledge/ and
 * upserts it into Qdrant. Point ids are deterministic (UUID from source+title),
 * so re-ingesting is idempotent — restarts update chunks in place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestionService {

    private final QdrantClient qdrant;
    private final AssistantProperties properties;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnStartup() {
        if (!properties.rag().ingestOnStartup()) {
            return;
        }
        if (!qdrant.isConfigured()) {
            log.warn("Qdrant not configured (QDRANT_URL / QDRANT_API_KEY missing) — skipping knowledge ingestion");
            return;
        }
        try {
            int count = ingest();
            log.info("Knowledge base ingested: {} chunks in collection '{}'",
                    count, properties.qdrant().collection());
        } catch (Exception e) {
            log.error("Knowledge ingestion failed — assistant will answer without RAG context", e);
        }
    }

    public int ingest() {
        qdrant.ensureCollection();
        List<QdrantClient.KnowledgeChunk> chunks = loadChunks();
        // Upsert in small batches — each chunk is embedded server-side by Qdrant
        for (int i = 0; i < chunks.size(); i += 16) {
            qdrant.upsertChunks(chunks.subList(i, Math.min(i + 16, chunks.size())));
        }
        return chunks.size();
    }

    private List<QdrantClient.KnowledgeChunk> loadChunks() {
        List<QdrantClient.KnowledgeChunk> chunks = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:knowledge/*.md");
            for (Resource resource : resources) {
                String source = resource.getFilename();
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                chunks.addAll(chunkBySections(source, content));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read knowledge base resources", e);
        }
        return chunks;
    }

    /** Split a markdown document into one chunk per "## " section. */
    private List<QdrantClient.KnowledgeChunk> chunkBySections(String source, String content) {
        List<QdrantClient.KnowledgeChunk> chunks = new ArrayList<>();
        String title = "Introduction";
        StringBuilder section = new StringBuilder();
        for (String line : content.split("\n")) {
            if (line.startsWith("## ")) {
                addChunk(chunks, source, title, section.toString());
                title = line.substring(3).trim();
                section = new StringBuilder();
            }
            section.append(line).append('\n');
        }
        addChunk(chunks, source, title, section.toString());
        return chunks;
    }

    private void addChunk(List<QdrantClient.KnowledgeChunk> chunks,
                          String source, String title, String text) {
        if (text.strip().length() < 40) {
            return;
        }
        String id = UUID.nameUUIDFromBytes((source + "::" + title).getBytes(StandardCharsets.UTF_8)).toString();
        chunks.add(new QdrantClient.KnowledgeChunk(id, source, title, text.strip()));
    }
}
