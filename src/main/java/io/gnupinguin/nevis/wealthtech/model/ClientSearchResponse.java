package io.gnupinguin.nevis.wealthtech.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record ClientSearchResponse(
        UUID id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String email,
        String description,
        @JsonProperty("social_links") List<String> socialLinks,
        float score
) {}
