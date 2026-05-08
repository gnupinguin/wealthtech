package io.gnupinguin.nevis.wealthtech.rest.dto;

import java.util.List;

public record SearchResponse(
        String query,
        List<ClientSearchResponse> clients,
        List<DocumentSearchResponse> documents,
        List<String> errors
) {}
