package io.gnupinguin.nevis.wealthtech.persistence.projection;

import java.time.Instant;
import java.util.UUID;

public record DocumentSearchProjection(UUID id,
                                   UUID clientId,
                                   String title,
                                   String matchedChunk,
                                   String summary,
                                   Instant createdAt,
                                   float score) {
}
