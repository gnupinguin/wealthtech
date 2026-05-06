package io.gnupinguin.nevis.wealthtech.model;

import java.util.List;

public record SearchResponse(
        String query,
        List<ClientSearchResult> clients,
        List<DocumentSearchResult> documents
) {}
