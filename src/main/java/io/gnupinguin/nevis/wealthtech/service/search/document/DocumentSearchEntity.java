package io.gnupinguin.nevis.wealthtech.service.search.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentSearchEntity(UUID id,
                                   UUID clientId,
                                   String title,
                                   String matchedChunk,
                                   String summary,
                                   Instant createdAt,
                                   float score) {
}
