package io.gnupinguin.nevis.wealthtech.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record DocumentSearchResult(
        UUID id,
        @JsonProperty("client_id") UUID clientId,
        float score,
        String title,
        String summary,
        @JsonProperty("matched_text") String matchedText
) {}
