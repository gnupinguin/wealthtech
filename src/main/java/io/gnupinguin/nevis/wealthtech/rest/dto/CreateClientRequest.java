package io.gnupinguin.nevis.wealthtech.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CreateClientRequest(
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String email,
        String description,
        @JsonProperty("social_links") List<String> socialLinks
) {}
