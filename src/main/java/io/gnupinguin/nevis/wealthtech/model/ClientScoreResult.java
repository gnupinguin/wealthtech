package io.gnupinguin.nevis.wealthtech.model;

import java.util.UUID;

public record ClientScoreResult(
        UUID id,
        float score
) {}
