package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;

import java.time.Instant;
import java.util.UUID;

public record DocumentEnrichmentEvent(
        UUID id,
        UUID documentId,
        JobType type,
        Instant createdAt
) {}
