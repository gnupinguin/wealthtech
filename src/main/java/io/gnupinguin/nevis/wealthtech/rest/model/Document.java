package io.gnupinguin.nevis.wealthtech.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record Document(
        UUID id,
        @JsonProperty("client_id") UUID clientId,
        String title,
        String content,
        String summary,
        @JsonProperty("created_at") Instant createdAt
) {}
