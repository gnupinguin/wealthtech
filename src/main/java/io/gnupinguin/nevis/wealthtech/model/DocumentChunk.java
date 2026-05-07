package io.gnupinguin.nevis.wealthtech.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record DocumentChunk(
        UUID id,
        @JsonProperty("document_id") UUID documentId,
        @JsonProperty("chunk_index") int chunkIndex,
        String content,
        float[] embedding,
        @JsonProperty("created_at") Instant createdAt
) {}
