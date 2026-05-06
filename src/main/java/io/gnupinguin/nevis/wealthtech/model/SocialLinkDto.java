package io.gnupinguin.nevis.wealthtech.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record SocialLinkDto(
        UUID id,
        String url,
        @JsonProperty("created_at") Instant createdAt
) {}
