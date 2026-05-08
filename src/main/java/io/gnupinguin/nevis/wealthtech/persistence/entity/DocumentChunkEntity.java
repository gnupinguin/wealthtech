package io.gnupinguin.nevis.wealthtech.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public record DocumentChunkEntity(
        UUID id,
        UUID documentId,
        int chunkIndex,
        String content,
        float[] embedding,
        Instant createdAt
) {}
