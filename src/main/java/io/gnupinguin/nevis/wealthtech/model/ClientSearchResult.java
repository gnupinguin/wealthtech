package io.gnupinguin.nevis.wealthtech.model;

import java.util.UUID;

public record ClientSearchResult(
        UUID id,
        float score
) {}
