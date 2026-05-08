package io.gnupinguin.nevis.wealthtech.service.search.document;

import java.util.UUID;

public record DocumentSearchResult(
        UUID id,
        UUID clientId,
        String title,
        String matchedChunk,
        String summary,
        float score
) {
}
