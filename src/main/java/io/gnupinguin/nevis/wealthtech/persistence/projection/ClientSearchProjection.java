package io.gnupinguin.nevis.wealthtech.persistence.projection;

import java.util.UUID;

public record ClientSearchProjection(UUID id, float score) {
}
