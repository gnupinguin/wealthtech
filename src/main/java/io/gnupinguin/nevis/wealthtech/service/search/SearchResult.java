package io.gnupinguin.nevis.wealthtech.service.search;

import io.gnupinguin.nevis.wealthtech.service.search.client.ClientSearchResult;
import io.gnupinguin.nevis.wealthtech.service.search.document.DocumentSearchResult;

import java.util.List;

public record SearchResult(
        List<ClientSearchResult> clients,
        List<DocumentSearchResult> documents,
        List<String> errors
) {
}
