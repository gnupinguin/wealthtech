package io.gnupinguin.nevis.wealthtech.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String email,
        String description,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("social_links") List<String> socialLinks
) {}
