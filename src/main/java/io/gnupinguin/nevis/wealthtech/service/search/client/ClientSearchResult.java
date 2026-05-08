package io.gnupinguin.nevis.wealthtech.service.search.client;

import java.util.List;
import java.util.UUID;

public record ClientSearchResult(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String description,
        List<String> socialLinks,
        float score
) {
}
