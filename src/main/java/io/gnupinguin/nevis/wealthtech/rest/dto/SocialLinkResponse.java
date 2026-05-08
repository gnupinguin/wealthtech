package io.gnupinguin.nevis.wealthtech.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record SocialLinkResponse(
        UUID id,
        String url,
        @JsonProperty("created_at") Instant createdAt
) {}
